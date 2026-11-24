package org.nebula.updater;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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
import java.net.URLEncoder;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private static final String TAG = "NebulaUpdater";
    private static final int REQUEST_LOCAL_UPDATE = 1001;

    // Official GitHub OTA Manifest Endpoints
    public static final String OTA_URL = "https://api.github.com/repos/xstacy666/NebulaOS_ruan_ota/contents/ota.json";
    public static final String RAW_OTA_URL = "https://raw.githubusercontent.com/xstacy666/NebulaOS_ruan_ota/main/ota.json";
    private static final String FALLBACK_OTA_URL = "https://raw.githubusercontent.com/xstacy666/AERA-ruan/main/ota.json";

    public static final String DEFAULT_UPDATE_FILE = "ColorOS17_ruan_dizi_Update.zip";

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
    private Button btnPreviousUpdates;
    private ImageView btnBack;
    private ImageView btnMore;

    private String mLatestVersion = "";
    private long mLatestVersionCode = 0;
    private String mDownloadUrl = "";
    private String mChangelog = "";
    private long mFileSize = 0;
    private String mFileName = DEFAULT_UPDATE_FILE;
    private File mUpdateDest = new File("/sdcard/Download/" + DEFAULT_UPDATE_FILE);
    private boolean mHasUpdate = false;
    private boolean mIsDownloaded = false;
    private boolean mIsDownloading = false;
    private DownloadTask mCurrentDownloadTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Schedule periodic background OTA check (every 6 hours)
        OtaBootReceiver.scheduleOtaJob(this);

        // Auto-nuke downloaded updates older than 1 month (30 days)
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
        btnPreviousUpdates = findViewById(R.id.btn_previous_updates);

        loadSystemInfo();

        btnBack.setOnClickListener(v -> {
            if (mIsDownloading) {
                confirmCancelDownload();
            } else {
                finish();
            }
        });
        btnMore.setOnClickListener(v -> showMoreOptions());
        if (btnPreviousUpdates != null) {
            btnPreviousUpdates.setOnClickListener(v -> openPreviousUpdates());
        }

        btnAction.setOnClickListener(v -> {
            if (mIsDownloaded) {
                if (mFileName.endsWith(".apk")) {
                    installApk(this, mUpdateDest.getAbsolutePath());
                } else {
                    confirmInstall(this, mUpdateDest.getAbsolutePath());
                }
            } else if (mIsDownloading) {
                confirmCancelDownload();
            } else if (mHasUpdate) {
                startDownload();
            } else {
                checkForUpdates();
            }
        });

        // Check if update file already exists in Download and is intact (>10KB)
        checkExistingDownloadedFile();

        // Process Intent extras (e.g. from Notification)
        handleIntent(getIntent());
    }

    private void checkExistingDownloadedFile() {
        if (mUpdateDest.exists() && mUpdateDest.length() > 10240) {
            mIsDownloaded = true;
            tvStatusTitle.setText("Update Ready to Install");
            long sizeKb = mUpdateDest.length() / 1024;
            String sizeStr = sizeKb > 1024 ? (sizeKb / 1024) + " MB" : sizeKb + " KB";
            tvStatusDesc.setText("Package ready in Download: " + sizeStr);

            if (mFileName.endsWith(".apk")) {
                btnAction.setText("Install Update (.apk)");
            } else {
                btnAction.setText("Install via OrangeFox Recovery");
            }
            btnAction.setBackgroundResource(R.drawable.bg_ios_glass_button_primary);
        }
    }

    @Override
    public void onBackPressed() {
        if (mIsDownloading) {
            confirmCancelDownload();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;

        boolean autoDownload = intent.getBooleanExtra("auto_download", false);
        String targetUrl = intent.getStringExtra("target_url");
        String version = intent.getStringExtra("version");
        String filename = intent.getStringExtra("filename");

        if (targetUrl != null && !targetUrl.isEmpty()) {
            mDownloadUrl = targetUrl;
        }
        if (version != null && !version.isEmpty()) {
            mLatestVersion = version;
        }
        if (filename != null && !filename.isEmpty()) {
            mFileName = filename;
            mUpdateDest = new File("/sdcard/Download/" + mFileName);
        }

        if (autoDownload && !mDownloadUrl.isEmpty() && !mIsDownloaded && !mIsDownloading) {
            Log.i(TAG, "auto_download triggered from notification: " + mDownloadUrl);
            mHasUpdate = true;
            startDownload();
        } else if (!mIsDownloaded && !mIsDownloading) {
            // Auto-check on launch
            checkForUpdates();
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
        String nebulaVer = getProp("ro.nebula.version", "17.0");

        tvCurrentVersion.setText("Version: NebulaOS " + nebulaVer + " (Android 15)");
        tvBuildId.setText("Build: " + incremental);
        tvSecurityPatch.setText("Security Patch: " + securityPatch);
        tvDeviceModel.setText(deviceName);

        tvStatusTitle.setText("Up to date");
        tvStatusDesc.setText("Already Looking for update's huh? 🤦");
        btnAction.setText("Check for Updates");
        btnAction.setBackgroundResource(R.drawable.bg_ios_glass_button);
    }

    public static String getProp(String key, String def) {
        try {
            Class<?> sp = Class.forName("android.os.SystemProperties");
            Method get = sp.getMethod("get", String.class, String.class);
            return (String) get.invoke(null, key, def);
        } catch (Exception e) {
            return def;
        }
    }

    public static long getLongProp(String key, long def) {
        try {
            Class<?> sp = Class.forName("android.os.SystemProperties");
            Method get = sp.getMethod("getLong", String.class, long.class);
            return (Long) get.invoke(null, key, def);
        } catch (Exception e) {
            return def;
        }
    }

    private void showMoreOptions() {
        String[] options = {
            "Previous updates (History & Rollback)",
            "Local update (Select .zip or .apk)",
            "Check for updates",
            "Open Download link in Browser",
            "Delete downloaded update",
            "About Nebula OS"
        };

        new AlertDialog.Builder(this)
                .setTitle("Update Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openPreviousUpdates();
                            break;
                        case 1:
                            pickLocalUpdateZip();
                            break;
                        case 2:
                            checkForUpdates();
                            break;
                        case 3:
                            openUrlInBrowser();
                            break;
                        case 4:
                            deleteDownloadedUpdate();
                            break;
                        case 5:
                            showAboutDialog();
                            break;
                    }
                })
                .show();
    }

    public void openPreviousUpdates() {
        Intent intent = new Intent(this, PreviousUpdatesActivity.class);
        startActivity(intent);
    }

    private void openUrlInBrowser() {
        String url = (mDownloadUrl != null && !mDownloadUrl.isEmpty()) ? mDownloadUrl : "https://github.com/xstacy666/NebulaOS_ruan_ota";
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open browser: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void pickLocalUpdateZip() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select Update Package"), REQUEST_LOCAL_UPDATE);
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

        File f = (path != null) ? new File(path) : null;
        if (f == null || !f.exists()) {
            path = copyUriToDownload(uri);
        }

        if (path != null && new File(path).exists()) {
            if (path.endsWith(".apk")) {
                installApk(this, path);
            } else {
                confirmInstall(this, path);
            }
        } else {
            Toast.makeText(this, "Could not resolve file path: " + uri, Toast.LENGTH_LONG).show();
        }
    }

    private String copyUriToDownload(Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            File dest = new File("/sdcard/Download/local_update.zip");
            OutputStream out = new FileOutputStream(dest);
            byte[] buf = new byte[16384];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close();
            out.close();
            return dest.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy local package", e);
            return null;
        }
    }

    private void deleteDownloadedUpdate() {
        boolean anyDeleted = false;
        if (mUpdateDest.exists()) {
            anyDeleted = mUpdateDest.delete();
        }
        File defaultZip = new File("/sdcard/Download/" + DEFAULT_UPDATE_FILE);
        if (defaultZip.exists()) {
            anyDeleted |= defaultZip.delete();
        }
        File tmp = new File(mUpdateDest.getAbsolutePath() + ".tmp");
        if (tmp.exists()) {
            tmp.delete();
        }
        File localZip = new File("/sdcard/Download/local_update.zip");
        if (localZip.exists()) {
            localZip.delete();
        }

        if (anyDeleted) {
            Toast.makeText(this, "Downloaded update deleted successfully", Toast.LENGTH_SHORT).show();
            mIsDownloaded = false;
            loadSystemInfo();
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

    public void checkForUpdates() {
        if (mIsDownloading) return;
        btnAction.setEnabled(false);
        btnAction.setText("Checking for updates...");
        tvStatusTitle.setText("Connecting to Nebula...");

        new CheckUpdateTask().execute(OTA_URL, FALLBACK_OTA_URL);
    }

    private class CheckUpdateTask extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... urls) {
            for (String targetUrl : urls) {
                try {
                    String queryUrl = targetUrl + "?t=" + System.currentTimeMillis(); URL url = new URL(queryUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("User-Agent", "NebulaOS-Updater/17.0");
                    conn.setRequestProperty("Accept", "application/vnd.github.v3.raw, application/json, */*");

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
                mLatestVersionCode = update.optLong("version_code", 0);
                String versionName = update.optString("version_name", "Nebula OS " + mLatestVersion);
                mDownloadUrl = update.optString("url", "");
                mChangelog = update.optString("changelog", "System improvements and performance optimizations.");
                mFileSize = update.optLong("size", 0);
                mFileName = update.optString("filename", DEFAULT_UPDATE_FILE);
                mUpdateDest = new File("/sdcard/Download/" + mFileName);

                if (isNewUpdate(mLatestVersion, mLatestVersionCode)) {
                    mHasUpdate = true;
                    tvStatusTitle.setText(versionName);
                    String sizeText = "";
                    if (mFileSize > 1048576) {
                        sizeText = " • " + (mFileSize / (1024 * 1024)) + " MB";
                    } else if (mFileSize > 0) {
                        sizeText = " • " + (mFileSize / 1024) + " KB";
                    }
                    tvStatusDesc.setText("New build available" + sizeText);

                    tvChangelog.setText(mChangelog);
                    layoutChangelogBox.setVisibility(View.VISIBLE);

                    checkExistingDownloadedFile();
                    if (!mIsDownloaded) {
                        btnAction.setText("Download Update");
                        btnAction.setBackgroundResource(R.drawable.bg_ios_glass_button_primary);
                    }
                    return;
                }
            }

            mHasUpdate = false;
            tvStatusTitle.setText("Up to date");
            tvStatusDesc.setText("Already Looking for update's huh? 🤦");
            layoutChangelogBox.setVisibility(View.GONE);
            btnAction.setText("Check for Updates");
            btnAction.setBackgroundResource(R.drawable.bg_ios_glass_button);
            Toast.makeText(MainActivity.this, "Your system is up to date!", Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean isNewUpdate(String remoteVer, long remoteCode) {
        long currentCode = getLongProp("ro.nebula.version.code", 0);
        if (remoteCode > 0 && currentCode > 0) {
            return remoteCode > currentCode;
        }

        String currentVer = getProp("ro.nebula.version", "");
        if (currentVer.isEmpty()) {
            currentVer = getProp("ro.build.version.incremental", "");
        }

        if (remoteVer == null || remoteVer.isEmpty()) return false;
        return !remoteVer.trim().equalsIgnoreCase(currentVer.trim());
    }

    private void startDownload() {
        if (mDownloadUrl.isEmpty()) {
            Toast.makeText(this, "No download URL available in update manifest", Toast.LENGTH_SHORT).show();
            return;
        }

        mIsDownloading = true;
        btnAction.setEnabled(true);
        btnAction.setText("Cancel Download");
        btnAction.setBackgroundResource(R.drawable.bg_ios_glass_button_cancel);
        pbDownload.setVisibility(View.VISIBLE);
        tvProgress.setVisibility(View.VISIBLE);
        pbDownload.setProgress(0);
        tvProgress.setText("Connecting to download server...");

        mCurrentDownloadTask = new DownloadTask();
        mCurrentDownloadTask.execute(mDownloadUrl);
    }

    private void confirmCancelDownload() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Download")
                .setMessage("Are you sure you want to cancel downloading this update?")
                .setPositiveButton("Stop Download", (dialog, which) -> cancelDownload())
                .setNegativeButton("Keep Downloading", null)
                .show();
    }

    private void cancelDownload() {
        if (mCurrentDownloadTask != null) {
            mCurrentDownloadTask.cancel(true);
            mCurrentDownloadTask = null;
        }
        mIsDownloading = false;
        pbDownload.setVisibility(View.GONE);
        tvProgress.setVisibility(View.GONE);
        tvStatusDesc.setText("Download cancelled. Tap to download again.");
        btnAction.setEnabled(true);
        btnAction.setText("Download Update");
        btnAction.setBackgroundResource(R.drawable.bg_ios_glass_button_primary);
        Toast.makeText(this, "Download cancelled", Toast.LENGTH_SHORT).show();

        // Clean up partial .tmp file
        File tmp = new File(mUpdateDest.getAbsolutePath() + ".tmp");
        if (tmp.exists()) {
            tmp.delete();
        }
    }

    private class DownloadTask extends AsyncTask<String, Object, Boolean> {
        private String mErrorMessage = "Download failed";
        private File mTempFile;

        @Override
        protected Boolean doInBackground(String... urls) {
            InputStream in = null;
            OutputStream out = null;
            HttpURLConnection conn = null;

            try {
                // Keep cookies across redirects (required for Google Drive large files)
                CookieManager cookieManager = new CookieManager();
                CookieHandler.setDefault(cookieManager);

                String currentUrl = urls[0];
                Log.i(TAG, "Starting OTA download: " + currentUrl);

                // 1. Resolve direct Google Drive link if applicable
                String resolvedUrl = resolveGoogleDriveStreamUrl(currentUrl);

                int redirects = 0;
                while (redirects < 10) {
                    if (isCancelled()) return false;
                    URL url = new URL(resolvedUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setInstanceFollowRedirects(true);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(30000);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 15; Tablet) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                    conn.connect();

                    int status = conn.getResponseCode();
                    if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                        status == HttpURLConnection.HTTP_MOVED_PERM ||
                        status == HttpURLConnection.HTTP_SEE_OTHER) {
                        resolvedUrl = conn.getHeaderField("Location");
                        conn.disconnect();
                        redirects++;
                        continue;
                    }
                    break;
                }

                if (isCancelled()) return false;

                if (conn == null || conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    mErrorMessage = "HTTP error: " + (conn != null ? conn.getResponseCode() : -1);
                    return false;
                }

                String contentType = conn.getContentType();
                Log.i(TAG, "Connection established. Content-Type: " + contentType + ", Code: " + conn.getResponseCode());

                // If Google Drive returned the virus scan warning HTML page, parse the confirmation link
                if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("text/html") && resolvedUrl.contains("drive.google")) {
                    Log.i(TAG, "Received HTML page from Google Drive, parsing virus confirmation...");
                    String confirmationUrl = parseGoogleDriveConfirmation(conn, resolvedUrl);
                    conn.disconnect();

                    if (confirmationUrl == null) {
                        mErrorMessage = "Google Drive link requires manual browser confirmation or is private.";
                        return false;
                    }

                    if (isCancelled()) return false;

                    Log.i(TAG, "Connecting to confirmation URL: " + confirmationUrl);
                    URL url = new URL(confirmationUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setInstanceFollowRedirects(true);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(30000);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 15; Tablet) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                    conn.connect();

                    if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        mErrorMessage = "Confirmation failed with HTTP " + conn.getResponseCode();
                        return false;
                    }
                }

                if (isCancelled()) return false;

                long fileLength = conn.getContentLengthLong();
                if (fileLength <= 0 && mFileSize > 0) {
                    fileLength = mFileSize;
                }

                in = conn.getInputStream();
                mTempFile = new File(mUpdateDest.getAbsolutePath() + ".tmp");
                mTempFile.getParentFile().mkdirs();
                out = new FileOutputStream(mTempFile);

                byte[] data = new byte[32768];
                long total = 0;
                int count;
                long lastUiUpdate = System.currentTimeMillis();
                long startTime = System.currentTimeMillis();

                while ((count = in.read(data)) != -1) {
                    if (isCancelled()) {
                        in.close();
                        out.close();
                        if (mTempFile.exists()) mTempFile.delete();
                        return false;
                    }
                    total += count;
                    out.write(data, 0, count);

                    long now = System.currentTimeMillis();
                    if (now - lastUiUpdate > 350) {
                        int percent = (fileLength > 0) ? (int) (total * 100 / fileLength) : 0;
                        double speedMb = ((double) total / (1024 * 1024)) / ((now - startTime + 1) / 1000.0);
                        publishProgress(percent, total, fileLength, speedMb);
                        lastUiUpdate = now;
                    }
                }

                out.flush();
                out.close();
                in.close();

                if (isCancelled()) {
                    if (mTempFile.exists()) mTempFile.delete();
                    return false;
                }

                // Sanity check: Ensure downloaded file is valid (>5KB) and rename .tmp
                if (mTempFile.exists() && mTempFile.length() > 5120) {
                    if (mUpdateDest.exists()) {
                        mUpdateDest.delete();
                    }
                    boolean renamed = mTempFile.renameTo(mUpdateDest);
                    if (renamed) {
                        return true;
                    } else {
                        mErrorMessage = "Failed to finalize downloaded package";
                        return false;
                    }
                } else {
                    mErrorMessage = "Downloaded file incomplete or invalid (" + (mTempFile.exists() ? mTempFile.length() : 0) + " bytes)";
                    mTempFile.delete();
                    return false;
                }

            } catch (Exception e) {
                Log.e(TAG, "Download error", e);
                mErrorMessage = "Error: " + e.getMessage();
                return false;
            } finally {
                try {
                    if (out != null) out.close();
                    if (in != null) in.close();
                } catch (Exception ignored) {}
                if (conn != null) conn.disconnect();
            }
        }

        private String resolveGoogleDriveStreamUrl(String url) {
            if (url.contains("drive.google.com") || url.contains("drive.usercontent.google.com")) {
                Pattern p = Pattern.compile("d/([a-zA-Z0-9_-]{20,})|id=([a-zA-Z0-9_-]{20,})");
                Matcher m = p.matcher(url);
                if (m.find()) {
                    String fileId = m.group(1) != null ? m.group(1) : m.group(2);
                    return "https://drive.usercontent.google.com/download?id=" + fileId + "&export=download&confirm=t";
                }
            }
            return url;
        }

        private String parseGoogleDriveConfirmation(HttpURLConnection conn, String originalUrl) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();
                String html = sb.toString();

                // 1. Look for <form id="download-form" action="..." ...>
                Pattern formPattern = Pattern.compile("<form[^>]*id=[\"']download-form[\"'][^>]*action=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
                Matcher fm = formPattern.matcher(html);
                if (fm.find()) {
                    String action = fm.group(1);
                    StringBuilder params = new StringBuilder();
                    Pattern inputPattern = Pattern.compile("<input[^>]*name=[\"']([^\"']+)[\"'][^>]*value=[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE);
                    Matcher im = inputPattern.matcher(html);
                    while (im.find()) {
                        String name = im.group(1);
                        String val = im.group(2);
                        if (params.length() > 0) params.append("&");
                        params.append(URLEncoder.encode(name, "UTF-8")).append("=").append(URLEncoder.encode(val, "UTF-8"));
                    }
                    if (params.length() > 0) {
                        return action + "?" + params.toString();
                    }
                }

                // 2. Look for download link <a id="uc-download-link" href="...">
                Pattern linkPattern = Pattern.compile("id=[\"']uc-download-link[\"'][^>]*href=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
                Matcher lm = linkPattern.matcher(html);
                if (lm.find()) {
                    String href = lm.group(1).replace("&amp;", "&");
                    if (href.startsWith("/")) {
                        return "https://drive.google.com" + href;
                    }
                    return href;
                }

                // 3. Look for confirm & uuid tokens
                Matcher cm = Pattern.compile("confirm=([a-zA-Z0-9_-]+)").matcher(html);
                Matcher um = Pattern.compile("uuid=([a-zA-Z0-9_-]+)").matcher(html);
                if (cm.find()) {
                    String confirm = cm.group(1);
                    String uuid = um.find() ? um.group(1) : "";
                    String fileId = "";
                    Matcher fid = Pattern.compile("id=([a-zA-Z0-9_-]{20,})").matcher(originalUrl);
                    if (fid.find()) fileId = fid.group(1);
                    return "https://drive.usercontent.google.com/download?id=" + fileId + "&export=download&confirm=" + confirm + (!uuid.isEmpty() ? "&uuid=" + uuid : "");
                }
            } catch (Exception e) {
                Log.w(TAG, "Error parsing Google Drive confirmation HTML: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            int percent = (Integer) values[0];
            long total = (Long) values[1];
            long length = (Long) values[2];
            double speed = (Double) values[3];

            pbDownload.setProgress(percent);

            String progressText;
            if (length > 1048576) {
                long totalMb = total / (1024 * 1024);
                long lengthMb = length / (1024 * 1024);
                progressText = String.format(Locale.getDefault(), "Downloading: %d%% (%d / %d MB) • %.1f MB/s",
                        percent, totalMb, lengthMb, speed);
            } else {
                long totalKb = total / 1024;
                long lengthKb = length / 1024;
                progressText = String.format(Locale.getDefault(), "Downloading: %d%% (%d / %d KB) • %.1f MB/s",
                        percent, totalKb, lengthKb, speed);
            }
            tvProgress.setText(progressText);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mIsDownloading = false;
            pbDownload.setVisibility(View.GONE);
            tvProgress.setVisibility(View.GONE);
            if (mTempFile != null && mTempFile.exists()) {
                mTempFile.delete();
            }
            btnAction.setEnabled(true);
            btnAction.setText("Download Update");
            btnAction.setBackgroundResource(R.drawable.bg_ios_glass_button_primary);
            tvStatusDesc.setText("Download cancelled");
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mIsDownloading = false;
            btnAction.setEnabled(true);
            pbDownload.setVisibility(View.GONE);
            tvProgress.setVisibility(View.GONE);

            if (success) {
                mIsDownloaded = true;
                tvStatusTitle.setText("Update Ready to Install");
                long sizeKb = mUpdateDest.length() / 1024;
                String sizeStr = sizeKb > 1024 ? (sizeKb / 1024) + " MB" : sizeKb + " KB";
                tvStatusDesc.setText("Package downloaded (" + sizeStr + ")");

                if (mFileName.endsWith(".apk")) {
                    btnAction.setText("Install Update (.apk)");
                } else {
                    btnAction.setText("Install via OrangeFox Recovery");
                }
                btnAction.setBackgroundResource(R.drawable.bg_ios_glass_button_primary);
                Toast.makeText(MainActivity.this, "Download completed!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, mErrorMessage, Toast.LENGTH_LONG).show();
                btnAction.setText("Download Update");
                btnAction.setBackgroundResource(R.drawable.bg_ios_glass_button_primary);
                showDownloadErrorDialog(mErrorMessage);
            }
        }
    }

    private void showDownloadErrorDialog(String error) {
        new AlertDialog.Builder(this)
                .setTitle("Download Issue")
                .setMessage(error + "\n\nWould you like to open the download link in your browser instead?")
                .setPositiveButton("Open in Browser", (d, w) -> openUrlInBrowser())
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static void installApk(Activity activity, String apkPath) {
        File apkFile = new File(apkPath);
        if (!apkFile.exists()) {
            Toast.makeText(activity, "APK not found: " + apkPath, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(activity)
                .setTitle("Install Nebula OS Update")
                .setMessage("A system app update is ready to install.\n\nPackage:\n" + apkFile.getName() + " (" + (apkFile.length() / 1024) + " KB)\n\nProceed to install?")
                .setPositiveButton("Install", (d, w) -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        Uri apkUri = Uri.parse("content://org.nebula.updater.provider" + apkFile.getAbsolutePath());
                        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        activity.startActivity(intent);
                    } catch (Exception e) {
                        Log.w(TAG, "Direct intent install failed: " + e.getMessage());
                        Toast.makeText(activity, "Opening installer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static void confirmInstall(Activity activity, String zipPath) {
        new AlertDialog.Builder(activity)
                .setTitle("Install via OrangeFox Recovery")
                .setMessage("Your tablet will configure the recovery commands and reboot into OrangeFox Recovery to install the update.\n\nPackage:\n" + zipPath + "\n\nApps, settings, and personal data will NOT be wiped.\n\nProceed?")
                .setPositiveButton("Reboot & Install", (dialog, which) -> executeInstall(activity, zipPath))
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static void executeInstall(Activity activity, String zipPath) {
        Toast.makeText(activity, "Rebooting to OrangeFox Recovery...", Toast.LENGTH_SHORT).show();
        boolean ok = InstallHelper.rebootToRecovery(activity, zipPath);
        if (!ok) {
            Toast.makeText(activity, "Could not reboot automatically. Please reboot to recovery manually to flash: " + zipPath, Toast.LENGTH_LONG).show();
        }
    }
}
