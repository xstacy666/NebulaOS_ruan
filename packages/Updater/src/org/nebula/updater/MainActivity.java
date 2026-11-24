package org.nebula.updater;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.OpenableColumns;
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
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private static final String TAG = "NebulaUpdater";
    private static final int REQUEST_LOCAL_UPDATE = 1001;

    // Official GitHub OTA Endpoints
    private static final String[] OTA_URLS = {
        "https://raw.githubusercontent.com/xstacy666/NebulaOS_ruan_ota/main/ota.json",
        "https://raw.githubusercontent.com/xstacy666/AERA-ruan/main/ota.json"
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

        // Schedule periodic background OTA job
        OtaBootReceiver.scheduleOtaJob(this);

        // Auto-nuke any downloaded updates older than 1 month (30 days)
        OtaCheckJobService.nukeExpiredUpdates();

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
                confirmInstall(UPDATE_DEST.getAbsolutePath());
            } else if (mHasUpdate) {
                startDownload();
            } else {
                checkForUpdates();
            }
        });

        // If update file already exists in Download and is fresh (<30 days), offer install
        if (UPDATE_DEST.exists() && UPDATE_DEST.length() > 1000000) {
            mIsDownloaded = true;
            tvStatusTitle.setText("Update Ready to Install");
            tvStatusDesc.setText("Recovery package ready in Download: " + (UPDATE_DEST.length() / (1024 * 1024)) + " MB");
            btnAction.setText("Install via OrangeFox Recovery");
            btnAction.setBackgroundResource(R.drawable.bg_ios_glass_button_primary);
        }
    }

    private void loadSystemInfo() {
        String hw = getProp("ro.boot.hwname", "ruan");
        String noril = getProp("ro.radio.noril", "no");
        String deviceName = "POCO Pad 5G (" + hw + ")";
        if ("yes".equals(noril) || "dizi".equals(hw)) {
            deviceName = "Redmi Pad Pro (" + hw + ")";
        }

        String incremental = getProp("ro.build.version.incremental", "NebulaOS_17.0");
        String securityPatch = getProp("ro.build.version.security_patch", "2026-09-01");

        tvCurrentVersion.setText("Version: NebulaOS 17.0 (Android 15)");
        tvBuildId.setText("Build: " + incremental);
        tvSecurityPatch.setText("Security Patch: " + securityPatch);
        tvDeviceModel.setText(deviceName);
        
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
        String[] options = {
            "Local update (Select .zip)",
            "Check for updates",
            "Delete downloaded update",
            "About Nebula OS"
        };

        new AlertDialog.Builder(this)
                .setTitle("Update Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            pickLocalUpdateZip();
                            break;
                        case 1:
                            checkForUpdates();
                            break;
                        case 2:
                            deleteDownloadedUpdate();
                            break;
                        case 3:
                            showAboutDialog();
                            break;
                    }
                })
                .show();
    }

    private void pickLocalUpdateZip() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select Recovery Update ZIP"), REQUEST_LOCAL_UPDATE);
        } catch (Exception e) {
            Toast.makeText(this, "No file manager found: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_LOCAL_UPDATE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                handleLocalUpdateUri(uri);
            }
        }
    }

    private void handleLocalUpdateUri(Uri uri) {
        String path = uri.getPath();
        if (path != null && path.contains(":")) {
            String[] parts = path.split(":");
            if (parts.length > 1) {
                path = "/sdcard/" + parts[1];
            }
        }
        
        File f = new File(path);
        if (!f.exists()) {
            // Copy to Download if virtual URI
            path = copyUriToDownload(uri);
        }

        if (path != null && new File(path).exists()) {
            confirmInstall(path);
        } else {
            Toast.makeText(this, "Could not resolve file path: " + uri, Toast.LENGTH_LONG).show();
        }
    }

    private String copyUriToDownload(Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            File dest = new File("/sdcard/Download/local_update.zip");
            OutputStream out = new FileOutputStream(dest);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close();
            out.close();
            return dest.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy local zip", e);
            return null;
        }
    }

    private void deleteDownloadedUpdate() {
        if (UPDATE_DEST.exists()) {
            boolean deleted = UPDATE_DEST.delete();
            if (deleted) {
                Toast.makeText(this, "Downloaded update deleted successfully", Toast.LENGTH_SHORT).show();
                mIsDownloaded = false;
                loadSystemInfo();
            } else {
                Toast.makeText(this, "Failed to delete update file", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No downloaded update found", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Nebula OS Updates")
                .setMessage("Nebula OS • Powered by ColorOS 17\nVersion: 17.0 (Android 15)\nRecovery Engine: OrangeFox / TWRP\nOfficial Endpoint: github.com/xstacy666/NebulaOS_ruan_ota\n\nMaintainers:\n• Øpium | Team Nebula\n• @Noble_actual117\n• @GAURA_140110\n• @NotYoursAditya\n• @Gignigga")
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
                    conn.setConnectTimeout(6000);
                    conn.setReadTimeout(6000);
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("User-Agent", "NebulaOS-Updater/17.0");

                    int code = conn.getResponseCode();
                    if (code == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        reader.close();

                        JSONObject root = new JSONObject(sb.toString());
                        JSONArray updates = root.optJSONArray("response");
                        if (updates != null && updates.length() > 0) {
                            return updates.getJSONObject(0);
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed checking " + targetUrl + ": " + e.getMessage());
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject update) {
            btnAction.setEnabled(true);
            if (update != null) {
                mLatestVersion = update.optString("version", "17.0.0");
                String versionName = update.optString("version_name", "Nebula OS " + mLatestVersion);
                mDownloadUrl = update.optString("url", "");
                mChangelog = update.optString("changelog", "System improvements and performance optimizations.");
                mFileSize = update.optLong("size", 0);

                String currentVer = getProp("ro.build.version.incremental", "");
                if (!mLatestVersion.equals(currentVer)) {
                    mHasUpdate = true;
                    tvStatusTitle.setText(versionName);
                    String sizeText = mFileSize > 0 ? " • " + (mFileSize / (1024 * 1024)) + " MB" : "";
                    tvStatusDesc.setText("New build available" + sizeText);
                    
                    tvChangelog.setText(mChangelog);
                    layoutChangelogBox.setVisibility(View.VISIBLE);

                    btnAction.setText("Download Update");
                    btnAction.setBackgroundResource(R.drawable.bg_ios_glass_button_primary);
                    return;
                }
            }

            mHasUpdate = false;
            tvStatusTitle.setText("Up to date");
            tvStatusDesc.setText("Already Looking for update's huh? 🤦");
            layoutChangelogBox.setVisibility(View.GONE);
            btnAction.setText("Check for Updates");
            btnAction.setBackgroundResource(R.drawable.bg_ios_glass_button);
            Toast.makeText(MainActivity.this, "Your system is already up to date!", Toast.LENGTH_SHORT).show();
        }
    }

    private void startDownload() {
        if (mDownloadUrl.isEmpty()) {
            Toast.makeText(this, "No download URL available", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAction.setEnabled(false);
        btnAction.setText("Preparing download...");
        pbDownload.setVisibility(View.VISIBLE);
        tvProgress.setVisibility(View.VISIBLE);
        pbDownload.setProgress(0);

        new DownloadTask().execute(mDownloadUrl);
    }

    private class DownloadTask extends AsyncTask<String, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(String... urls) {
            InputStream in = null;
            OutputStream out = null;
            HttpURLConnection conn = null;

            try {
                CookieHandler.setDefault(new CookieManager());
                String downloadUrl = normalizeGoogleDriveUrl(urls[0]);

                int redirects = 0;
                while (redirects < 6) {
                    URL url = new URL(downloadUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setInstanceFollowRedirects(true);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Tablet) NebulaOS-OTA/17.0");
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

                byte[] data = new byte[16384];
                long total = 0;
                int count;
                long lastUpdate = System.currentTimeMillis();

                while ((count = in.read(data)) != -1) {
                    if (isCancelled()) {
                        in.close();
                        return false;
                    }
                    total += count;
                    long now = System.currentTimeMillis();
                    if (fileLength > 0 && (now - lastUpdate > 300)) {
                        publishProgress((int) (total * 100 / fileLength));
                        lastUpdate = now;
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
                tvStatusDesc.setText("Recovery package downloaded (" + (UPDATE_DEST.length() / (1024 * 1024)) + " MB)");
                btnAction.setText("Install via OrangeFox Recovery");
                btnAction.setBackgroundResource(R.drawable.bg_ios_glass_button_primary);
            } else {
                Toast.makeText(MainActivity.this, "Download failed. Check your internet connection.", Toast.LENGTH_LONG).show();
                btnAction.setText("Download Update");
            }
        }
    }

    private void confirmInstall(String zipPath) {
        new AlertDialog.Builder(this)
                .setTitle("Install via OrangeFox Recovery")
                .setMessage("Your tablet will configure the recovery commands and reboot into OrangeFox Recovery to install the update.\n\nFile:\n" + zipPath + "\n\nApps, settings, and personal data will NOT be wiped.\n\nProceed?")
                .setPositiveButton("Reboot & Install", (dialog, which) -> executeInstall(zipPath))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeInstall(String zipPath) {
        Toast.makeText(this, "Rebooting to OrangeFox Recovery...", Toast.LENGTH_SHORT).show();
        boolean ok = InstallHelper.rebootToRecovery(this, zipPath);
        if (!ok) {
            Toast.makeText(this, "Could not reboot automatically. Please reboot to recovery manually to flash: " + zipPath, Toast.LENGTH_LONG).show();
        }
    }
}
