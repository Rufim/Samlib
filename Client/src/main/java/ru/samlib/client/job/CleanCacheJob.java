package ru.samlib.client.job;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.util.Log;
import com.annimon.stream.Stream;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import ru.kazantsev.template.util.AndroidSystemUtils;
import ru.kazantsev.template.util.SystemUtils;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.App;
import ru.samlib.client.R;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Bookmark;
import ru.samlib.client.domain.entity.SavedHtml;
import ru.samlib.client.net.HtmlClient;
import ru.samlib.client.service.DatabaseService;

import javax.inject.Inject;
import java.io.File;
import java.util.*;

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
        TreeSet<File> allCachedFiles = new TreeSet<>(new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                long x = o1 == null ? 0 : o1.lastModified();
                long y = o1 == null ? 0 : o2.lastModified();
                return (x < y) ? -1 : ((x == y) ? 0 : 1);
            }
        });
        allCachedFiles.addAll(SystemUtils.listFilesRecursive(context.getCacheDir()));
        if(context.getExternalCacheDir().canWrite()) {
                   allCachedFiles.addAll(SystemUtils.listFiles(context.getExternalCacheDir()));
        }
        long totalSize = 0;
        List<SavedHtml> savedHtmls = databaseService.selectCachedEntities();
        for (File cached : allCachedFiles) {
            totalSize += cached.length();
        }
        SharedPreferences preference = AndroidSystemUtils.getDefaultPreference(context);
        int maxSizeMB = TextUtils.parseInt(preference.getString(context.getString(R.string.preferenceMaxCacheSize), context.getResources().getString(R.string.preferenceMaxCacheSizeDefault)));
        if(maxSizeMB < 0) return;
        long maxSize = maxSizeMB * 1024 * 1024;
        List<File> deleteFiles = new ArrayList<>();
        List<SavedHtml> deleteEntities = new ArrayList<>();
        while (maxSize < totalSize) {
            File toDelete = allCachedFiles.pollFirst();
            totalSize -= toDelete.length();
            deleteFiles.add(toDelete);
            SavedHtml savedHtml = Stream.of(savedHtmls).filter(html -> html.getFilePath().equals(toDelete.getAbsolutePath())).findFirst().orElse(null);
            if(savedHtml != null) {
                deleteEntities.add(savedHtml);
            }
        }
        if(deleteFiles.size() > 0) {
            Iterator<File> it = deleteFiles.iterator();
            while (it.hasNext()){
                File file = it.next();
                if(!file.delete()) {
                    Log.e(TAG, "Cannot delete file on path: " + file.getAbsolutePath());
                    it.remove();
                }
            }
            HtmlClient.cleanCache(deleteEntities);
            databaseService.deleteCachedEntities(deleteEntities);
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
