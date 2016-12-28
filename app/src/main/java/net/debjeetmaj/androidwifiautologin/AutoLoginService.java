package net.debjeetmaj.androidwifiautologin;

import android.app.IntentService;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class AutoLoginService extends IntentService {
    public final static String LOG_TAG = "AutoLoginService";
    private WifiConfig wifiConfig = null;

    public AutoLoginService(){
        super("Auto Login Service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.w(LOG_TAG,"Service Started");
        if(WifiUtil.isWifiConnected(getBaseContext())){
            Log.i(LOG_TAG,"WIFI is ON");
            WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String activeWifiName = wifiInfo.getSSID().replace("\"","");
            Log.i(LOG_TAG,activeWifiName + " WIFI found");
            for(String ssid : getStoredSSIDs()){
                Log.i(LOG_TAG, "Checking " + ssid + " config");
                if(ssid.equals(activeWifiName)){
                    Log.d(LOG_TAG,"Detected a stored Network "+ssid+" for auto login.");
                    wifiConfig = WifiConfig.loadWifiConfig(new File(getFilesDir(),ssid+".json"));
                    break;
                }
            }
            if (wifiConfig == null) {
                Log.i(LOG_TAG, "No matching configuration found");
                return;
            }

            login();
        }
        else{
            Log.d(LOG_TAG,"WIFI is OFF");
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

//    interface StateFunc {
//        StateFunc func();
//    }

    String readStream(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        br.close();
        return sb.toString();
    }

    private void login() {
        String authUrl = null;
        HttpURLConnection httpConnection = null;

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

            if (responseCode != HttpURLConnection.HTTP_SEE_OTHER) {
                Log.d(LOG_TAG, "Internet is on");
                return;
            }

            authUrl = httpConnection.getHeaderField("Location");
            Log.i(LOG_TAG, "Auth URL: " + authUrl);
        } catch (IOException e) {
            Log.e(LOG_TAG, "IOException");
            e.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (httpConnection != null)
                httpConnection.disconnect();
        }

        String data = null;
        HttpsURLConnection httpsConnection  = null;
        try {
            httpsConnection = (HttpsURLConnection) (new URL(authUrl)).openConnection();
            httpsConnection.setRequestMethod("GET");
            httpsConnection.setUseCaches(false);
            httpsConnection.setAllowUserInteraction(false);
            httpsConnection.setConnectTimeout(100000); // in msecs; 100 secs
            httpsConnection.setReadTimeout(100000);

            httpsConnection.connect();
            data = readStream(httpsConnection.getInputStream());
            Log.d(LOG_TAG, "Data:\n" + data);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (httpsConnection != null)
                httpsConnection.disconnect();
        }

        Pattern p = Pattern.compile("value=\"([0-9a-f]+)\"");
        assert data != null;
        Matcher m = p.matcher(data);
        if (!m.find()) {
            Log.e(LOG_TAG, "magic string not found");
            return;
        }
        String magic = m.group(1);
        Log.d(LOG_TAG, "magic string is " + magic);

        data = null;
        httpsConnection = null;
        try {
            String encodedData = String.format("username=%s&password=%s&magic=%s&4Tredir=%%2F",
                    URLEncoder.encode(wifiConfig.getUsername(), "UTF-8"),
                    URLEncoder.encode(wifiConfig.getPassword(), "UTF-8"),
                    URLEncoder.encode(magic, "UTF-8"));
            httpsConnection = (HttpsURLConnection) (new URL(authUrl)).openConnection();
            httpsConnection.setDoOutput(true);
            httpsConnection.setRequestMethod("POST");
            httpsConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpsConnection.setRequestProperty("Content-Length", String.valueOf(encodedData.length()));

            Log.d(LOG_TAG, "encoded Data = " + encodedData);
            DataOutputStream os = new DataOutputStream(httpsConnection.getOutputStream());
            os.writeBytes(encodedData);
            os.flush();
            os.close();

            int responseCode = httpsConnection.getResponseCode();
            Log.d(LOG_TAG, "POST response: " + responseCode);
            data = readStream(httpsConnection.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (httpsConnection != null)
                httpsConnection.disconnect();
        }

        p = Pattern.compile("location.href=\"(.+?)\"");
        assert data != null;
        m = p.matcher(data);
        if (!m.find()) {
            Log.e(LOG_TAG, "keep alive url not found");
            return;
        }
        String keepAliveUrl = m.group(1);
        Log.d(LOG_TAG, "keep alive url is " + keepAliveUrl);
    }
}
