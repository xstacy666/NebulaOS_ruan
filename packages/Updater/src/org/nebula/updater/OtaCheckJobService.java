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
import java.net.HttpURLConnection;
import java.net.URL;

public class OtaCheckJobService extends JobService {
    private static final String TAG = "NebulaOtaJob";
    private static final String CHANNEL_ID = "nebula_ota_channel";

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i(TAG, "Executing periodic Nebula OS OTA update check...");
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
            // 1. Auto-nuke expired update packages older than 1 month (30 days)
            nukeExpiredUpdates();

            // 2. Query remote GitHub OTA manifest
            String[] urls = {
                "https://api.github.com/repos/xstacy666/NebulaOS_ruan_ota/contents/ota.json",
                MainActivity.OTA_URL,
                "https://raw.githubusercontent.com/xstacy666/AERA-ruan/main/ota.json"
            };

            for (String targetUrl : urls) {
                try {
                    String queryUrl = targetUrl + "?t=" + System.currentTimeMillis(); URL url = new URL(queryUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("User-Agent", "NebulaOS-Updater/17.0");
                    conn.setRequestProperty("Accept", "application/vnd.github.v3.raw, application/json, */*");

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
                    Log.w(TAG, "OTA check failed for " + targetUrl + ": " + e.getMessage());
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject update) {
            if (update != null) {
                String latestVer = update.optString("version", "");
                long latestCode = update.optLong("version_code", 0);
                String filename = update.optString("filename", "");
                String url = update.optString("url", "");
                String changelog = update.optString("changelog", "System improvements and bug fixes.");

                if (MainActivity.isNewUpdate(OtaCheckJobService.this, latestVer, latestCode, filename)) {
                    Log.i(TAG, "New OTA update discovered: " + latestVer + " (code: " + latestCode + ")");
                    postUpdateNotification(latestVer, latestCode, url, changelog);
                } else {
                    Log.i(TAG, "System is already up to date.");
                }
            }
            jobFinished(mParams, false);
        }
    }

    private void postUpdateNotification(String version, long versionCode, String downloadUrl, String changelog) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Nebula OS Updates",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("System update alerts for Nebula OS");
        channel.enableLights(true);
        channel.enableVibration(true);
        channel.setShowBadge(true);
        nm.createNotificationChannel(channel);

        // Tap on notification body -> opens MainActivity with auto-check
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        openIntent.putExtra("auto_check", true);
        openIntent.putExtra("version", version);
        openIntent.putExtra("target_url", downloadUrl);
        PendingIntent openPi = PendingIntent.getActivity(this, 100, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Tap on [Download] action button -> directly starts downloading from Google Drive!
        Intent downloadIntent = new Intent(this, MainActivity.class);
        downloadIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        downloadIntent.putExtra("auto_download", true);
        downloadIntent.putExtra("version", version);
        downloadIntent.putExtra("target_url", downloadUrl);
        PendingIntent downloadPi = PendingIntent.getActivity(this, 101, downloadIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        android.app.Notification.Action downloadAction = new android.app.Notification.Action.Builder(
                R.drawable.ic_updater,
                "Download",
                downloadPi
        ).build();

        android.app.Notification.Builder b = new android.app.Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_updater)
                .setContentTitle("Nebula OS Update Available")
                .setContentText("Version " + version + " is ready to install")
                .setStyle(new android.app.Notification.BigTextStyle().bigText("Version " + version + " is available.\n\n" + changelog))
                .setContentIntent(openPi)
                .addAction(downloadAction)
                .setAutoCancel(true);

        nm.notify(1701, b.build());
        Log.i(TAG, "Notification posted for Nebula OS version: " + version);
    }

    /**
     * Automatically nukes downloaded update archives older than 1 month (30 days)
     * to preserve user storage space.
     */
    public static void nukeExpiredUpdates() {
        String[] dirs = {
            "/sdcard/Download",
            "/data/media/0/Download"
        };

        long oneMonthMs = 30L * 24L * 60L * 60L * 1000L; // 30 days
        long now = System.currentTimeMillis();

        for (String dirPath : dirs) {
            File dir = new File(dirPath);
            if (!dir.exists() || !dir.isDirectory()) continue;

            File[] files = dir.listFiles();
            if (files == null) continue;

            for (File f : files) {
                String name = f.getName().toLowerCase();
                // Check if file is an update package
                if ((name.startsWith("nebula") || name.startsWith("coloros") || name.contains("update") || name.startsWith("ruan") || name.startsWith("dizi")) 
                        && (name.endsWith(".zip") || name.endsWith(".7z") || name.endsWith(".tmp"))) {
                    
                    // Delete incomplete .tmp older than 24 hours
                    if (name.endsWith(".tmp") && (now - f.lastModified() > 24L * 60L * 60L * 1000L)) {
                        Log.i(TAG, "Nuking abandoned temporary update download: " + f.getName());
                        f.delete();
                        continue;
                    }

                    // Delete packages older than 30 days
                    if (now - f.lastModified() > oneMonthMs) {
                        long ageDays = (now - f.lastModified()) / (24L * 60L * 60L * 1000L);
                        Log.i(TAG, "Nuking expired update package (" + ageDays + " days old): " + f.getName());
                        f.delete();
                    }
                }
            }
        }
    }
}
