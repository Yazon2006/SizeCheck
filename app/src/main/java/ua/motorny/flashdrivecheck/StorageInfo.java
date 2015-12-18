package ua.motorny.flashdrivecheck;

import java.io.File;
import java.text.DecimalFormat;

public class StorageInfo {

    private static final long TB = 1099511627776l;
    private static final long GB = 1073741824l;
    private static final long MB = 1048576l;
    private static final long KB = 1024l;

    public final String path;
    public final boolean readonly;
    public final boolean removable;
    public final int number;
    public final long totalSize;

    StorageInfo(String path, boolean readonly, boolean removable, int number, long totalSize) {
        this.path = path;
        this.readonly = readonly;
        this.removable = removable;
        this.number = number;
        this.totalSize = totalSize;
    }

    public String getDisplayName() {
        StringBuilder res = new StringBuilder();
        if (!removable) {
            res.append("Internal SD card ");
            res.append(getFormattedSize());
        } else {
            if (path.toLowerCase().contains("usb".toLowerCase())) {
                res.append("USB drive ");
                res.append(getFormattedSize());
            } else {
                res.append("SD card ");
                res.append(getFormattedSize());
            }
        }
        if (readonly) {
            res.append(" (Read only)");
        }
        return res.toString();
    }

    public String getFormattedSize() {
        return getFormattedSize(totalSize);
    }

    public long getFreeSize() {
        long freeSize;
        try {
            freeSize = new File(path).getFreeSpace();
        } catch (Exception e) {
            freeSize = 0l;
        }
        return freeSize;
    }

    public String getFreeFormattedSize() {
        return getFormattedSize(getFreeSize());
    }

    private String getFormattedSize(long currentSizeLong) {
        String suffix;

        DecimalFormat df;
        Double size = (double) currentSizeLong;
        if (currentSizeLong > TB) {
            suffix = "TB";
            size /= TB;
            df = new DecimalFormat("#0.00");
        } else {
            if (currentSizeLong > GB) {
                suffix = "GB";
                size /= GB;
                df = new DecimalFormat("#0.0");
            } else {
                if (currentSizeLong > MB) {
                    suffix = "MB";
                    size /= MB;
                    df = new DecimalFormat("#0");
                } else {
                    suffix = "KB";
                    size /= KB;
                    df = new DecimalFormat("#0");
                }
            }
        }
        return df.format(size) + suffix;
    }
}