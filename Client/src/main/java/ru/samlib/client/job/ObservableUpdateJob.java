package ru.samlib.client.job;

import android.support.annotation.NonNull;
import android.util.Log;
import com.annimon.stream.Stream;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;

import de.greenrobot.event.EventBus;
import io.requery.Persistable;
import io.requery.sql.EntityDataStore;
import ru.samlib.client.App;
import ru.samlib.client.domain.entity.*;
import ru.samlib.client.domain.events.AuthorUpdatedEvent;
import ru.samlib.client.domain.events.Event;
import ru.samlib.client.domain.events.ObservableCheckedEvent;
import ru.samlib.client.parser.AuthorParser;
import ru.samlib.client.service.ObservableService;

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
            updateObservable();
        } catch (Exception e) {
            Log.e(TAG, "Unknown exception", e);
            return Result.FAILURE;
        } finally {

        }
        postEvent(new ObservableCheckedEvent());
        return Result.SUCCESS;
    }


    protected void postEvent(Event event) {
        EventBus.getDefault().post(event);
    }

    public void updateObservable() {
        Stream.of(observableService.getObservableAuthors()).forEach(author -> {
            try {
                AuthorParser parser = new AuthorParser(author);
                author = observableService.updateAuthor((AuthorEntity) parser.parse());
                author.setParsed(true);
                postEvent(new AuthorUpdatedEvent(author));
                Log.e(TAG, "Author " +  author.getShortName() + " updated");
            } catch (Exception e) {
                Log.e(TAG, "Unknown exception while update", e);
            }
        });
    }

    public static void checkWork(Work work, EntityDataStore<Persistable> dataStore) {

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
