package ru.samlib.client.job;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import com.annimon.stream.Stream;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;

import de.greenrobot.event.EventBus;
import ru.samlib.client.App;
import ru.samlib.client.R;
import ru.samlib.client.activity.SectionActivity;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.*;
import ru.samlib.client.domain.events.AuthorUpdatedEvent;
import ru.samlib.client.domain.events.Event;
import ru.samlib.client.domain.events.ObservableCheckedEvent;
import ru.samlib.client.parser.AuthorParser;
import ru.samlib.client.service.ObservableService;
import ru.samlib.client.util.GuiUtils;

;import javax.inject.Inject;

/**
 * Created by 0shad on 01.03.2016.
 */
public class ObservableUpdateJob extends Job {


    @Inject
    ObservableService observableService;

    private static final String TAG = ObservableUpdateJob.class.getSimpleName();
    public static int jobId = -1;

    public ObservableUpdateJob() {
        App.getInstance().getComponent().inject(this);
    }

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        if (isCanceled()) {
            return Result.SUCCESS;
        }
        try {
            updateObservable(observableService, getContext());
        } catch (Exception e) {
            Log.e(TAG, "Unknown exception", e);
            return Result.FAILURE;
        } finally {

        }
        return Result.SUCCESS;
    }


    protected void postEvent(Event event) {
        EventBus.getDefault().post(event);
    }

    public static void updateObservable(ObservableService service, Context context) {
        Stream.of(service.getObservableAuthors()).forEach(author -> {
            try {
                AuthorParser parser = new AuthorParser(author);
                author.setParsed(true);
                if(context != null && author.isHasUpdates() && author.isNotNotified()) {
                    Intent intent = new Intent(context, SectionActivity.class);
                    intent.setData(Uri.parse(Constants.Net.BASE_DOMAIN + author.getLink()));
                    GuiUtils.sendNotification(context, R.drawable.ic_update_white_36dp, "Автор обновился", author.getShortName(), intent, "observable");
                    author.setNotNotified(false);
                }
                author = service.updateAuthor((AuthorEntity) parser.parse());
                EventBus.getDefault().post(new AuthorUpdatedEvent(author));
                Log.e(TAG, "Author " +  author.getShortName() + " updated");
            } catch (Exception e) {
                Log.e(TAG, "Unknown exception while update", e);
            }
        });
        EventBus.getDefault().post(new ObservableCheckedEvent());
    }

    public static void stop() {
        if (jobId > 0) {
            JobManager.instance().cancel(jobId);
        }
    }

    public static void startSchedule() {
        jobId = AppJobCreator.request(JobType.UPDATE_OBSERVABLE)
                .setPeriodic(200000)
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
}
