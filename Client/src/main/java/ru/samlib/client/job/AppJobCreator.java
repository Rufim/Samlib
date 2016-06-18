package ru.samlib.client.job;

import com.annimon.stream.Stream;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import ru.samlib.client.util.SystemUtils;


import java.util.Set;

/**
 * Created by 0shad on 01.03.2016.
 */

public class AppJobCreator implements JobCreator {

    @Override
    public Job create(String name) {
        return SystemUtils.nn(JobType::instantiate, JobType.valueOf(name));
    }

    public static void scheduleJob(JobType type, long delay) {
        request(type).setPeriodic(delay).build().schedule();
    }

    public static void scheduleTask(JobType type, long start, long maxLong) {
        request(type).setExecutionWindow(start, maxLong).build().schedule();
    }

    public static Set<Job> getJobs(JobType type) {
        return JobManager.instance().getAllJobsForTag(type.name());
    }
    public static Set<JobRequest> getJobRequests(JobType type) {
        return JobManager.instance().getAllJobRequestsForTag(type.name());
    }

    public static void cancelJobs(JobType type) {
        Stream.of(getJobs(type)).forEach(Job::cancel);
    }

    public static void cancelJobRequests(JobType type) {
        Stream.of(getJobRequests(type)).map(r -> JobManager.instance().cancel(r.getJobId()));
    }

    public static void cancelJob(int jobId) {
        JobManager.instance().cancel(jobId);
    }

    public static JobRequest.Builder request(JobType type) {
        return new JobRequest.Builder(type.name());
    }
}
