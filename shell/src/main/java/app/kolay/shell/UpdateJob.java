package app.kolay.shell;

import android.app.job.*;
import android.content.ComponentName;
import android.content.Context;

/** Periodic background check so a new payload is already on disk the next time the app opens. */
public class UpdateJob extends JobService {
    private static final int ID = 4711;

    static void schedule(Context c) {
        try {
            JobScheduler js = c.getSystemService(JobScheduler.class);
            if (js == null || js.getPendingJob(ID) != null) return;
            js.schedule(new JobInfo.Builder(ID, new ComponentName(c, UpdateJob.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(12L * 3600 * 1000).build());
        } catch (Throwable ignored) { }
    }

    @Override public boolean onStartJob(JobParameters p) {
        new Thread(() -> {
            try { Updater.check(getApplicationContext(), false); } catch (Throwable ignored) { }
            jobFinished(p, false);
        }).start();
        return true;
    }

    @Override public boolean onStopJob(JobParameters p) { return false; }
}
