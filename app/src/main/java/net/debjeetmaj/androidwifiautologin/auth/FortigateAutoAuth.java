package net.debjeetmaj.androidwifiautologin.auth;

import android.util.Log;

import net.debjeetmaj.androidwifiautologin.util.IOUtil;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import static net.debjeetmaj.androidwifiautologin.util.IOUtil.readStream;

/**
 * Created by Debjeet Majumdar on 29-12-2016.
 */

public class FortigateAutoAuth extends AutoAuth {
    String keepAliveUrl = null;
    public FortigateAutoAuth(String authUrl, String username, String password) throws Exception{
        super(authUrl, username, password);
    }

    /* make sure keepaliveUrl is not null */
    public void keepalive()
    {
        HttpsURLConnection httpsConnection  = null;
        try {
            httpsConnection = (HttpsURLConnection) (new URL(keepAliveUrl)).openConnection();
            httpsConnection.setRequestMethod("GET");
            httpsConnection.setUseCaches(false);
            httpsConnection.setAllowUserInteraction(false);
            httpsConnection.setConnectTimeout(100000); // in msecs;
            httpsConnection.setReadTimeout(100000);

            httpsConnection.connect();

            int responseCode = httpsConnection.getResponseCode();
            Log.d(LOG_TAG, "keep alive response code : " + responseCode);

            String data = IOUtil.readStream(httpsConnection.getInputStream());
            Log.d(LOG_TAG, "keep alive data: " + data);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (httpsConnection != null)
                httpsConnection.disconnect();
        }
    }

    @Override
    public void authenticate() {
        if (keepAliveUrl != null) {
            keepalive();
            return;
        }
        String data = null;
        HttpsURLConnection httpsConnection  = null;
        try {
            httpsConnection = (HttpsURLConnection) (new URL(authUrl)).openConnection();
            httpsConnection.setRequestMethod("GET");
            httpsConnection.setUseCaches(false);
            httpsConnection.setAllowUserInteraction(false);
            httpsConnection.setConnectTimeout(100000); // in msecs;
            httpsConnection.setReadTimeout(100000);

            httpsConnection.connect();
            data = IOUtil.readStream(httpsConnection.getInputStream());
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
                    URLEncoder.encode(this.username, "UTF-8"),
                    URLEncoder.encode(this.password, "UTF-8"),
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
        keepAliveUrl = m.group(1);
        Log.d(LOG_TAG, "keep alive url is " + keepAliveUrl);
    }

    @Override
    public void keepAlive() {
        return;
    }
}
