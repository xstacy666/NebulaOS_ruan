package org.nebula.updater;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PreviousUpdatesActivity extends Activity {

    private static final String TAG = "PreviousUpdates";

    private ImageView btnBack;
    private ImageView btnRefresh;
    private ProgressBar pbLoading;
    private TextView tvEmptyNotice;
    private LinearLayout llUpdatesContainer;

    private long mCurrentSystemCode = 0;
    private final Map<String, ItemDownloadTask> mActiveDownloads = new HashMap<>();

    public static class UpdateItem {
        public String version;
        public long versionCode;
        public String versionName;
        public String date;
        public long size;
        public String filename;
        public String url;
        public String changelog;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_previous_updates);

        mCurrentSystemCode = MainActivity.getLongProp("ro.nebula.version.code", 102);

        btnBack = findViewById(R.id.btn_back);
        btnRefresh = findViewById(R.id.btn_refresh);
        pbLoading = findViewById(R.id.pb_loading);
        tvEmptyNotice = findViewById(R.id.tv_empty_notice);
        llUpdatesContainer = findViewById(R.id.ll_updates_container);

        btnBack.setOnClickListener(v -> finish());
        btnRefresh.setOnClickListener(v -> loadHistory());

        loadHistory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel any background downloads on exit
        for (ItemDownloadTask task : mActiveDownloads.values()) {
            if (task != null) {
                task.cancel(true);
            }
        }
        mActiveDownloads.clear();
    }

    private void loadHistory() {
        llUpdatesContainer.removeAllViews();
        tvEmptyNotice.setVisibility(View.GONE);
        pbLoading.setVisibility(View.VISIBLE);

        new FetchHistoryTask().execute(MainActivity.RAW_OTA_URL, MainActivity.OTA_URL);
    }

    private class FetchHistoryTask extends AsyncTask<String, Void, List<UpdateItem>> {
        @Override
        protected List<UpdateItem> doInBackground(String... urls) {
            for (String targetUrl : urls) {
                try {
                    String queryUrl = targetUrl + "?t=" + System.currentTimeMillis();
                    URL url = new URL(queryUrl);
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
                            List<UpdateItem> list = new ArrayList<>();
                            for (int i = 0; i < updates.length(); i++) {
                                JSONObject obj = updates.getJSONObject(i);
                                UpdateItem item = new UpdateItem();
                                item.version = obj.optString("version", "");
                                item.versionCode = obj.optLong("version_code", 0);
                                item.versionName = obj.optString("version_name", "Nebula OS " + item.version);
                                item.date = obj.optString("date", "");
                                item.size = obj.optLong("size", 0);
                                item.filename = obj.optString("filename", "update.apk");
                                item.url = obj.optString("url", "");
                                item.changelog = obj.optString("changelog", "System optimizations and stability fixes.");
                                list.add(item);
                            }
                            return list;
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error fetching from " + targetUrl + ": " + e.getMessage());
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<UpdateItem> items) {
            pbLoading.setVisibility(View.GONE);
            if (items == null || items.isEmpty()) {
                tvEmptyNotice.setVisibility(View.VISIBLE);
                return;
            }

            renderItems(items);
        }
    }

    private void renderItems(List<UpdateItem> items) {
        LayoutInflater inflater = LayoutInflater.from(this);

        for (UpdateItem item : items) {
            View card = inflater.inflate(R.layout.item_previous_update, llUpdatesContainer, false);

            TextView tvTitle = card.findViewById(R.id.tv_item_title);
            TextView tvBadge = card.findViewById(R.id.tv_item_badge);
            TextView tvSubtitle = card.findViewById(R.id.tv_item_subtitle);
            TextView tvChangelog = card.findViewById(R.id.tv_item_changelog);
            ProgressBar pbDownload = card.findViewById(R.id.pb_item_download);
            TextView tvProgress = card.findViewById(R.id.tv_item_progress);
            Button btnAction = card.findViewById(R.id.btn_item_action);

            tvTitle.setText(item.versionName);

            // Configure badge depending on whether build is current, newer, or older
            if (item.versionCode == mCurrentSystemCode) {
                tvBadge.setText("CURRENT VERSION");
                tvBadge.setBackgroundResource(R.drawable.bg_pill_current);
                tvBadge.setTextColor(0xFF00E676);
            } else if (item.versionCode > mCurrentSystemCode) {
                tvBadge.setText("LATEST UPDATE");
                tvBadge.setBackgroundResource(R.drawable.bg_pill_newer);
                tvBadge.setTextColor(0xFF00B4D8);
            } else {
                tvBadge.setText("PREVIOUS BUILD");
                tvBadge.setBackgroundResource(R.drawable.bg_pill_version);
                tvBadge.setTextColor(0xFFB0C4DE);
            }

            // Size text formatting
            String sizeStr = "";
            if (item.size > (1024 * 1024)) {
                sizeStr = (item.size / (1024 * 1024)) + " MB";
            } else if (item.size > 0) {
                sizeStr = (item.size / 1024) + " KB";
            }

            String subtitleText = "Version " + item.version;
            if (!item.date.isEmpty()) {
                subtitleText += " • " + item.date;
            }
            if (!sizeStr.isEmpty()) {
                subtitleText += " • " + sizeStr;
            }
            tvSubtitle.setText(subtitleText);

            tvChangelog.setText(item.changelog);

            // Check if file already downloaded
            File targetFile = new File("/sdcard/Download/" + item.filename);
            updateButtonState(item, targetFile, btnAction, pbDownload, tvProgress);

            btnAction.setOnClickListener(v -> {
                if (targetFile.exists() && targetFile.length() > 10240) {
                    if (item.filename.endsWith(".apk")) {
                        MainActivity.installApk(PreviousUpdatesActivity.this, targetFile.getAbsolutePath());
                    } else {
                        MainActivity.confirmInstall(PreviousUpdatesActivity.this, targetFile.getAbsolutePath());
                    }
                } else if (mActiveDownloads.containsKey(item.filename)) {
                    // Cancel active download
                    ItemDownloadTask task = mActiveDownloads.get(item.filename);
                    if (task != null) {
                        task.cancel(true);
                        mActiveDownloads.remove(item.filename);
                    }
                    pbDownload.setVisibility(View.GONE);
                    tvProgress.setVisibility(View.GONE);
                    btnAction.setText("Download & Install");
                    btnAction.setBackgroundResource(R.drawable.bg_ios_glass_button_primary);
                    Toast.makeText(this, "Download cancelled", Toast.LENGTH_SHORT).show();
                } else {
                    // Start download
                    if (item.url.isEmpty()) {
                        Toast.makeText(this, "No valid download URL available for this build", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    pbDownload.setVisibility(View.VISIBLE);
                    tvProgress.setVisibility(View.VISIBLE);
                    pbDownload.setProgress(0);
                    tvProgress.setText("Connecting...");
                    btnAction.setText("Cancel Download");
                    btnAction.setBackgroundResource(R.drawable.bg_ios_glass_button_cancel);

                    ItemDownloadTask downloadTask = new ItemDownloadTask(item, targetFile, btnAction, pbDownload, tvProgress);
                    mActiveDownloads.put(item.filename, downloadTask);
                    downloadTask.execute(item.url);
                }
            });

            llUpdatesContainer.addView(card);
        }
    }

    private void updateButtonState(UpdateItem item, File targetFile, Button btnAction, ProgressBar pbDownload, TextView tvProgress) {
        if (targetFile.exists() && targetFile.length() > 10240) {
            pbDownload.setVisibility(View.GONE);
            tvProgress.setVisibility(View.GONE);
            if (item.filename.endsWith(".apk")) {
                btnAction.setText("Install Update (.apk)");
            } else {
                btnAction.setText("Install via Recovery");
            }
            btnAction.setBackgroundResource(R.drawable.bg_ios_glass_button_primary);
        } else {
            btnAction.setText("Download & Install");
            btnAction.setBackgroundResource(R.drawable.bg_ios_glass_button_primary);
        }
    }

    private class ItemDownloadTask extends AsyncTask<String, Object, Boolean> {
        private final UpdateItem mItem;
        private final File mDestFile;
        private final Button mBtn;
        private final ProgressBar mPb;
        private final TextView mTv;
        private File mTempFile;
        private String mError = "";

        public ItemDownloadTask(UpdateItem item, File destFile, Button btn, ProgressBar pb, TextView tv) {
            mItem = item;
            mDestFile = destFile;
            mBtn = btn;
            mPb = pb;
            mTv = tv;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String downloadUrl = params[0];
            InputStream in = null;
            FileOutputStream out = null;
            HttpURLConnection conn = null;

            try {
                String resolvedUrl = downloadUrl;
                int redirects = 0;

                while (redirects < 10) {
                    if (isCancelled()) return false;
                    URL u = new URL(resolvedUrl);
                    conn = (HttpURLConnection) u.openConnection();
                    conn.setInstanceFollowRedirects(true);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(30000);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 15; Tablet) AppleWebKit/537.36");
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
                    mError = "HTTP error: " + (conn != null ? conn.getResponseCode() : -1);
                    return false;
                }

                long fileLength = conn.getContentLengthLong();
                if (fileLength <= 0 && mItem.size > 0) {
                    fileLength = mItem.size;
                }

                in = conn.getInputStream();
                mTempFile = new File(mDestFile.getAbsolutePath() + ".tmp");
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
                    if (now - lastUiUpdate > 300) {
                        int progress = (fileLength > 0) ? (int) ((total * 100) / fileLength) : 0;
                        double elapsedSec = Math.max((now - startTime) / 1000.0, 0.1);
                        double speedKb = (total / 1024.0) / elapsedSec;
                        publishProgress(progress, total, fileLength, speedKb);
                        lastUiUpdate = now;
                    }
                }

                out.flush();
                out.close();
                in.close();
                conn.disconnect();

                if (mTempFile.length() < 1024) {
                    mError = "Downloaded file is corrupted or incomplete";
                    if (mTempFile.exists()) mTempFile.delete();
                    return false;
                }

                if (mDestFile.exists()) {
                    mDestFile.delete();
                }
                return mTempFile.renameTo(mDestFile);

            } catch (Exception e) {
                mError = e.getMessage();
                Log.e(TAG, "Item download error: " + e.getMessage(), e);
                return false;
            } finally {
                try { if (in != null) in.close(); } catch (Exception ignored) {}
                try { if (out != null) out.close(); } catch (Exception ignored) {}
                if (conn != null) conn.disconnect();
            }
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            int progress = (Integer) values[0];
            long total = (Long) values[1];
            long length = (Long) values[2];
            double speed = (Double) values[3];

            mPb.setIndeterminate(false);
            mPb.setProgress(progress);

            long totalKb = total / 1024;
            long lengthKb = length / 1024;
            String text = String.format(Locale.US, "%d%% (%d KB / %d KB) - %.0f KB/s", progress, totalKb, lengthKb, speed);
            mTv.setText(text);
        }

        @Override
        protected void onCancelled() {
            mActiveDownloads.remove(mItem.filename);
            mPb.setVisibility(View.GONE);
            mTv.setVisibility(View.GONE);
            if (mTempFile != null && mTempFile.exists()) {
                mTempFile.delete();
            }
            mBtn.setText("Download & Install");
            mBtn.setBackgroundResource(R.drawable.bg_ios_glass_button_primary);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mActiveDownloads.remove(mItem.filename);
            mPb.setVisibility(View.GONE);
            mTv.setVisibility(View.GONE);

            if (success) {
                if (mItem.filename.endsWith(".apk")) {
                    mBtn.setText("Install Update (.apk)");
                    MainActivity.installApk(PreviousUpdatesActivity.this, mDestFile.getAbsolutePath());
                } else {
                    mBtn.setText("Install via Recovery");
                    MainActivity.confirmInstall(PreviousUpdatesActivity.this, mDestFile.getAbsolutePath());
                }
                mBtn.setBackgroundResource(R.drawable.bg_ios_glass_button_primary);
                Toast.makeText(PreviousUpdatesActivity.this, "Download complete: " + mDestFile.getName(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(PreviousUpdatesActivity.this, "Download failed: " + mError, Toast.LENGTH_LONG).show();
                mBtn.setText("Download & Install");
                mBtn.setBackgroundResource(R.drawable.bg_ios_glass_button_primary);
            }
        }
    }
}
