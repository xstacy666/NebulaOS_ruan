package org.nebula.updater;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;

public class InstallHelper {
    private static final String TAG = "NebulaUpdater";

    public static boolean directAutoInstallAndReboot(Context context, String zipPath) {
        Log.i(TAG, "Initiating direct auto-install & reboot for: " + zipPath);
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());

            os.writeBytes("TMP=/data/local/tmp/ota_install\n");
            os.writeBytes("rm -rf \"$TMP\" && mkdir -p \"$TMP\"\n");
            os.writeBytes("/data/user/0/ai.antigem/files/usr/bin/unzip -o \"" + zipPath + "\" -d \"$TMP\" 2>/dev/null || unzip -o \"" + zipPath + "\" -d \"$TMP\"\n");
            os.writeBytes("if [ -f \"$TMP/installer.sh\" ]; then sh \"$TMP/installer.sh\"; fi\n");
            os.writeBytes("rm -rf \"$TMP\"\n");
            os.writeBytes("sync\n");
            os.writeBytes("sleep 2 && /system/bin/reboot\n");
            os.writeBytes("exit\n");
            os.flush();
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "directAutoInstallAndReboot failed: " + t.getMessage(), t);
            return false;
        }
    }

    public static boolean rebootToRecovery(Context context, String zipPath) {
        Log.i(TAG, "Initiating recovery install for: " + zipPath);

        // 1. Write standard recovery commands
        writeRecoveryCommands(zipPath);

        // 2. Try root execution first for instant guaranteed reboot
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("mkdir -p /cache/recovery\n");
            os.writeBytes("echo '--update_package=" + zipPath + "' > /cache/recovery/command\n");
            os.writeBytes("chmod 644 /cache/recovery/command\n");
            os.writeBytes("reboot recovery\n");
            os.flush();
            os.close();
            p.waitFor();
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "su reboot failed: " + t.getMessage());
        }

        // 3. PowerManager reboot (privileged permission)
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                Log.i(TAG, "Rebooting to recovery via PowerManager...");
                pm.reboot("recovery");
                return true;
            }
        } catch (Throwable t) {
            Log.w(TAG, "PowerManager reboot failed: " + t.getMessage());
        }

        // 4. System property sys.powerctl
        try {
            Class<?> sp = Class.forName("android.os.SystemProperties");
            Method set = sp.getMethod("set", String.class, String.class);
            set.invoke(null, "sys.powerctl", "reboot,recovery");
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "sys.powerctl reboot failed: " + t.getMessage());
        }

        // 5. Native binary
        try {
            Runtime.getRuntime().exec(new String[]{"/system/bin/reboot", "recovery"});
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "Runtime exec reboot failed: " + t.getMessage());
        }

        return false;
    }

    public static boolean silentInstallApk(String apkPath) {
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("pm install -r \"" + apkPath + "\"\n");
            os.writeBytes("exit\n");
            os.flush();
            int res = p.waitFor();
            return (res == 0);
        } catch (Throwable t) {
            Log.w(TAG, "silentInstallApk failed: " + t.getMessage());
            return false;
        }
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
                if (f.getParentFile() != null) f.getParentFile().mkdirs();
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
