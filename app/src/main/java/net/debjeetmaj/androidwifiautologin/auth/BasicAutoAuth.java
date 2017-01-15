package net.debjeetmaj.androidwifiautologin.auth;

import android.util.Base64;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static net.debjeetmaj.androidwifiautologin.util.IOUtil.readStream;

public class BasicAutoAuth extends AutoAuth {
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
                // danger mode "ON"
                disableSSLTrustChecks();

                httpsConnection = (HttpsURLConnection) (new URL(authUrl)).openConnection();
                httpsConnection.setUseCaches(false);
                httpsConnection.setRequestMethod("GET");
                httpsConnection.setRequestProperty("Authorization", "Basic " +
                        Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP));
                httpsConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                httpsConnection.connect();

                int responseCode = httpsConnection.getResponseCode();
                // TODO add checks here; figure out which checks you need first
                Log.d(LOG_TAG, "Auth response code: " + responseCode);
                data = readStream(httpsConnection.getInputStream());
                Log.d(LOG_TAG,data);

                String encodedData = String.format("username=%s&password=%s&sid=0",
                        URLEncoder.encode(this.username, "UTF-8"),
                        URLEncoder.encode(this.password, "UTF-8"));

                // no-proxy auth
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
                if (responseCode != HttpsURLConnection.HTTP_OK) {
                    data = readStream(httpsConnection.getInputStream());
                    Log.d(LOG_TAG, "response data:" + data);
                    return false;
                }

                return true;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                e.printStackTrace();
                return false;
            } finally {
                if (httpsConnection != null)
                    httpsConnection.disconnect();
            }
        }
    return false;
}

    private void disableSSLTrustChecks() throws NoSuchAlgorithmException, KeyManagementException {
        /* "Borrowed" from
        http://stackoverflow.com/questions/3761737/https-get-ssl-with-android-and-self-signed-server-certificate
         */
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, new TrustManager[] {
                new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[]{}; }
                }
        }, null);

        HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier(){
            public boolean verify(String hostname, SSLSession session) { return true; }
        });
    }

    @Override
    public int sleepTimeout() {
        return 3600000; // 1 hr; "official" timeout is 2 hrs / 7200 secs
    }
}
