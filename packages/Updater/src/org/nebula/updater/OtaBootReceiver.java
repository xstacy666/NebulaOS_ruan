package org.nebula.updater;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class OtaBootReceiver extends BroadcastReceiver {
    private static final String TAG = "NebulaOtaBoot";
    private static final int JOB_ID = 17001;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.i(TAG, "Boot completed, scheduling periodic OTA checks...");
            scheduleOtaJob(context);
        }
    }

    public static void scheduleOtaJob(Context context) {
        try {
            JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (js == null) return;

            ComponentName cn = new ComponentName(context, OtaCheckJobService.class);
            JobInfo info = new JobInfo.Builder(JOB_ID, cn)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(6 * 60 * 60 * 1000L) // Every 6 hours
                    .setPersisted(true)
                    .build();

            js.schedule(info);
            Log.i(TAG, "Periodic OTA check job scheduled successfully");
        } catch (Exception e) {
            Log.w(TAG, "Failed to schedule OTA job: " + e.getMessage());
        }
    }
}
