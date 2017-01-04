package net.debjeetmaj.androidwifiautologin.auth;

import android.util.Base64;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import static net.debjeetmaj.androidwifiautologin.util.IOUtil.readStream;

public class BasicAutoAuth extends AutoAuth {
    private final static int CONNECTION_CHECK_ATTEMPTS = 10;

    public BasicAutoAuth(String authUrl, String username, String password)throws Exception {
        super(authUrl, username, password);
    }

    @Override
    public boolean authenticate() {
        Log.i(LOG_TAG, "Attempting to authenticate.");
        String data = null;
        HttpsURLConnection httpsConnection = null;

        for (int i = 0; i < CONNECTION_CHECK_ATTEMPTS; ++i) {
            try {
                HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier(){
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }});
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, new HttpsTrustManager[]{new HttpsTrustManager()}, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());

                httpsConnection = (HttpsURLConnection) (new URL(authUrl)).openConnection();
                httpsConnection.setUseCaches(false);
                httpsConnection.setRequestMethod("GET");
                String authCred = Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP);
                Log.d(LOG_TAG, "authStr = " + authCred);
                httpsConnection.setRequestProperty("Authorization", "Basic " + authCred);
                httpsConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                httpsConnection.connect();

                int responseCode = httpsConnection.getResponseCode();
                // TODO add checks here
                Log.d(LOG_TAG, "Auth response code: " + responseCode);
                data = readStream(httpsConnection.getInputStream());
                Log.d(LOG_TAG,data);

                String encodedData = String.format("username=%s&password=%s&sid=0",
                        URLEncoder.encode(this.username, "UTF-8"),
                        URLEncoder.encode(this.password, "UTF-8"));

                httpsConnection = (HttpsURLConnection) (new URL("https://authenticate.iitk.ac.in/netaccess/loginuser.html")).openConnection();
                httpsConnection.setDoOutput(true);
                httpsConnection.setRequestMethod("POST");
                httpsConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                httpsConnection.setRequestProperty("Content-Length", String.valueOf(encodedData.length()));

                DataOutputStream os = new DataOutputStream(httpsConnection.getOutputStream());
                os.writeBytes(encodedData);
                os.flush();
                os.close();

                responseCode = httpsConnection.getResponseCode();
                Log.d(LOG_TAG, "authenticate: POST response: " + responseCode);
                // TODO add checks here
                data = readStream(httpsConnection.getInputStream());
                Log.d(LOG_TAG, "response data:" + data);

                return true;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            } finally {
                if (httpsConnection != null)
                    httpsConnection.disconnect();
            }
        }
    return false;
}

    @Override
    public int sleepTimeout() {
        return 10000; // "official" timeout is 2 hrs / 7200 secs
    }
}
