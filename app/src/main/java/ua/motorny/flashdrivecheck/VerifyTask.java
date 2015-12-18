package ua.motorny.flashdrivecheck;

import android.os.AsyncTask;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;
import java.util.concurrent.TimeUnit;

class VerifyTask extends AsyncTask<Void, Integer, Void> {
    private long flashsize;
    private int errors;
    private StorageInfo currentStorage;
    private VerifyCallbacks verifyCallbacks;

    public VerifyTask (StorageInfo currentStorage) {
        this.currentStorage = currentStorage;
    }

    public void setCallbacks (VerifyCallbacks verifyCallbacks) {
        this.verifyCallbacks = verifyCallbacks;
    }

    @Override
    protected Void doInBackground(Void... params) {
        errors = 0;
        int cnt_rd = 0, cnt_err = 0;
        long cnt_chk = 0;

        try {
            int mb = 1048576; // 1 mb
            for (long filecount = 0; filecount < 1024; filecount++) {
                File fhandle = new File(currentStorage.file + MainActivity.dirPath + "/check" + filecount + ".dat");
                if (fhandle.exists()) {
                    try {
                        RandomAccessFile out = new RandomAccessFile(currentStorage.file + MainActivity.dirPath + "/check" + filecount + ".dat", "rw");
                        for (long determinedbytes=0; determinedbytes < fhandle.length()/1024/1024; determinedbytes++) {
                            out.seek(mb * determinedbytes);
                            if (out.readLong() == cnt_chk) {
                                boolean error = false;
                                for (int i = 0; i < 10; i++) {
                                    Random randNumber = new Random();
                                    int iNumber = randNumber.nextInt(mb - 20) + 20;
                                    out.seek(determinedbytes+iNumber);
                                    if (out.readByte() != 0) error = true;
                                }
                                if (!error) cnt_rd++;
                            } else {
                                cnt_err++;
                            }
                            cnt_chk++;
                            publishProgress(cnt_rd,cnt_err);
                            if (isCancelled()) return null;
                        }
                    } catch (IOException e) {
                        errors = cnt_err;
                        this.cancel(true);
                        TimeUnit.SECONDS.sleep(1);
                    }
                } else {
                    errors=cnt_err;
                    this.cancel(true);
                    TimeUnit.SECONDS.sleep(1);
                    break;
                }
            }
            // разъединяемся
            TimeUnit.MILLISECONDS.sleep(1);
            if (isCancelled()) return null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        flashsize = values[0];
        errors = values[1];
        super.onProgressUpdate(values);
        if (!this.isCancelled()) {
            verifyCallbacks.verifyProgressUpdate(flashsize, errors);

        }
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);

        verifyCallbacks.verifyOnPostExecute(flashsize, errors);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        verifyCallbacks.verifyCancelled(flashsize, errors);
    }

    public interface VerifyCallbacks {
        void verifyProgressUpdate(long flashsize, int errors);
        void verifyCancelled(long flashsize, int errors);
        void verifyOnPostExecute(long flashsize, int errors);
    }
}
