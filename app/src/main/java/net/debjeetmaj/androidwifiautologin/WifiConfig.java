package net.debjeetmaj.androidwifiautologin;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.security.MessageDigest;
import java.util.Scanner;

class WifiConfig {
    private static String LOG_TAG = "WifiConfig";
    
    private String ssid,username,password;
    private boolean keepAlive;

    WifiConfig(String ssid) {
        this.ssid = ssid;
    }

    String getUsername() {
        return username;
    }

    void setUsername(String username) {
        this.username = username;
    }

    String getSsid() {
        return ssid;
    }

    // dead code?
    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    String getPassword() {
        return password;
    }

    void setPassword(String password) {
        this.password = password;
    }

    boolean isKeepAlive() {
        return keepAlive;
    }

    void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    private String toJSONString(){
        JSONObject object = new JSONObject();
        try {
            object.put("ssid", ssid);
            object.put("username", username);
            object.put("password", password);
            object.put("keepalive",keepAlive);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object.toString();
    }

    private static String md5(final String str) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("md5");
            digest.update(str.getBytes());
            final byte[] bytes = digest.digest();
            final StringBuilder sb = new StringBuilder();
            for (byte aByte : bytes) sb.append(String.format("%02X", aByte));
            return sb.toString().toLowerCase();
        } catch (Exception ex) {
            return "";
        }
    }

    static String getFileName(String ssid) {
        return md5(ssid) + ".json";
    }

    private static String readSSID(File file) {
        String content;
        try {
            content = new Scanner(file).useDelimiter("\\Z").next();
            JSONObject jsonObject = new JSONObject(content);
            return jsonObject.get("ssid").toString();
        } catch (FileNotFoundException | JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

    static String[] getStoredSSIDs(Context ctx) {
        final String[] fnames = ctx.getFilesDir().list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".json");
            }
        });
        String ssids[] = new String[fnames.length];
        for (int i = 0; i < ssids.length; i++)
            ssids[i] = readSSID(new File(ctx.getFilesDir(), fnames[i]));

        return ssids;
    }

    static WifiConfig loadWifiConfig(File file){
        WifiConfig wifiConfig = null;
        try {
            String content = new Scanner(file).useDelimiter("\\Z").next();
            JSONObject jsonObject = new JSONObject(content);
            String ssid = jsonObject.get("ssid").toString();

            wifiConfig = new WifiConfig(ssid);
            wifiConfig.setUsername(jsonObject.get("username").toString());
            wifiConfig.setPassword(jsonObject.get("password").toString());
            wifiConfig.setKeepAlive((Boolean)jsonObject.get("keepalive"));
        } catch (Exception ex){
            Log.e(LOG_TAG, ex.getMessage());
        }
        return wifiConfig;
    }

    static void saveWifiConfig(WifiConfig wifiConfig, File file) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(wifiConfig.toJSONString().getBytes());
            fos.close();

            if(file.exists())
                Log.i(LOG_TAG,wifiConfig.getSsid()+" Config written.");
            else
                Log.i(LOG_TAG,wifiConfig.getSsid()+" Config not written.");
        }
        catch (Exception ex){
            Log.e(LOG_TAG, ex.getMessage());
        }
    }
}
