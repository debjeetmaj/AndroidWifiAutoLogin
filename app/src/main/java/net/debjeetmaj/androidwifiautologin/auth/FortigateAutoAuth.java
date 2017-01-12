package net.debjeetmaj.androidwifiautologin.auth;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import static net.debjeetmaj.androidwifiautologin.util.IOUtil.readStream;

public class FortigateAutoAuth extends AutoAuth {
    private String keepAliveUrl = null;

    public FortigateAutoAuth(String authUrl, String username, String password) throws Exception {
        super(authUrl, username, password);
    }

    /* make sure keepaliveUrl is not null */
    private boolean keepAlive() {
        assert keepAliveUrl != null;
        Log.d(LOG_TAG, "keep alive url is "+keepAliveUrl);
        HttpsURLConnection httpsConnection  = null;
        try {
            httpsConnection = (HttpsURLConnection) (new URL(keepAliveUrl)).openConnection();
            httpsConnection.setRequestMethod("GET");
            httpsConnection.setUseCaches(false);
            httpsConnection.setAllowUserInteraction(false);
            httpsConnection.setConnectTimeout(CONNECTION_TIMEOUT);
            httpsConnection.setReadTimeout(CONNECTION_TIMEOUT);

            httpsConnection.connect();
            int responseCode = httpsConnection.getResponseCode();
            Log.d(LOG_TAG, "keep alive: response code: " + responseCode);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                String data = readStream(httpsConnection.getInputStream());
                Log.d(LOG_TAG, "keep alive: data: " + data);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (httpsConnection != null)
                httpsConnection.disconnect();
        }
        return true;
    }

    @Override
    /*
     * return true if authentication was successful
     */
    public boolean authenticate() {
        if (keepAliveUrl != null)
            return keepAlive();

        String data = null;
        HttpsURLConnection httpsConnection = null;
        for (int i = 0; i < CONNECTION_CHECK_ATTEMPTS; ++i) {
            try {
                httpsConnection = (HttpsURLConnection) (new URL(authUrl)).openConnection();
                httpsConnection.setRequestMethod("GET");
                httpsConnection.setUseCaches(false);
                httpsConnection.setAllowUserInteraction(false);
                httpsConnection.setConnectTimeout(CONNECTION_TIMEOUT);
                httpsConnection.setReadTimeout(CONNECTION_TIMEOUT);

                httpsConnection.connect();
                data = readStream(httpsConnection.getInputStream());
//                Log.d(LOG_TAG, "authenticate: response data: " + data);
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                if (httpsConnection != null)
                    httpsConnection.disconnect();
            }

            Pattern p = Pattern.compile("value=\"([0-9a-f]+)\"");
//            assert data != null;
            Matcher m = p.matcher(data);
            if (!m.find()) {
                Log.e(LOG_TAG, "authenticate: magic string not found");
                return false;
            }
            String magic = m.group(1);
            Log.d(LOG_TAG, "authenticate: magic string is " + magic);

            data = null;
            httpsConnection = null;
            try {
                String encodedData = String.format("username=%s&password=%s&magic=%s&4Tredir=%%2F",
                        URLEncoder.encode(this.username, "UTF-8"),
                        URLEncoder.encode(this.password, "UTF-8"),
                        URLEncoder.encode(magic, "UTF-8"));
                httpsConnection = (HttpsURLConnection) (new URL(authUrl)).openConnection();
                httpsConnection.setDoOutput(true);
                httpsConnection.setRequestMethod("POST");
                httpsConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                httpsConnection.setRequestProperty("Content-Length", String.valueOf(encodedData.length()));

//                Log.d(LOG_TAG, "authenticate: encoded data: " + encodedData);
                DataOutputStream os = new DataOutputStream(httpsConnection.getOutputStream());
                os.writeBytes(encodedData);
                os.flush();
                os.close();

                int responseCode = httpsConnection.getResponseCode();
                Log.d(LOG_TAG, "authenticate: POST response: " + responseCode);
                data = readStream(httpsConnection.getInputStream());

            } catch (IOException e) {
                e.printStackTrace();
                continue;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                if (httpsConnection != null)
                    httpsConnection.disconnect();
            }

            p = Pattern.compile("location.href=\"(.+?)\"");
            assert data != null;
            m = p.matcher(data);
            if (!m.find()) {
                Log.e(LOG_TAG, "authenticate: keep alive url not found");
                return false;
            }
            keepAliveUrl = m.group(1);
            Log.d(LOG_TAG, "authenticate: keep alive url is " + keepAliveUrl);
            return true;
        }
        return false;
    }

    public int sleepTimeout() { return 2200000; } // 2200 secs TODO Make it configurable
}
