package net.debjeetmaj.androidwifiautologin.auth;

import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Debjeet Majumdar on 29-12-2016.
 */

public class BasicAutoAuth extends AutoAuth {
    public BasicAutoAuth(String authUrl, String username, String password)throws Exception {
        super(authUrl, username, password);
    }

    @Override
    public void keepAlive() {
        return;
    }

    @Override
    public void authenticate() {
        Log.i(LOG_TAG,"Attempting to authenticate.");
        String data = null;
        HttpURLConnection httpConnection  = null;
        int k=2;
        while(k-->0){
            try {
                authUrl = authUrl.replace("https://", "http://");
                byte[] encodedData = Base64.encode((username + ":" + password).getBytes(), Base64.NO_WRAP);
//            String encodedData = String.format("username=%s&password=%s",
//                    URLEncoder.encode(this.username, "UTF-8"),
//                    URLEncoder.encode(this.password, "UTF-8"));
////                    URLEncoder.encode(magic, "UTF-8"));
                httpConnection = (HttpURLConnection) (new URL(authUrl)).openConnection();
                httpConnection.setUseCaches(false);
//            httpsConnection.setDoOutput(true);
//                httpConnection.setRequestMethod("GET");
//                httpConnection.setRequestProperty("WWW-Authenticate", "Basic realm=\"IronPort Web Security Appliance\"");
                httpConnection.setRequestProperty("Authorization", "Basic " + encodedData);
                httpConnection.connect();
//            httpsConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//            httpsConnection.setRequestProperty("Authentication", basicAuth);
//            httpsConnection.setRequestProperty("Content-Length", String.valueOf(encodedData.length()));

//            Log.d(LOG_TAG, "encoded Data = " + encodedData);
//            DataOutputStream os = new DataOutputStream(httpsConnection.getOutputStream());
//            os.writeBytes(encodedData);
//            os.flush();
//            os.close();

                int responseCode = httpConnection.getResponseCode();
                Log.d(LOG_TAG, "POST response: " + responseCode);
                //data = IOUtil.readStream(httpConnection.getInputStream());
                //Log.d(LOG_TAG,data);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (httpConnection != null)
                    httpConnection.disconnect();
            }
        }


    }

}
