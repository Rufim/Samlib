package ru.samlib.client.job;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;
import com.annimon.stream.Stream;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import net.vrallev.android.cat.Cat;
import org.greenrobot.eventbus.EventBus;
import ru.kazantsev.template.net.HTTPExecutor;
import ru.kazantsev.template.net.Request;
import ru.kazantsev.template.net.Response;
import ru.kazantsev.template.util.AndroidSystemUtils;
import ru.samlib.client.App;
import ru.samlib.client.R;
import ru.samlib.client.activity.MainActivity;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.events.AuthorUpdatedEvent;
import ru.kazantsev.template.domain.event.Event;
import ru.samlib.client.domain.events.ObservableCheckedEvent;
import ru.samlib.client.fragments.ObservableFragment;
import ru.samlib.client.parser.AuthorParser;
import ru.samlib.client.parser.api.ApiParser;
import ru.samlib.client.parser.api.Command;
import ru.samlib.client.parser.api.DataCommand;
import ru.samlib.client.service.DatabaseService;
import ru.kazantsev.template.util.GuiUtils;
import ru.samlib.client.util.MergeFromRequery;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

;

/**
 * Created by 0shad on 01.03.2016.
 */
public class ObservableUpdateJob extends Job {

    private static final SimpleDateFormat urlLogDate = new SimpleDateFormat("/yyyy/MM-dd'.log'");

    @Inject
    DatabaseService databaseService;

    private static final String TAG = ObservableUpdateJob.class.getSimpleName();
    public static int jobId = -1;

    public ObservableUpdateJob() {
        App.getInstance().getComponent().inject(this);
    }

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        Context context = getContext();
        if (isCanceled() || context == null || !AndroidSystemUtils.getStringResPreference(context, R.string.preferenceObservableAuto, true)) {
            return Result.SUCCESS;
        }
        try {
            updateObservable(databaseService, getContext());
        } catch (Exception e) {
            Log.e(TAG, "Unknown exception", e);
            return Result.FAILURE;
        }
        return Result.SUCCESS;
    }


    protected void postEvent(Event event) {
        EventBus.getDefault().post(event);
    }


    public static void updateObservable(DatabaseService service, Context context) {
        List<CharSequence> notifyAuthors = new ArrayList<>();
        MergeFromRequery.merge(context, service);
        boolean statServerReachable = false;
        if(HTTPExecutor.pingHost(Constants.Net.STAT_SERVER, 80, 10000)) {
            statServerReachable = true;
        }
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        yesterday.set(Calendar.HOUR_OF_DAY, 23);
        yesterday.set(Calendar.SECOND, 59);
        yesterday.set(Calendar.MINUTE, 59);
        yesterday.set(Calendar.MILLISECOND, 999);
        List<DataCommand> commands = getChangesInTodayLog();
        for (Author author : service.getObservableAuthors()) {
            try {
                boolean wasUpdates = author.isHasUpdates();
                AuthorParser parser = new AuthorParser(author);
                author.setDeleted(false);
                if (author.getLastCheckedDate() == null || yesterday.getTime().after(author.getLastCheckedDate())) {
                    if(statServerReachable) {
                       //use server api
                        try {
                            checkUpdateDateOnStatServer(author);
                        } catch (Throwable ex) {
                            Cat.e(ex);
                            author = parser.parse();
                        }
                    } else {
                        author = parser.parse();
                    }
                } else if(commands != null) {
                    //parse last day log
                    for (DataCommand command : commands) {
                        if(command.getLink().contains(author.getLink())
                                && (Command.NEW.equals(command.getCommand()) || Command.TXT.equals(command.getCommand()))
                                && command.getCommandDate().after(author.getLastCheckedDate())) {
                            author.hasNewUpdates();
                        }
                    }
                } else {
                    author = parser.parse();
                }
                author.setLastCheckedDate(new Date());
                author = service.createOrUpdateAuthor(author);
                author.setParsed(true);
                if (context != null && author.isHasUpdates() && !wasUpdates && author.isNotNotified()) {
                    notifyAuthors.add(author.getShortName());
                    author.setNotNotified(false);
                }
                EventBus.getDefault().post(new AuthorUpdatedEvent(author));
                Log.e(TAG, "Author " + author.getShortName() + " updated");
            } catch (AuthorParser.AuthorNotExistException ex) {
                author.setDeleted(true);
                author.save();
            } catch (Exception e) {
                Log.e(TAG, "Unknown exception while update", e);
            }
        }
        if(!notifyAuthors.isEmpty() && AndroidSystemUtils.getStringResPreference(context, R.string.preferenceObservableNotification, true)) {
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra(Constants.ArgsName.FRAGMENT_CLASS, ObservableFragment.class.getSimpleName());
            GuiUtils.sendBigNotification(context, 1, R.drawable.ic_update_white, "Есть новые обновления", "Есть новые обновления", null, intent, notifyAuthors);
        }
        EventBus.getDefault().post(new ObservableCheckedEvent());
    }

    public static void stop() {
        if (jobId > 0) {
            JobManager.instance().cancel(jobId);
        }
    }

    public static void startSchedule() {
        jobId = AppJobCreator.request(JobType.UPDATE_OBSERVABLE)
                .setPeriodic(10800000)
                .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                .setUpdateCurrent(true)
                .build()
                .schedule();
    }

    public static void start() {
        AppJobCreator.request(JobType.UPDATE_OBSERVABLE)
                .setExecutionWindow(1L, 2000L)
                .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                .build()
                .schedule();
    }


    public static boolean checkUpdateDateOnStatServer(Author author) {
        throw new UnsupportedOperationException("TBD");
    }


    public static List<DataCommand> getChangesInTodayLog() {
        try {
            Request toLog = new Request(Constants.Net.LOG_PATH + urlLogDate.format(new Date()));
            Response response = toLog.execute();
            ApiParser parser = new ApiParser();
            return parser.parseInput(response.getInputStream(), ApiParser.getLogDelegateInstance());
        } catch (Throwable e) {
            Cat.e(e);
            return null;
        }
    }

}
