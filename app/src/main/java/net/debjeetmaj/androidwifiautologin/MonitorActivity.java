package net.debjeetmaj.androidwifiautologin;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by debjeet on 10-01-2017.
 */

public class MonitorActivity extends AppCompatActivity {
    TextView txtLogView;
    Button btnGetLog;
    Timer timer;
    TimerTask timerTask;
    final int timerTaskPeriod = 10000;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.monitor_activity);
        txtLogView = (TextView) findViewById(R.id.txtLogView);
//        btnGetLog = (Button) findViewById(R.id.btnGetLog);
//        btnGetLog.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                new LogRetrieverAsyncTask().execute("");
//            }
//        });
        timer = null;
        timerTask = null;
    }

    @Override
    protected void onStart() {
        super.onStart();
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                new LogRetrieverAsyncTask().execute("");
            }
        };
        timer.scheduleAtFixedRate(timerTask,0,timerTaskPeriod);
    }

    @Override
    protected void onStop() {
        if(timer!=null)
        {
            timer.cancel();
            timer = null;
        }
        super.onStop();
    }

    private class LogRetrieverAsyncTask extends AsyncTask<String,Void,String>{
        @Override
        protected void onPostExecute(String s) {
            if(s!=null && !s.isEmpty()) {
                txtLogView.setText(txtLogView.getText() + "\n" + s.toString());
                txtLogView.setMovementMethod(new ScrollingMovementMethod());
//            final int scrollAmount = txtLogView.getLayout().getLineTop(txtLogView.getLineCount());
//            // if there is no need to scroll, scrollAmount will be <=0
//            if (scrollAmount > 0)
//                txtLogView.scrollTo(0, scrollAmount);
//            else
//                txtLogView.scrollTo(0, 0);
            }
        }

        @Override
        protected String doInBackground(String... params) {
            String readLog="";
            try {
                int pid = android.os.Process.myPid();
                Process process = Runtime.getRuntime().exec("logcat -d -v time net.debjeetmaj.androidwifiautologin AutoLoginService:I AutoAuthObj:D WifiSenseReceiver:D *:S");
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));

                StringBuilder log=new StringBuilder();
                String line = "";
                while ((line = bufferedReader.readLine()) != null) {
                    if(!line.isEmpty())
                        log.append(line+"\n");
                }
                readLog = log.toString();
//                process = Runtime.getRuntime().exec("logcat -c -v time net.debjeetmaj.androidwifiautologin:I AutoLoginService:I *:S");
            }
            catch (IOException e) {}
            Log.v("AutoLoginService",readLog.length()+"");
            return readLog;
        }
    }
}
