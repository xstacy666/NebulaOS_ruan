package org.nebula.updater;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;

public class OtaCheckJobService extends JobService {
    private static final String TAG = "NebulaOtaJob";
    private static final String CHANNEL_ID = "nebula_ota_channel";
    private static final String OTA_URL = "https://raw.githubusercontent.com/xstacy666/NebulaOS_ruan_ota/main/ota.json";

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i(TAG, "Executing background OTA check & cleanup job...");
        new CheckTask(params).execute();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    private class CheckTask extends AsyncTask<Void, Void, JSONObject> {
        private final JobParameters mParams;

        CheckTask(JobParameters params) {
            mParams = params;
        }

        @Override
        protected JSONObject doInBackground(Void... voids) {
            // 1. Auto-nuke expired update files (>30 days / 1 month old)
            nukeExpiredUpdates();

            // 2. Check remote ota.json
            try {
                URL url = new URL(OTA_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestMethod("GET");
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONObject root = new JSONObject(sb.toString());
                    JSONArray arr = root.optJSONArray("response");
                    if (arr != null && arr.length() > 0) {
                        return arr.getJSONObject(0);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Background OTA check error: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject update) {
            if (update != null) {
                String latestVer = update.optString("version", "");
                String currentVer = getProp("ro.build.version.incremental", "");
                if (!latestVer.isEmpty() && !latestVer.equals(currentVer)) {
                    postUpdateNotification(latestVer, update.optString("changelog", "New system enhancements and security updates available."));
                }
            }
            jobFinished(mParams, false);
        }
    }

    private void postUpdateNotification(String version, String changelog) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Nebula OS System Updates", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Notifications for available Nebula OS OTA updates");
        channel.enableLights(true);
        nm.createNotificationChannel(channel);

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        android.app.Notification.Builder b = new android.app.Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_updater)
                .setContentTitle("Nebula OS Update Available")
                .setContentText("Version " + version + " is ready to install")
                .setStyle(new android.app.Notification.BigTextStyle().bigText("Version " + version + " is ready to download.\n\n" + changelog))
                .setContentIntent(pi)
                .setAutoCancel(true);

        nm.notify(1701, b.build());
        Log.i(TAG, "Posted OTA update notification for version: " + version);
    }

    public static void nukeExpiredUpdates() {
        File dir = new File("/sdcard/Download");
        if (!dir.exists() || !dir.isDirectory()) return;
        long oneMonthMs = 30L * 24L * 60L * 60L * 1000L; // 30 days
        long now = System.currentTimeMillis();
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            String name = f.getName().toLowerCase();
            if ((name.startsWith("nebula") || name.startsWith("coloros17") || name.contains("update")) && name.endsWith(".zip")) {
                if (now - f.lastModified() > oneMonthMs) {
                    Log.i(TAG, "Auto-nuking expired update package (>30 days): " + f.getName());
                    f.delete();
                }
            }
        }
    }

    private static String getProp(String key, String def) {
        try {
            Class<?> sp = Class.forName("android.os.SystemProperties");
            Method get = sp.getMethod("get", String.class, String.class);
            return (String) get.invoke(null, key, def);
        } catch (Exception e) {
            return def;
        }
    }
}
