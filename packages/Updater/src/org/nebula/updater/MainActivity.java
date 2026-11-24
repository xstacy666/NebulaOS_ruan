package org.nebula.updater;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private static final String TAG = "NebulaUpdater";
    
    // Official GitHub OTA Endpoint
    private static final String[] OTA_URLS = {
        "https://raw.githubusercontent.com/xstacy666/NebulaOS_ruan_ota/main/ota.json",
        "https://raw.githubusercontent.com/xstacy666/AERA-ruan/main/ota.json",
        "http://127.0.0.1:8080/ota.json"
    };

    private static final String UPDATE_FILE_NAME = "ColorOS17_ruan_dizi_Update.zip";
    private static final File UPDATE_DEST = new File("/sdcard/Download/" + UPDATE_FILE_NAME);

    private TextView tvCurrentVersion;
    private TextView tvBuildId;
    private TextView tvSecurityPatch;
    private TextView tvDeviceModel;
    private TextView tvStatusTitle;
    private TextView tvStatusDesc;
    private TextView tvChangelog;
    private LinearLayout layoutChangelogBox;
    private ProgressBar pbDownload;
    private TextView tvProgress;
    private Button btnAction;
    private ImageView btnBack;
    private ImageView btnMore;

    private String mLatestVersion = "";
    private String mDownloadUrl = "";
    private String mChangelog = "";
    private long mFileSize = 0;
    private boolean mHasUpdate = false;
    private boolean mIsDownloaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnBack = findViewById(R.id.btn_back);
        btnMore = findViewById(R.id.btn_more);
        tvCurrentVersion = findViewById(R.id.tv_current_version);
        tvBuildId = findViewById(R.id.tv_build_id);
        tvSecurityPatch = findViewById(R.id.tv_security_patch);
        tvDeviceModel = findViewById(R.id.tv_device_model);
        tvStatusTitle = findViewById(R.id.tv_status_title);
        tvStatusDesc = findViewById(R.id.tv_status_desc);
        tvChangelog = findViewById(R.id.tv_changelog);
        layoutChangelogBox = findViewById(R.id.layout_changelog_box);
        pbDownload = findViewById(R.id.pb_download);
        tvProgress = findViewById(R.id.tv_progress);
        btnAction = findViewById(R.id.btn_action);

        loadSystemInfo();

        btnBack.setOnClickListener(v -> finish());
        btnMore.setOnClickListener(v -> showMoreOptions());

        btnAction.setOnClickListener(v -> {
            if (mIsDownloaded) {
                confirmInstall();
            } else if (mHasUpdate) {
                startDownload();
            } else {
                checkForUpdates();
            }
        });
    }

    private void loadSystemInfo() {
        String displayId = getProp("ro.build.display.id", "ColorOS 17 (Android 17)");
        String incremental = getProp("ro.build.version.incremental", "OPD2404_17.0.0.102(CN01)");
        String securityPatch = getProp("ro.build.version.security_patch", "2026-09-01");
        String device = getProp("ro.product.device", "ruan");

        tvCurrentVersion.setText("Version: ColorOS 17 (Android 17)");
        tvBuildId.setText("Build: " + incremental);
        tvSecurityPatch.setText("Security Patch: " + securityPatch);
        tvDeviceModel.setText("Redmi Pad Pro 5G (" + device + ")");
        
        tvStatusTitle.setText("Up to date");
        tvStatusDesc.setText("Already Looking for update's huh? 🤦");
        btnAction.setText("Check for Updates");
        btnAction.setBackgroundResource(R.drawable.bg_ios_glass_button);
    }

    private String getProp(String key, String def) {
        try {
            Class<?> sp = Class.forName("android.os.SystemProperties");
            Method get = sp.getMethod("get", String.class, String.class);
            return (String) get.invoke(null, key, def);
        } catch (Exception e) {
            return def;
        }
    }

    private void showMoreOptions() {
        new AlertDialog.Builder(this)
                .setTitle("Nebula OS Updates")
                .setMessage("Nebula OS • Powered by ColorOS 17\nVersion: 17.0\nRecovery Engine: OrangeFox / TWRP\nOfficial Endpoint: github.com/xstacy666/NebulaOS_ruan_ota\n\nCredits:\n• Øpium | Team Nebula\n• @Noble_actual117\n• @GAURA_140110\n• @NotYoursAditya\n• @Gignigga\n• Antigravity")
                .setPositiveButton("Close", null)
                .show();
    }

    private void checkForUpdates() {
        btnAction.setEnabled(false);
        btnAction.setText("Checking for updates...");
        tvStatusTitle.setText("Connecting...");

        new CheckUpdateTask().execute(OTA_URLS);
    }

    private class CheckUpdateTask extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... urls) {
            for (String targetUrl : urls) {
                try {
                    URL url = new URL(targetUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.setRequestMethod("GET");
                    if (conn.getResponseCode() == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) response.append(line);
                        in.close();
                        conn.disconnect();
                        return new JSONObject(response.toString());
                    }
                    conn.disconnect();
                } catch (Exception ignored) {}
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            btnAction.setEnabled(true);
            if (result == null) {
                showUpToDate();
                return;
            }

            try {
                JSONArray responseArray = result.optJSONArray("response");
                if (responseArray == null || responseArray.length() == 0) {
                    showUpToDate();
                    return;
                }

                JSONObject updateObj = responseArray.getJSONObject(0);
                mLatestVersion = updateObj.optString("version", "");
                mDownloadUrl = updateObj.optString("url", "");
                mChangelog = updateObj.optString("changelog", "• Nebula OS System Enhancements\n• Ultra Smooth Aquamorphic Animations\n• System Stability & Performance Tuning");
                mFileSize = updateObj.optLong("size", 0);

                if (mLatestVersion.isEmpty() || mDownloadUrl.isEmpty()) {
                    showUpToDate();
                    return;
                }

                mHasUpdate = true;
                tvStatusTitle.setText("New Update Available");
                tvStatusDesc.setText("Version " + mLatestVersion + " is ready to install.");

                if (!mChangelog.isEmpty()) {
                    layoutChangelogBox.setVisibility(View.VISIBLE);
                    tvChangelog.setText("Changelog:\n" + mChangelog);
                }

                btnAction.setText("Download Update");
                btnAction.setBackgroundResource(R.drawable.bg_ios_glass_button_primary);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing OTA JSON", e);
                showUpToDate();
            }
        }

        private void showUpToDate() {
            tvStatusTitle.setText("Up to date");
            tvStatusDesc.setText("Already Looking for update's huh? 🤦");
            btnAction.setText("Check for Updates");
            btnAction.setBackgroundResource(R.drawable.bg_ios_glass_button);
            layoutChangelogBox.setVisibility(View.GONE);
            Toast.makeText(MainActivity.this, "System is up to date", Toast.LENGTH_SHORT).show();
        }
    }

    private void startDownload() {
        btnAction.setEnabled(false);
        btnAction.setText("Downloading...");
        pbDownload.setVisibility(View.VISIBLE);
        tvProgress.setVisibility(View.VISIBLE);
        pbDownload.setProgress(0);

        new DownloadTask().execute(mDownloadUrl);
    }

    private class DownloadTask extends AsyncTask<String, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(String... urls) {
            String downloadUrl = urls[0];
            if (downloadUrl == null || downloadUrl.isEmpty()) {
                return false;
            }

            // Normalize Google Drive links to direct download
            downloadUrl = normalizeGoogleDriveUrl(downloadUrl);

            InputStream in = null;
            OutputStream out = null;
            HttpURLConnection conn = null;
            try {
                int redirects = 0;
                while (redirects < 5) {
                    URL url = new URL(downloadUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setInstanceFollowRedirects(true);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 NebulaOS-Updater/17.0");
                    conn.connect();

                    int status = conn.getResponseCode();
                    if (status == HttpURLConnection.HTTP_MOVED_TEMP || 
                        status == HttpURLConnection.HTTP_MOVED_PERM || 
                        status == HttpURLConnection.HTTP_SEE_OTHER) {
                        downloadUrl = conn.getHeaderField("Location");
                        conn.disconnect();
                        redirects++;
                        continue;
                    }
                    break;
                }

                if (conn == null || conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return false;
                }

                int fileLength = conn.getContentLength();
                in = conn.getInputStream();
                UPDATE_DEST.getParentFile().mkdirs();
                out = new FileOutputStream(UPDATE_DEST);

                byte[] data = new byte[8192];
                long total = 0;
                int count;
                while ((count = in.read(data)) != -1) {
                    if (isCancelled()) {
                        in.close();
                        return false;
                    }
                    total += count;
                    if (fileLength > 0) {
                        publishProgress((int) (total * 100 / fileLength));
                    }
                    out.write(data, 0, count);
                }
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Download failed", e);
                return false;
            } finally {
                try {
                    if (out != null) out.close();
                    if (in != null) in.close();
                } catch (Exception ignored) {}
                if (conn != null) conn.disconnect();
            }
        }

        private String normalizeGoogleDriveUrl(String url) {
            if (url.contains("drive.google.com")) {
                Pattern p = Pattern.compile("d/([a-zA-Z0-9_-]+)|id=([a-zA-Z0-9_-]+)");
                Matcher m = p.matcher(url);
                if (m.find()) {
                    String fileId = m.group(1) != null ? m.group(1) : m.group(2);
                    return "https://drive.usercontent.google.com/download?id=" + fileId + "&export=download&confirm=t";
                }
            }
            return url;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            pbDownload.setProgress(progress[0]);
            tvProgress.setText("Downloading update: " + progress[0] + "%");
        }

        @Override
        protected void onPostExecute(Boolean success) {
            btnAction.setEnabled(true);
            pbDownload.setVisibility(View.GONE);
            tvProgress.setVisibility(View.GONE);

            if (success) {
                mIsDownloaded = true;
                tvStatusTitle.setText("Update Ready to Install");
                tvStatusDesc.setText("Recovery package downloaded. Ready for OrangeFox flashing.");
                btnAction.setText("Install via OrangeFox Recovery");
                btnAction.setBackgroundResource(R.drawable.bg_ios_glass_button_primary);
            } else {
                Toast.makeText(MainActivity.this, "Download failed. Check your internet connection.", Toast.LENGTH_LONG).show();
                btnAction.setText("Download Update");
            }
        }
    }

    private void confirmInstall() {
        new AlertDialog.Builder(this)
                .setTitle("Install via OrangeFox Recovery")
                .setMessage("Your tablet will prepare the recovery command and reboot into OrangeFox Recovery to flash the update.\n\nYour apps, settings, and personal data will NOT be wiped.\n\nProceed?")
                .setPositiveButton("Reboot & Install", (dialog, which) -> executeInstall())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeInstall() {
        Toast.makeText(this, "Preparing OrangeFox recovery script & rebooting...", Toast.LENGTH_SHORT).show();
        String zipPath = "/data/media/0/Download/" + UPDATE_FILE_NAME;
        boolean ok = InstallHelper.rebootToRecovery(this, zipPath);
        if (!ok) {
            Toast.makeText(this, "Failed to reboot automatically. Please reboot into OrangeFox Recovery manually to flash: " + UPDATE_DEST.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
    }
}
