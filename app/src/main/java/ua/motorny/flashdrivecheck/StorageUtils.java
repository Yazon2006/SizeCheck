package ua.motorny.flashdrivecheck;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilePermission;
import java.io.FileReader;
import java.io.IOException;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

public class StorageUtils {

    private static final String TAG = "StorageUtils";

    public static List<StorageInfo> getStorageList() {

        List<StorageInfo> list = new ArrayList<>();
        File defPath = Environment.getExternalStorageDirectory();
        boolean defPathRemovable = Environment.isExternalStorageRemovable();
        String defPathState = Environment.getExternalStorageState();
        boolean defPathAvailable = defPathState.equals(Environment.MEDIA_MOUNTED)
                || defPathState.equals(Environment.MEDIA_MOUNTED_READ_ONLY);
        boolean defPathReadonly = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY);

        HashSet<File> paths = new HashSet<>();
        int curRemovableNumber = 0;

        if (defPathAvailable) {
            paths.add(defPath);
            list.add(0, new StorageInfo(defPath, defPathReadonly, defPathRemovable, curRemovableNumber++ , getSize(defPath.getPath())));
            Log.d(TAG, ">>>>> added new storage: " + defPath);
        }

        BufferedReader bufReader = null;
        try {
            bufReader = new BufferedReader(new FileReader("/proc/mounts"));
            String line;
            Log.d(TAG, "/proc/mounts");
            while ((line = bufReader.readLine()) != null) {
                Log.d(TAG, line);
                if (line.contains("vfat") || line.contains("/mnt")) {
                    StringTokenizer tokens = new StringTokenizer(line, " ");
                    String unused = tokens.nextToken(); //device
                    String mountPoint = tokens.nextToken(); //mount point
                    if (paths.contains(new File(mountPoint))) {
                        continue;
                    }
                    unused = tokens.nextToken(); //file system
                    List<String> flags = Arrays.asList(tokens.nextToken().split(",")); //flags
                    boolean readonly = flags.contains("ro");

                    if (line.contains("/dev/block/vold")) {
                        if (!line.contains("/mnt/secure")
                            && !line.contains("/mnt/asec")
                            && !line.contains("/mnt/obb")
                            && !line.contains("/dev/mapper")
                            && !line.contains("tmpfs")) {
                            if (getSize(mountPoint) > 0l) {
                                File tempFile = new File(mountPoint);
                                paths.add(tempFile);
                                list.add(new StorageInfo(tempFile, readonly, true, curRemovableNumber++, getSize(mountPoint)));
                                Log.d(TAG, ">>>>> added new storage: " + mountPoint);
                            } else {
                                Log.d(TAG, ">>>>> " + mountPoint + " size is 0");
                                String nextLine = bufReader.readLine();
                                Log.d(TAG, nextLine);
                                StringTokenizer tmpTokens = new StringTokenizer(nextLine, " ");
                                unused = tmpTokens.nextToken();
                                String emulatedMountPoint = tmpTokens.nextToken();
                                if (getSize(emulatedMountPoint) > 0l) {
                                    mountPoint = emulatedMountPoint;
                                    File tempFile = new File(mountPoint);
                                    list.add(new StorageInfo(tempFile, readonly, true, curRemovableNumber++, getSize(mountPoint)));
                                    Log.d(TAG, ">>>>> added new storage: " + mountPoint);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bufReader != null) {
                try {
                    bufReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return list;
    }

    private static long getSize(String path) {
        long size = 0;
        if (DirectoryExisting(path)) {
            size = new File(path).getTotalSpace();
        }
        return size;
    }

    private static boolean DirectoryExisting (String path) {
        File f = new File(path);
        return (f.isDirectory());
    }

    private static boolean readWritePermissionGranted(String path) {
        String actions = "read,write";

        try {
            AccessController.checkPermission(new FilePermission(path, actions));
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

}
