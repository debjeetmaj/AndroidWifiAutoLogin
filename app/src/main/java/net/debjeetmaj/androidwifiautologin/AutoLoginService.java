package net.debjeetmaj.androidwifiautologin;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import net.debjeetmaj.androidwifiautologin.auth.AutoAuth;
import net.debjeetmaj.androidwifiautologin.auth.BasicAutoAuth;
import net.debjeetmaj.androidwifiautologin.auth.FortigateAutoAuth;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

public class AutoLoginService extends IntentService {
    public final static String LOG_TAG = "AutoLoginService";
    public final static int CONNECTION_CHECK_ATTEMPTS = 10;
    public final static int RETRY_TIMEOUT = 10000; // 10 secs

    private static WifiConfig wifiConfig = null;
    private static AutoAuth autoAuthObj = null;
//    private static LoginState state = LoginState.STOPPED;

    public AutoLoginService() {
        super("Auto Login Service");
        Log.w(LOG_TAG, "Service created");
//        AutoLoginService.setState(LoginState.STOPPED);
    }

    public static LoginState getState(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(context.getResources().getString(R.string.autologinservice),Context.MODE_PRIVATE);
        int state = sharedPref.getInt(context.getResources().getString(R.string.autologinservice),LoginState.STOPPED.ordinal());
        return LoginState.values()[state];
    }

    public static void setState(Context context, LoginState state) {
        SharedPreferences sharedPref = context.getSharedPreferences(context.getResources().getString(R.string.autologinservice),Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(context.getResources().getString(R.string.autologinservice),state.ordinal());
        editor.commit();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(LOG_TAG, "Service Started");
        Log.i(LOG_TAG, "current state: " + AutoLoginService.getState(getBaseContext()).toString());

        switch (AutoLoginService.getState(getBaseContext())) {
            case START:
                startStateHandler();
                break;
            case LOGGED_IN:
                loggedInStateHandler();
                break;
            case STOPPED:
                stoppedStateHandler();
                break;
        }
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "Service Stopped");
        super.onDestroy();
    }

    private String[] getStoredSSIDs() {
        String[] ssids = getFilesDir().list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".json");
            }
        });
        for (int i = 0; i < ssids.length; i++) {
            ssids[i] = ssids[i].substring(0, ssids[i].indexOf('.'));
        }
        return ssids;
    }

    //Create autoAuthObj based on authUrl
    private AutoAuth createAuthObj(String authUrl) throws Exception {
        //TODO : generalise the code to make it rule based
        if (authUrl.contains("/fgtauth?")) {
            //Fortigate firewall mechanism
            return new FortigateAutoAuth(authUrl, wifiConfig.getUsername(), wifiConfig.getPassword());
        } else {
            return new BasicAutoAuth(authUrl, wifiConfig.getUsername(), wifiConfig.getPassword());
        }
    }

    /*
    * checks if internet connection is active,
    * If auth required sets appropriate AutoAuth object and returns true
    * else returns false
    * */
    protected boolean checkAuthRequired() {
        HttpURLConnection httpConnection = null;
        for (int i = 0; i < CONNECTION_CHECK_ATTEMPTS; ++i) {
            try {
                // using bing.com's IP as it doesn't use https
                // TODO: switch to something else as it may change soon(TM) (looking at samik)
                httpConnection = (HttpURLConnection) (new URL("http://13.107.21.200")).openConnection();
                httpConnection.setRequestMethod("GET");
                httpConnection.setRequestProperty("Content-length", "0");
                httpConnection.setUseCaches(false);
                httpConnection.setConnectTimeout(AutoAuth.CONNECTION_TIMEOUT);
                httpConnection.setReadTimeout(AutoAuth.CONNECTION_TIMEOUT);

                httpConnection.connect();
                int responseCode = httpConnection.getResponseCode();
                Log.i(LOG_TAG, "checkAuth: connection response: " + responseCode);
                // see other, Http status 303, proxy redirect  : 307
                if (responseCode != HttpURLConnection.HTTP_SEE_OTHER && responseCode != 307) {
                    Log.d(LOG_TAG, "checkAuth: Internet is on");
                    return false;
                }

                String authUrl = httpConnection.getHeaderField("Location");
                Log.i(LOG_TAG, "checkAuth: Auth URL: " + authUrl);
                autoAuthObj = createAuthObj(authUrl);
                return true;

            } catch (IOException e) { // connection failed; will retry
                Log.e(LOG_TAG, "IOException");
                e.printStackTrace();
            } catch (Exception ex) { // something else happened; lets get out
                ex.printStackTrace();
                return false;
            } finally {
                if (httpConnection != null)
                    httpConnection.disconnect();
            }
        }
        return false;
    }

    private boolean login() {
        if (checkAuthRequired()) {
            assert autoAuthObj != null;
            return autoAuthObj.authenticate();
        } else {
            Log.i(LOG_TAG, "Authentication not required.");
            return false;
        }
    }

    /* schedule a job for later */
    void scheduleTimer() {
//        Timer timer = new Timer();
//        timer.schedule(new TimerTask() {
//            public void run() {
//                Intent intent = new Intent(getApplicationContext(), AutoLoginService.class);
//                getApplicationContext().startService(intent);
//            }
//        }, getState(getBaseContext()) == LoginState.STOPPED ? 10 :
//                // getState() == LoginState.LOGGED_IN
//                autoAuthObj != null ? autoAuthObj.sleepTimeout() :
//                // getState() == LoginState.START
//                        RETRY_TIMEOUT);
        int timeout = (getState(getBaseContext()) == LoginState.STOPPED ? 10 :
                // getState() == LoginState.LOGGED_IN
                autoAuthObj != null ? autoAuthObj.sleepTimeout() :
                // getState() == LoginState.START
                        RETRY_TIMEOUT);
        Log.i(LOG_TAG,"Scheduling an alarm for "+timeout+" secs.");
        Intent alarmIntent = new Intent(getBaseContext(), AlarmReciever.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(), 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager)getBaseContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis()+timeout, pendingIntent);
    }

    /* startStateHandler: will try to login
        Initial state: START
        possible transitions:
            * START: already logged-in, n/w failed, a timer will be scheduled
            * LOGGED_IN; just logged-in,, a timer will be scheduled
            * STOPPED; causes: config not found
     */
    private void startStateHandler() {
        //clean up any residual objects of last loggedIn state
        File f = new File(getFilesDir()+getResources().getString(R.string.autoAuthObjFile));
        if(f.exists())
            f.delete();
        if (WifiUtil.isWifiConnected(getBaseContext())) {
            Log.d(LOG_TAG, "WIFI is ON");

            WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            // TODO we are using SSID names as filenames... what if it contains a '/' or '\0'?
            String activeWifiName = wifiInfo.getSSID().replace("\"", "").replace("/", ""); // moar robust?

            Log.d(LOG_TAG, activeWifiName + " WIFI found");

            for (String ssid : getStoredSSIDs()) {
                Log.i(LOG_TAG, "Checking " + ssid + " config");
                if (ssid.equals(activeWifiName)) {
                    Log.i(LOG_TAG, "Detected a stored Network '" + ssid + "' for auto login.");
                    wifiConfig = WifiConfig.loadWifiConfig(new File(getFilesDir(), ssid + ".json"));
                    break;
                }
            }

            if (wifiConfig == null) {
                Log.w(LOG_TAG, "No matching configuration found");
                AutoLoginService.setState(getBaseContext(),LoginState.STOPPED);
            } else {
                if (login()) {
                    Log.i(LOG_TAG, "Logged In");
                    // what if authentication is  *not* required? we should keep retrying then?
                    AutoLoginService.setState(getBaseContext(),LoginState.LOGGED_IN);
                    // save the AutoAuth object
                    AutoAuth.save(f,autoAuthObj);
                } else {
                    // login failed but we still have wifi and config, we'll try again
                    AutoLoginService.setState(getBaseContext(),LoginState.START);
                }
            }

            // respawn us again after timeout
            scheduleTimer();
        } else {
            // :'(
            Log.d(LOG_TAG, "WIFI is OFF");
            AutoLoginService.setState(getBaseContext(),LoginState.STOPPED);
        }
    }

    /* loggedInStateHandler: have autoAuthObj, will keep-alive
        Initial state: LOGGED_IN
        possible transitions:
            * START: autoAuthObj was not found, a timer will be scheduled
            * LOGGED_IN; authenticate was successful, a timer will be scheduled
            * STOPPED; authenticate failed, TODO: should we retry?
     */
    private void loggedInStateHandler() {
        File f = new File(getFilesDir()+getResources().getString(R.string.autoAuthObjFile));
        //check if any stored AutoAuthObj present
        //if present load it, if not already loaded
        if(autoAuthObj == null && f.exists())
        {
            autoAuthObj = AutoAuth.load(f);
        }
        if (autoAuthObj != null) {
            Log.d(LOG_TAG, "Keeping alive");
            if (!autoAuthObj.authenticate()) {
                AutoLoginService.setState(getBaseContext(),LoginState.STOPPED);
                Log.w(LOG_TAG, "Keep alive failed");
            }
            else
                AutoAuth.save(f,autoAuthObj);
        } else
            AutoLoginService.setState(getBaseContext(),LoginState.START);

        scheduleTimer();
    }

    /* stoppedStateHandler: destroy everything
        Initial state: STOPPED
        possible transitions:
            * STOPPED; will kill the service
     */
    private void stoppedStateHandler() {
        wifiConfig = null;
        autoAuthObj = null;
        stopSelf();
        File f = new File(getFilesDir()+getResources().getString(R.string.autoAuthObjFile));
        if(f.exists())
            f.delete();
    }
}
