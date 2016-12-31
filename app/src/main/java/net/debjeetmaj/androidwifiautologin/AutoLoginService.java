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

public class AutoLoginService extends IntentService {
    public final static String LOG_TAG = "AutoLoginService";
    private static WifiConfig wifiConfig = null;
    private static AutoAuth autoAuthObj = null;
    private static LoginState state;

//    private int maxLoginAttempt = 5;
//    private int loginAttemptInterval = 50000; //msecs

    public AutoLoginService(){
        super("Auto Login Service");
        Log.w(LOG_TAG,"Service created");
        AutoLoginService.setState(LoginState.STOPPED);
    }

    public static LoginState getState() {
        return state;
    }

    public static void setState(LoginState state) {
        AutoLoginService.state = state;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.w(LOG_TAG,"Service Started");

        switch (AutoLoginService.getState()){
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
        Log.w(LOG_TAG,"Service Stopped");
        super.onDestroy();
    }

    private String[] getStoredSSIDs(){
        String[] ssids = getFilesDir().list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".json");
            }
        });
        for(int i=0;i<ssids.length;i++){
            ssids[i]=ssids[i].substring(0,ssids[i].indexOf('.'));
        }
        return ssids;
    }
    /*
    * checks if internet connection is active,
    * If auth required sets appropriate AutoAuth object and returns true
    * else returns false
    * */
    protected boolean checkAuthRequired(){
        HttpURLConnection httpConnection = null;
        String authUrl =  null;
        try {
            httpConnection = (HttpURLConnection) (new URL("http://www.bing.com/")).openConnection();
            httpConnection.setRequestMethod("GET");
            httpConnection.setRequestProperty("Content-length", "0");
            httpConnection.setUseCaches(false);
            httpConnection.setAllowUserInteraction(false);
            httpConnection.setConnectTimeout(100000); // in msecs; 100 secs
            httpConnection.setReadTimeout(100000);

            httpConnection.connect();
            int responseCode = httpConnection.getResponseCode();
            Log.i(LOG_TAG, "connection response: " + responseCode);
            // see other, Http status 303, proxy redirect  : 307
            if (responseCode != HttpURLConnection.HTTP_SEE_OTHER && responseCode != 307) {
                Log.d(LOG_TAG, "Internet is on");
                return false;
            }

            authUrl = httpConnection.getHeaderField("Location");
            Log.i(LOG_TAG, "Auth URL: " + authUrl);
            //Create autoAuthObj based on authUrl
            //TODO : generalise the code to make it rule based
            if(authUrl.contains("/fgtauth?")){
                //Fortigate firewall mechanism
                autoAuthObj = new FortigateAutoAuth(authUrl,wifiConfig.getUsername(),wifiConfig.getPassword());
            }
            else{
                autoAuthObj = new BasicAutoAuth(authUrl,wifiConfig.getUsername(),wifiConfig.getPassword());
            }

        } catch (IOException e) {
            Log.e(LOG_TAG, "IOException");
            e.printStackTrace();
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        } finally {
            if (httpConnection != null)
                httpConnection.disconnect();
        }

        return true;
    }
    private void startStateHandler(){
        if (WifiUtil.isWifiConnected(getBaseContext())) {
            Log.i(LOG_TAG, "WIFI is ON");
            WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String activeWifiName = wifiInfo.getSSID().replace("\"", "");
            Log.i(LOG_TAG, activeWifiName + " WIFI found");
            for (String ssid : getStoredSSIDs()) {
                Log.i(LOG_TAG, "Checking " + ssid + " config");
                if (ssid.equals(activeWifiName)) {
                    Log.i(LOG_TAG, "Detected a stored Network " + ssid + " for auto login.");
                    wifiConfig = WifiConfig.loadWifiConfig(new File(getFilesDir(), ssid + ".json"));
                    break;
                }
            }
            if (wifiConfig == null) {
                Log.i(LOG_TAG, "No matching configuration found");
                return;
            }

            login();
            AutoLoginService.setState(LoginState.LOGGED_IN);
        } else {
            Log.d(LOG_TAG, "WIFI is OFF");
        }
    }
    private void loggedInStateHandler(){
        assert autoAuthObj!=null;
        autoAuthObj.keepAlive();
    }
    private void stoppedStateHandler(){
        wifiConfig=null;
        autoAuthObj=null;
    }
    private void login() {
        if(checkAuthRequired()){
            assert autoAuthObj!=null;
            autoAuthObj.authenticate();
        }
        else{
            Log.i(LOG_TAG,"Authentication not required.");
        }
    }
}
