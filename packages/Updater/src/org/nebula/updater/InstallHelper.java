package org.nebula.updater;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;

public class InstallHelper {
    private static final String TAG = "NebulaUpdater";

    public static boolean rebootToRecovery(Context context, String zipPath) {
        Log.i(TAG, "Initiating recovery install for: " + zipPath);

        // 1. Write standard recovery commands for OrangeFox / TWRP
        writeRecoveryCommands(zipPath);

        // 2. Reboot via PowerManager (granted via android.permission.REBOOT in priv-app)
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                Log.i(TAG, "Rebooting to recovery via PowerManager...");
                pm.reboot("recovery");
                return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "PowerManager reboot failed: " + e.getMessage());
        }

        // 3. Fallback: System property trigger
        try {
            Class<?> sp = Class.forName("android.os.SystemProperties");
            Method set = sp.getMethod("set", String.class, String.class);
            set.invoke(null, "sys.powerctl", "reboot,recovery");
            return true;
        } catch (Exception e) {
            Log.w(TAG, "sys.powerctl reboot failed: " + e.getMessage());
        }

        return false;
    }

    public static void writeRecoveryCommands(String zipPath) {
        String[] commandPaths = {
            "/cache/recovery/command",
            "/data/cache/recovery/command",
            "/data/recovery/openrecoveryscript",
            "/cache/recovery/openrecoveryscript"
        };

        for (String p : commandPaths) {
            try {
                File f = new File(p);
                f.getParentFile().mkdirs();
                FileWriter fw = new FileWriter(f);
                if (p.endsWith("openrecoveryscript")) {
                    fw.write("install " + zipPath + "\nwipe cache\n");
                } else {
                    fw.write("--update_package=" + zipPath + "\n");
                }
                fw.flush();
                fw.close();
                f.setReadable(true, false);
                f.setWritable(true, false);
            } catch (Exception ignored) {}
        }
    }
}
