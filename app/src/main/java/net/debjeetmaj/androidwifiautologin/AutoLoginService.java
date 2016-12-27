package net.debjeetmaj.androidwifiautologin;

import android.app.IntentService;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by hp 1 on 27-12-2016.
 */

public class AutoLoginService extends IntentService {
    public final static String LOG_TAG = "AutoLoginService";
    public AutoLoginService(){
        super("Auto Login Service");
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(LOG_TAG,"Service Started");
        if(WifiUtil.isWifiConnected(getBaseContext())){
            Log.i(LOG_TAG,"WIFI found");
            WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String activeWifiName = wifiInfo.getSSID().replace("\"","");
            Log.i(LOG_TAG,activeWifiName + "WIFI found");
            for(String ssid : getStoredSSIDs()){
                Log.i(LOG_TAG,ssid + " config");
                if(ssid.equals(activeWifiName)){
                    Log.d(LOG_TAG,"Detected a stored Network "+ssid+" for auto login.");
                    WifiConfig wifiConfig = WifiConfig.loadWifiConfig(new File(getFilesDir(),ssid+".json"));
                    //auto Login code
                    checkInternet();
                    break;
                }
            }
        }
        else{
            Log.d(LOG_TAG,"WIFI Not found");
        }
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG,"Service is Stopped");
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

    private void checkInternet(){
        try {

            URL mUrl = new URL("http://www.bing.com/");
            HttpURLConnection httpConnection = (HttpURLConnection) mUrl.openConnection();
            httpConnection.setRequestMethod("GET");
            httpConnection.setRequestProperty("Content-length", "0");
            httpConnection.setUseCaches(false);
            httpConnection.setAllowUserInteraction(false);
            httpConnection.setConnectTimeout(100000);
            httpConnection.setReadTimeout(100000);

            httpConnection.connect();

            int responseCode = httpConnection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
//                BufferedReader br = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
//                StringBuilder sb = new StringBuilder();
//                String line;
//                while ((line = br.readLine()) != null) {
//                    sb.append(line + "\n");
//                }
//                br.close();
//                //Log.d(LOG_TAG,sb.toString());
//                //return sb.toString();
                Log.d(LOG_TAG,"Internet is on");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
