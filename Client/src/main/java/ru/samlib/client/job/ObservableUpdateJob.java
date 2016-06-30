package ru.samlib.client.job;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import ru.samlib.client.activity.MainActivity;
import ru.samlib.client.activity.SectionActivity;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.*;
import ru.samlib.client.domain.events.AuthorUpdatedEvent;
import ru.samlib.client.domain.events.Event;
import ru.samlib.client.domain.events.ObservableCheckedEvent;
import ru.samlib.client.fragments.AuthorFragment;
import ru.samlib.client.fragments.ObservableFragment;
import ru.samlib.client.parser.AuthorParser;
import ru.samlib.client.service.ObservableService;
import ru.samlib.client.util.GuiUtils;

;import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

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
        List<CharSequence> notifyAuthors = new ArrayList<>();
        Stream.of(service.getObservableAuthors()).forEach(author -> {
            try {
                AuthorParser parser = new AuthorParser(author);
                author = service.updateAuthor((AuthorEntity) parser.parse());
                author.setParsed(true);
                if(context != null && author.isHasUpdates() && author.isNotNotified()) {
                    notifyAuthors.add(author.getShortName());
                    author.setNotNotified(false);
                }
                EventBus.getDefault().post(new AuthorUpdatedEvent(author));
                Log.e(TAG, "Author " +  author.getShortName() + " updated");
            } catch (Exception e) {
                Log.e(TAG, "Unknown exception while update", e);
            }
        });
        if(!notifyAuthors.isEmpty()) {
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra(Constants.ArgsName.FRAGMENT_CLASS, ObservableFragment.class.getSimpleName());
            GuiUtils.sendBigNotification(context, 1, R.drawable.ic_update_white_36dp, "Есть новые обновления", "Есть новые обновления", null, intent, notifyAuthors);
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
}
