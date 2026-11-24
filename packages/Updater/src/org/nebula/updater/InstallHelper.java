package org.nebula.updater;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;
import java.io.DataOutputStream;

public class InstallHelper {
    private static final String TAG = "NebulaUpdater";

    public static boolean rebootToRecovery(Context context, String zipPath) {
        // Try method 1: Standard su shell stream
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("mkdir -p /cache/recovery\n");
            os.writeBytes("mkdir -p /data/cache/recovery\n");
            os.writeBytes("echo '--update_package=" + zipPath + "' > /cache/recovery/command\n");
            os.writeBytes("echo 'install " + zipPath + "' > /cache/recovery/openrecoveryscript\n");
            os.writeBytes("echo 'wipe cache' >> /cache/recovery/openrecoveryscript\n");
            os.writeBytes("/system/bin/reboot recovery\n");
            os.writeBytes("setprop sys.powerctl reboot,recovery\n");
            os.writeBytes("exit\n");
            os.flush();
            int code = p.waitFor();
            if (code == 0) {
                return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "su reboot failed: " + e.getMessage());
        }

        // Try method 2: PowerManager (if granted system privileges)
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                pm.reboot("recovery");
                return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "PowerManager reboot failed: " + e.getMessage());
        }

        // Try method 3: direct svc power reboot recovery
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "svc power reboot recovery"});
            p.waitFor();
            return true;
        } catch (Exception ignored) {}

        return false;
    }
}
