package ru.samlib.client.job;

import android.app.NotificationChannel;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;

import net.vrallev.android.cat.Cat;
import org.greenrobot.eventbus.EventBus;
import ru.kazantsev.template.net.HTTPExecutor;
import ru.kazantsev.template.net.Request;
import ru.kazantsev.template.net.Response;
import ru.kazantsev.template.util.AndroidSystemUtils;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.App;
import ru.samlib.client.R;
import ru.samlib.client.activity.MainActivity;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.domain.events.AuthorUpdatedEvent;
import ru.kazantsev.template.domain.event.Event;
import ru.samlib.client.domain.events.ObservableCheckedEvent;
import ru.samlib.client.fragments.ObservableFragment;
import ru.samlib.client.fragments.SettingsFragment;
import ru.samlib.client.parser.AuthorParser;
import ru.samlib.client.parser.api.ApiParser;
import ru.samlib.client.parser.api.Command;
import ru.samlib.client.parser.api.DataCommand;
import ru.samlib.client.service.DatabaseService;
import ru.samlib.client.util.MergeFromRequery;

import javax.inject.Inject;

import java.text.SimpleDateFormat;
import java.util.*;

;import static ru.samlib.client.fragments.SettingsFragment.DEF_OBSERVABLE_AUTO;
import static ru.samlib.client.fragments.SettingsFragment.DEF_OBSERVABLE_NOTIFICATION;

/**
 * Created by 0shad on 01.03.2016.
 */
public class ObservableUpdateJob extends Job {

    private static final SimpleDateFormat urlLogDate = new SimpleDateFormat("/yyyy/MM-dd'.log'");

    @Inject
    DatabaseService databaseService;
    private static NotificationChannel mChannelObservable;

    private static final String TAG = ObservableUpdateJob.class.getSimpleName();
    public static int jobId = -1;

    public ObservableUpdateJob() {
        App.getInstance().getComponent().inject(this);
    }

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        Context context = getContext();
        if (isCanceled() || context == null || !AndroidSystemUtils.getStringResPreference(context, R.string.preferenceObservableAuto, DEF_OBSERVABLE_AUTO)) {
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
        if(HTTPExecutor.pingHost(Constants.Net.BASE_HOST, 80, 10000)) {
            List<DataCommand> commands = getChangesInTodayLog();
            for (Author author : service.getObservableAuthors()) {
                try {
                    boolean wasUpdates = author.isHasUpdates();
                    AuthorParser parser = new AuthorParser(author);
                    author.setDeleted(false);
                    boolean parsed = false;
                    if (author.getLastCheckedDate() == null || yesterday.getTime().after(author.getLastCheckedTime())) {
                        if (statServerReachable && author.getLastUpdateDate() != null) {
                            //use server api
                            try {
                                if(checkUpdateDateOnStatServer(author)) {
                                    author = parser.parse();
                                    parsed = true;
                                }
                            } catch (Throwable ex) {
                                Cat.e(ex);
                                author = parser.parse();
                                parsed = true;
                            }
                        } else {
                            author = parser.parse();
                            parsed = true;
                        }
                    }
                    if (!author.isHasUpdates() && !parsed) {
                        if (commands != null) {
                            //parse last day log
                            for (DataCommand command : commands) {
                                if (command.getLink().contains(author.getLink())
                                        && (Command.NEW.equals(command.getCommand()) || Command.TXT.equals(command.getCommand()))
                                        && command.getCommandDate().after(author.getLastUpdateDate())) {
                                    boolean found = false;
                                    for (Work work : author.getWorks()) {
                                        if(work.getLink().contains(command.getLink())) {
                                            found = true;
                                            if(work.getSize() == null || !work.getSize().equals(command.getSize())) {
                                                author.hasNewUpdates();
                                            }
                                        }
                                    }
                                    if(!found) {
                                        author.hasNewUpdates();
                                    }
                                    author.setLastUpdateDate(command.getCommandDate());
                                }
                            }
                        } else {
                            author = parser.parse();
                            parsed = true;
                        }
                    }
                    if(TextUtils.isEmpty(author.getFullName())) continue; // Something go wrong
                    author.setLastCheckedTime(new Date());
                    if(parsed) {
                        author = service.createOrUpdateAuthor(author);
                    } else {
                        author.save();
                    }
                    author.setParsed(parsed);
                    if (context != null && author.isHasUpdates() && !wasUpdates && author.isNotNotified()) {
                        notifyAuthors.add(author.getShortName());
                        author.setNotNotified(false);
                    }
                    EventBus.getDefault().post(new AuthorUpdatedEvent(author));
                    Log.e(TAG, "Author " + author.getShortName() + " updated");
                } catch (AuthorParser.AuthorNotExistException ex) {
                    author = service.getAuthor(author.getLink()); // get valid author
                    author.setDeleted(true);
                    author.save();
                } catch (Exception e) {
                    Log.e(TAG, "Unknown exception while update", e);
                }
            }
        }
        if(!notifyAuthors.isEmpty() && AndroidSystemUtils.getStringResPreference(context, R.string.preferenceObservableNotification, DEF_OBSERVABLE_NOTIFICATION )) {
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra(Constants.ArgsName.FRAGMENT_CLASS, ObservableFragment.class.getSimpleName());
            if(context != null) {
                if(mChannelObservable == null) {
                    mChannelObservable = AndroidSystemUtils.createNotificationChannel(context, "samlib_observable", context.getString(R.string.drawer_observable));
                }
                AndroidSystemUtils.sendBigNotification(context, mChannelObservable, 1, R.drawable.ic_update_white, "Есть новые обновления", "Есть новые обновления", null, intent, notifyAuthors);
            }
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
                .setRequiredNetworkType(JobRequest.NetworkType.UNMETERED)
                .setUpdateCurrent(true)
                .setRequirementsEnforced(true)
                .build()
                .schedule();
    }

    public static void start() {
        AppJobCreator.request(JobType.UPDATE_OBSERVABLE)
                .setExecutionWindow(1L, 2000L)
                .setRequiredNetworkType(JobRequest.NetworkType.UNMETERED)
                .build()
                .schedule();
    }


    public static boolean checkUpdateDateOnStatServer(Author author) throws Exception {
        Request update = new Request(Constants.Net.STAT_SERVER_UPDATE);
        update.addParam("link", author.getLink());
        Response response = update.execute();
        Long date = Long.parseLong(response.getRawContent());
        if(date > 0) {
            Date lastUpdateDate = new Date(date);
            Calendar lastUpdate = Calendar.getInstance();
            lastUpdate.setTime(author.getLastUpdateDate());
            if(author.getLastCheckedDate() == null || (lastUpdate.get(Calendar.HOUR_OF_DAY) == 0 && lastUpdate.get(Calendar.MINUTE) == 0 && lastUpdate.get(Calendar.SECOND) == 0)) {
                lastUpdate.set(Calendar.HOUR_OF_DAY, 23);
                lastUpdate.set(Calendar.SECOND, 59);
                lastUpdate.set(Calendar.MINUTE, 59);
                lastUpdate.set(Calendar.MILLISECOND, 999);
            }
            if (lastUpdateDate.after(lastUpdate.getTime())) {
                author.setLastUpdateDate(lastUpdateDate);
                return true;
            }
        } else {
            throw new Exception("Author not found!");
        }
        return false;
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
