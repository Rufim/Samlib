package ru.samlib.client.job;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.util.Log;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import ru.kazantsev.template.util.AndroidSystemUtils;
import ru.kazantsev.template.util.SystemUtils;
import ru.samlib.client.App;
import ru.samlib.client.R;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.SavedHtml;
import ru.samlib.client.service.DatabaseService;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by 0shad on 07.05.2017.
 */
public class CleanCacheJob extends Job {

    private static final String TAG = CleanCacheJob.class.getSimpleName();

    @Inject
    DatabaseService databaseService;

    public static int jobId = -1;

    public CleanCacheJob() {
        App.getInstance().getComponent().inject(this);
    }


    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        if (isCanceled()) {
            return Result.SUCCESS;
        }
        try {
            cleanCache(databaseService, getContext());
        } catch (Exception e) {
            Log.e(TAG, "Unknown exception", e);
            return Result.FAILURE;
        }
        return Result.SUCCESS;
    }

    private void cleanCache(DatabaseService databaseService, Context context) {
        List<SavedHtml> savedHtmls = databaseService.selectCachedEntities();
        long totalSize = 0;
        for (SavedHtml savedHtml : savedHtmls) {
            totalSize += savedHtml.getSize();
        }
        SharedPreferences preference = AndroidSystemUtils.getDefaultPreference(context);
        int maxSizeMB = preference.getInt(context.getString(R.string.preferenceMaxCacheSize), context.getResources().getInteger(R.integer.preferenceMaxCacheSizeDefault));
        long maxSize = maxSizeMB * 1024 * 1024;
        List<SavedHtml> delete = new ArrayList<>();
        int i = 0;
        while (maxSize < totalSize) {
            SavedHtml toDelete = savedHtmls.get(i++);
            totalSize -= toDelete.getSize();
            delete.add(toDelete);
        }
        if(delete.size() > 0) {
            Iterator<SavedHtml> it = delete.iterator();
            while (it.hasNext()){
                SavedHtml savedHtml = it.next();
                if(!(new File(savedHtml.getFilePath()).delete())) {
                    Log.e(TAG, "Cannot delete file on path: " + savedHtml.getFilePath());
                    it.remove();
                }
            }
            databaseService.deleteCachedEntities(delete);
        }
    }


    public static void stop() {
        if (jobId > 0) {
            JobManager.instance().cancel(jobId);
        }
    }


    public static void startSchedule() {
        jobId = AppJobCreator.request(JobType.CLEAN_CACHE)
                .setPeriodic(10800000)
                .setUpdateCurrent(true)
                .build()
                .schedule();
    }
}
