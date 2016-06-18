package ru.samlib.client.job;

import android.content.Context;
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
import ru.samlib.client.database.SnappyHelper;
import ru.samlib.client.domain.Validatable;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.entity.AuthorEntity;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.domain.entity.WorkEntity;
import ru.samlib.client.domain.events.Event;
import ru.samlib.client.domain.events.ObservableCheckedEvent;
import ru.samlib.client.parser.AuthorParser;

import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.Set;

;

/**
 * Created by 0shad on 01.03.2016.
 */
public class ObservableUpdateJob extends Job {

    private static final String TAG = ObservableUpdateJob.class.getSimpleName();
    private EntityDataStore<Persistable> dataStore;
    public static int jobId = -1;

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        if (isCanceled()) {
            return Result.SUCCESS;
        }
        try {
            updateObservable(getDataStore());
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

    private EntityDataStore<Persistable> getDataStore() throws Exception {
        if(App.getInstance() == null || App.getInstance().getDataStore() == null) throw new Exception("database not available");
        return App.getInstance().getDataStore();
    };

    public static void updateObservable(EntityDataStore<Persistable> dataStore) {
        Stream.of(dataStore.select(AuthorEntity.class).get().toList()).forEach(author -> {
            try {
                AuthorParser parser = new AuthorParser(author);
                dataStore.update((AuthorEntity) parser.parse());
            } catch (MalformedURLException e) {
                Log.e(TAG, "Unknown exception", e);
            }
        });
    }

    public static void checkWork(Work work, EntityDataStore<Persistable> dataStore) {

    }

    public static void stop() {
        if(jobId > 0) {
            JobManager.instance().cancel(jobId);
        }
    }

    public static void start() {
        Set<JobRequest> requests = AppJobCreator.getJobRequests(JobType.UPDATE_OBSERVABLE);
        if (requests.size() == 0) {
            jobId = AppJobCreator.request(JobType.UPDATE_OBSERVABLE)
                    .setPeriodic(200000)
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                    .build()
                    .schedule();
        } else {
            Iterator<JobRequest> it = requests.iterator();
            jobId = it.next().getJobId();
            if (requests.size() > 1) {
                while (it.hasNext()) {
                    JobManager.instance().cancel(it.next().getJobId());
                }
            }
        }
    }
}
