package com.bgmi.sdk;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import net_62v.external.MetaStorageManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Copies the host game OBB files into the virtual container
 * ({@code <external>/SdCard/Android/obb/<pkg>}) with real byte progress.
 */
public final class ObbSetup {

    private static final String TAG = "ObbSetup";
    private static final String PKG = BoxManager.PKG_BGMI;

    public interface Progress {
        void onProgress(int percent, String message);
    }

    public static final class Result {
        public final boolean ok;
        public final String message;

        Result(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }
    }

    private ObbSetup() {
    }

    /** Virtual container OBB directory, or {@code null} when unavailable. */
    public static File virtualDir() {
        try {
            return MetaStorageManager.getVirtualObbContainerPath(PKG);
        } catch (Throwable t) {
            Log.e(TAG, "virtualDir failed", t);
            return null;
        }
    }

    /** Host OBB directory (real device storage), or {@code null}. */
    public static File hostDir(Context context) {
        List<File> candidates = new ArrayList<>();
        try {
            File sdkHost = MetaStorageManager.getHostObbDir(PKG);
            if (sdkHost != null) candidates.add(sdkHost);
        } catch (Throwable ignored) {
        }

        try {
            File external = Environment.getExternalStorageDirectory();
            if (external != null) {
                candidates.add(new File(external, "Android/obb/" + PKG));
            }
        } catch (Throwable ignored) {
        }
        candidates.add(new File("/storage/emulated/0/Android/obb/" + PKG));

        // Secondary volumes (SD cards / OTG).
        try {
            File storage = new File("/storage");
            File[] volumes = storage.listFiles();
            if (volumes != null) {
                for (File volume : volumes) {
                    if (volume == null) continue;
                    String name = volume.getName();
                    if ("emulated".equals(name) || "self".equals(name)) continue;
                    candidates.add(new File(volume, "Android/obb/" + PKG));
                }
            }
        } catch (Throwable ignored) {
        }

        for (File candidate : candidates) {
            if (candidate == null) continue;
            File[] obbs = candidate.listFiles(ObbSetup::isObb);
            if (obbs != null && obbs.length > 0) {
                return candidate;
            }
        }
        return null;
    }

    /** True when the virtual container already holds up-to-date OBB files. */
    public static boolean isReady(Context context) {
        File dest = virtualDir();
        if (dest == null || !dest.isDirectory()) return false;

        File[] virtual = dest.listFiles(ObbSetup::isObb);
        if (virtual == null || virtual.length == 0) return false;

        File host = hostDir(context);
        if (host != null) {
            File[] source = host.listFiles(ObbSetup::isObb);
            if (source != null) {
                for (File src : source) {
                    File copy = new File(dest, src.getName());
                    if (!copy.isFile() || copy.length() != src.length()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static Result copy(Context context, Progress progress) {
        File host = hostDir(context);
        if (host == null) {
            return new Result(false,
                    "BGMI OBB not found. Install BGMI and open it once, then try again.");
        }

        File[] sources = host.listFiles(ObbSetup::isObb);
        if (sources == null || sources.length == 0) {
            return new Result(false,
                    "BGMI OBB not found. Install BGMI and open it once, then try again.");
        }

        File dest = virtualDir();
        if (dest == null) {
            return new Result(false, "Could not open the virtual storage container.");
        }
        if (!dest.isDirectory() && !dest.mkdirs()) {
            return new Result(false,
                    "Cannot write to storage. Please allow all files access and try again.");
        }

        long total = 0L;
        for (File src : sources) total += Math.max(0L, src.length());
        if (total <= 0L) {
            return new Result(false, "The OBB files are empty or unreadable.");
        }

        if (progress != null) progress.onProgress(0, "Preparing OBB copy…");

        long done = 0L;
        int lastPercent = -1;
        byte[] buffer = new byte[128 * 1024];

        for (File src : sources) {
            File target = new File(dest, src.getName());
            long length = src.length();

            if (target.isFile() && target.length() == length) {
                done += length;
                continue;
            }

            File tmp = new File(dest, src.getName() + ".part");
            FileInputStream in = null;
            FileOutputStream out = null;
            try {
                in = new FileInputStream(src);
                out = new FileOutputStream(tmp);
                long copied = 0L;
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    copied += read;
                    done += read;
                    if (progress != null) {
                        int percent = (int) Math.min(99L, (done * 100L) / total);
                        if (percent != lastPercent) {
                            lastPercent = percent;
                            progress.onProgress(percent, String.format(Locale.US,
                                    "Copying OBB… %d%%", percent));
                        }
                    }
                }
                out.flush();
                out.close();
                out = null;
                in.close();
                in = null;

                if (copied != length) {
                    //noinspection ResultOfMethodCallIgnored
                    tmp.delete();
                    return new Result(false, "OBB copy was interrupted. Please retry.");
                }

                if (target.exists() && !target.delete()) {
                    //noinspection ResultOfMethodCallIgnored
                    tmp.delete();
                    return new Result(false, "Could not replace the existing OBB file.");
                }
                if (!tmp.renameTo(target)) {
                    //noinspection ResultOfMethodCallIgnored
                    tmp.delete();
                    return new Result(false, "Could not save the OBB file.");
                }
            } catch (Exception e) {
                Log.e(TAG, "copy failed for " + src, e);
                if (tmp.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    tmp.delete();
                }
                String reason = e.getMessage();
                return new Result(false, reason == null ? "OBB copy failed." : reason);
            } finally {
                try {
                    if (in != null) in.close();
                } catch (Exception ignored) {
                }
                try {
                    if (out != null) out.close();
                } catch (Exception ignored) {
                }
            }
        }

        // Best effort: bring across the host's external data directory too.
        try {
            if (progress != null) progress.onProgress(99, "Syncing game data…");
            MetaStorageManager.syncDataToVirtualIfNeeded(PKG);
        } catch (Throwable t) {
            Log.w(TAG, "data sync skipped", t);
        }

        if (progress != null) progress.onProgress(100, "OBB ready");

        if (!isReady(context)) {
            return new Result(false, "OBB verification failed. Please try again.");
        }
        return new Result(true, "OBB ready");
    }

    private static boolean isObb(File dir, String name) {
        return name != null && name.toLowerCase(Locale.US).endsWith(".obb");
    }
}
