package net.debjeetmaj.androidwifiautologin;

import android.app.IntentService;
import android.content.Intent;
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
import java.util.Timer;
import java.util.TimerTask;

public class AutoLoginService extends IntentService {
    public final static String LOG_TAG = "AutoLoginService";
    public final static int CONNECTION_CHECK_ATTEMPTS = 10;
    public final static int RETRY_TIMEOUT = 10000; // 10 secs

    private static WifiConfig wifiConfig = null;
    private static AutoAuth autoAuthObj = null;
    private static LoginState state = null;

    public AutoLoginService() {
        super("Auto Login Service");
        Log.w(LOG_TAG, "Service created");
//        AutoLoginService.setState(LoginState.STOPPED);
    }

    public static LoginState getState() {
        return state;
    }

    public static void setState(LoginState state) {
        AutoLoginService.state = state;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(LOG_TAG, "Service Started");
        Log.i(LOG_TAG, "current state: " + AutoLoginService.getState().toString());

        switch (AutoLoginService.getState()) {
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
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                Intent intent = new Intent(getApplicationContext(), AutoLoginService.class);
                getApplicationContext().startService(intent);
            }
        }, getState() == LoginState.STOPPED ? 10 :
                // getState() == LoginState.LOGGED_IN
                autoAuthObj != null ? autoAuthObj.sleepTimeout() :
                // getState() == LoginState.START
                        RETRY_TIMEOUT);
    }

    /* startStateHandler: will try to login
        Initial state: START
        possible transitions:
            * START: already logged-in, n/w failed, a timer will be scheduled
            * LOGGED_IN; just logged-in,, a timer will be scheduled
            * STOPPED; causes: config not found
     */
    private void startStateHandler() {
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
                AutoLoginService.setState(LoginState.STOPPED);
            } else {
                if (login()) {
                    Log.i(LOG_TAG, "Logged In");
                    // what if authentication is  *not* required? we should keep retrying then?
                    AutoLoginService.setState(LoginState.LOGGED_IN);
                } else {
                    // login failed but we still have wifi and config, we'll try again
                    AutoLoginService.setState(LoginState.START);
                }
            }

            // respawn us again after timeout
            scheduleTimer();
        } else {
            // :'(
            Log.d(LOG_TAG, "WIFI is OFF");
            AutoLoginService.setState(LoginState.STOPPED);
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
        if (autoAuthObj != null) {
            Log.d(LOG_TAG, "Keeping alive");

            if (!autoAuthObj.authenticate()) {
                AutoLoginService.setState(LoginState.STOPPED);
                Log.w(LOG_TAG, "Keep alive failed");
            }
        } else
            AutoLoginService.setState(LoginState.START);

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
    }
}
