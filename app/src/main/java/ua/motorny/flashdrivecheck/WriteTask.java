package ua.motorny.flashdrivecheck;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.TimeUnit;

public class WriteTask extends AsyncTask<Void, Long, Void> {

    private static final String TAG = "FlashTest";
    private long writed;
    private long checksize;

    private StorageInfo currentStorage;
    private WriteCallbacks writeCallbacks;

    public WriteTask (StorageInfo currentStorage, long checksize) {
        this.currentStorage = currentStorage;
        this.checksize = checksize;
    }

    public void setCallbacks (WriteCallbacks writeCallbacks) {
        this.writeCallbacks = writeCallbacks;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        writeCallbacks.writeOnPreExecute();
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            long cnt_wrt = 0;

            int mb = 1048576; // 1 mb
            int gb = 1073741824; // 1gb

            for (long fileCount = 0; currentStorage.getFreeSize() > mb; fileCount++) {
                File directories = new File(currentStorage.file + MainActivity.dirPath);
                if (!directories.exists()) {
                    if (directories.mkdirs()) {
                        Log.d(TAG, "Directory " + directories.toString() + " was created");
                    } else {
                        Log.d(TAG, "Can't create: " + directories.toString());
                    }
                }
                File fhandle = new File(currentStorage.file + MainActivity.dirPath + "/check" + fileCount + ".dat");

                if (!fhandle.exists()) {
                    try {
                        RandomAccessFile out = new RandomAccessFile(currentStorage.file + MainActivity.dirPath + "/check" + fileCount + ".dat", "rw");

                        for (long determinedbytes = 0; determinedbytes < 1024; determinedbytes++) {
                            out.seek(mb * determinedbytes);
                            out.writeLong(cnt_wrt);
                            cnt_wrt++;
                            publishProgress(cnt_wrt);
                            if ((fileCount + 1) * determinedbytes + 1 == checksize || (fileCount + 1) * determinedbytes + 1 == currentStorage.getFreeSize()) {
                                this.cancel(true);
                            }
                            if (isCancelled()) return null;
                        }
                        out.seek(gb-1);
                        out.writeByte(0);
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        this.cancel(true);
                        TimeUnit.SECONDS.sleep(1);
                    }
                } else {
                    cnt_wrt += fhandle.length()/1024/1024;
                }
            }
            // разъединяемся
            TimeUnit.MILLISECONDS.sleep(1);
            if (isCancelled()) return null;
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Long... values) {
        super.onProgressUpdate(values);
        writed = values[0];
        if (!this.isCancelled()) {
            writeCallbacks.writeProgressUpdate(writed);}
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        writed--;

        writeCallbacks.writeOnPostExecute(writed);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();

        writeCallbacks.writeCancelled(writed);
    }

    public interface WriteCallbacks {
        void writeOnPreExecute();
        void writeProgressUpdate(long writed);
        void writeCancelled(long writed);
        void writeOnPostExecute(long writed);
    }
}