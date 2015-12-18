package ua.motorny.flashdrivecheck;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements VerifyTask.VerifyCallbacks {

    public static String dirPath = "/Android/data/ua.motorny.flashdrivecheck/files/CHK";
    private static final String TAG = "FlashTest";

    private Spinner devices_spinner;
    private TextView temp_textView;
    private EditText size_editText;
    private Button stop_button, start_button, verify_button, erase_button;

    private WriteTask writeTask;
    private VerifyTask verifyTask;
    protected StorageInfo currentStorage;
    protected long checksize;

    private List<StorageInfo> storageList;

    private AdapterView.OnItemSelectedListener onItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            currentStorage = storageList.get(position);
            size_editText.setText(String.valueOf(currentStorage.totalSize));
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        temp_textView = (TextView) findViewById(R.id.temp_textView);
        devices_spinner = (Spinner) findViewById(R.id.devices_spinner);
        size_editText = (EditText) findViewById(R.id.size_editText);
        stop_button = (Button) findViewById(R.id.stop_button);
        start_button = (Button) findViewById(R.id.start_button);
        verify_button = (Button) findViewById(R.id.verify_button);
        erase_button = (Button) findViewById(R.id.erase_button);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getExternalFilesDirs("CHK");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        addDevicesToList();
        devices_spinner.setOnItemSelectedListener(onItemSelectedListener);
        if (currentStorage != null) {
            devices_spinner.setSelection(currentStorage.number);
        } else {
            if (storageList.size() > 1) {
                devices_spinner.setSelection(1);
            } else {
                devices_spinner.setSelection(0);
            }
        }
    }

    public void addDevicesToList() {
        List<String> stringListOfDevices = new ArrayList<>();

        storageList = StorageUtils.getStorageList();
        for (int i = 0; i < storageList.size(); i++) {
            stringListOfDevices.add(getResources().getString(R.string.free, storageList.get(i).number + 1, storageList.get(i).getDisplayName(), storageList.get(i).getFreeFormattedSize()));
        }

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, stringListOfDevices);
        dataAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        devices_spinner.setAdapter(dataAdapter);
        if (currentStorage != null) {
            devices_spinner.setSelection(currentStorage.number);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_about:
                Intent intent_about = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(intent_about);

            break;
            case R.id.action_instruction:
                Intent intent_instructions = new Intent(MainActivity.this, InstructionActivity.class);
                startActivity(intent_instructions);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void verifyProgressUpdate(long flashsize, int errors) {
            temp_textView.setText(getResources().getString(R.string.working) + " "
                    + flashsize + getResources().getString(R.string.mb) + "/" + Long.toString(checksize) + getResources().getString(R.string.mb) + "\n" + getResources().getString(R.string.errors) + " "
                    + errors + getResources().getString(R.string.mb) + "/" + Long.toString(checksize) + getResources().getString(R.string.mb));
    }

    @Override
    public void verifyCancelled(long flashsize, int errors) {
        temp_textView.setText(getResources().getString(R.string.over) + "\n" + getResources().getString(R.string.working) + " " + flashsize + " " + getResources().getString(R.string.mb) + "\n" + getResources().getString(R.string.errors) + " " + errors + " " + getResources().getString(R.string.mb));

        if (errors!=0) {
            temp_textView.setText(temp_textView.getText() + "\n" + getResources().getString(R.string.caution_bad));
        } else {
            temp_textView.setText(temp_textView.getText() + "\n" + getResources().getString(R.string.caution_good));
        }
        size_editText.setEnabled(true);
    }

    @Override
    public void verifyOnPostExecute(long flashsize, int errors) {
        long totaltested = flashsize + errors;
        temp_textView.setText(getResources().getString(R.string.over) + "\n" + getResources().getString(R.string.tested) + " " + totaltested + getResources().getString(R.string.mb) + "\n" + getResources().getString(R.string.working) + " " + flashsize + " " + getResources().getString(R.string.mb) + "\n" + getResources().getString(R.string.errors) + " " + errors + " " + getResources().getString(R.string.mb));
        if (errors!=0) {
            temp_textView.setText(temp_textView.getText() + "\n" + getResources().getString(R.string.caution_bad));
        } else {
            temp_textView.setText(temp_textView.getText() + "\n" + getResources().getString(R.string.caution_good));
        }
        size_editText.setEnabled(true);
    }

    private class WriteTask extends AsyncTask<Void, Long, Void> {
        protected long writed;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            temp_textView.setText(getResources().getString(R.string.start_write));
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                long cnt_wrt = 0;

                int mb = 1048576; // 1 mb
                int gb = 1073741824; // 1gb

                for (long fileCount = 0; currentStorage.getFreeSize() > mb; fileCount++) {
                    File directories = new File(currentStorage.path + dirPath);
                    if (!directories.exists()) {
                        if (directories.mkdirs()) {
                            Log.d(TAG, "Directory " + directories.toString() + " was created");
                        } else {
                            Log.d(TAG, "Can't create: " + directories.toString());
                        }
                    }
                    File fhandle = new File(currentStorage.path + dirPath + "/check" + fileCount + ".dat");

                    if (!fhandle.exists()) {
                        try {
                            RandomAccessFile out = new RandomAccessFile(currentStorage.path + dirPath + "/check" + fileCount + ".dat", "rw");

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
            writed=values[0];
            if (!this.isCancelled()) {
                temp_textView.setText(getResources().getString(R.string.wrt) + " " + writed + getResources().getString(R.string.mb) + '/' + Long.toString(checksize) + getResources().getString(R.string.mb));}
        }

        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);
            writed--;
            temp_textView.setText(getResources().getString(R.string.wrt) + " " + writed + getResources().getString(R.string.mb) + "\n" + getResources().getString(R.string.verify));
            size_editText.setEnabled(true);
            addDevicesToList();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();

            temp_textView.setText(getResources().getString(R.string.wrt) + " " + writed + getResources().getString(R.string.mb) + "\n" + getResources().getString(R.string.verify));
            size_editText.setEnabled(true);
            addDevicesToList();
        }
    }

    private class DeleteTask extends AsyncTask<Void, String, Void> {
        public boolean delete(File file) {
            if (!file.exists()) {
                return false;
            }
            if (file.isDirectory()) {
                for(File f : file.listFiles())
                    delete(f);
                return file.delete();
            } else {
                publishProgress(file.getName());
                return file.delete();
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            File file = new File(currentStorage.path + dirPath);
            delete(file);
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            temp_textView.setText(getResources().getString(R.string.deleting) + values[0] + "\n" + getResources().getString(R.string.wait));
        }

        @Override
        protected void onPostExecute(Void result) {

            temp_textView.setText(getResources().getString(R.string.deleted));
            start_button.setEnabled(true);
            stop_button.setEnabled(true);
            verify_button.setEnabled(true);
            addDevicesToList();
        }
    }

    public void action (View view) {
        checksize = storageList.get(devices_spinner.getSelectedItemPosition()).totalSize / 1024 / 1024;

        if (currentStorage.path != null) {
            switch (view.getId()) {
                case R.id.start_button:
                    if (verifyTask != null && verifyTask.getStatus() == AsyncTask.Status.RUNNING) {
                        verifyTask.cancel(true);
                    }
                        writeTask = new WriteTask();
                        writeTask.execute();
                        size_editText.setEnabled(false);
                    break;
                case R.id.verify_button:
                    if (writeTask != null && writeTask.getStatus() == AsyncTask.Status.RUNNING) {
                        writeTask.cancel(true);
                    }
                    verifyTask = new VerifyTask(currentStorage);
                    verifyTask.setCallbacks(this);
                    verifyTask.execute();
                    size_editText.setEnabled(false);
                    break;
                case R.id.stop_button:
                    if (verifyTask != null && verifyTask.getStatus() == AsyncTask.Status.RUNNING) {
                        verifyTask.cancel(true);
                    }
                    if (writeTask != null && writeTask.getStatus() == AsyncTask.Status.RUNNING) {
                        writeTask.cancel(true);
                    }
                    size_editText.setEnabled(true);
                    break;
                case R.id.erase_button:
                    if (verifyTask != null && verifyTask.getStatus() == AsyncTask.Status.RUNNING) {
                        verifyTask.cancel(true);
                    }
                    if (writeTask != null && writeTask.getStatus() == AsyncTask.Status.RUNNING) {
                        writeTask.cancel(true);
                    }
                    start_button.setEnabled(false);
                    stop_button.setEnabled(false);
                    verify_button.setEnabled(false);
                    DeleteTask deleteTask = new DeleteTask();
                    deleteTask.execute();

                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (writeTask.getStatus() == AsyncTask.Status.RUNNING || verifyTask.getStatus() == AsyncTask.Status.RUNNING) {
            Toast.makeText(this, "Для выхода из приложения, пожалуйста, остановите тестирование!", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }
}
