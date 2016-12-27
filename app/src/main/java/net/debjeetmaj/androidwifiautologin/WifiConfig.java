package net.debjeetmaj.androidwifiautologin;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * Created by hp 1 on 26-12-2016.
 */

public class WifiConfig {
    private String ssid,username,password;
    private boolean keepAlive;

    public WifiConfig(String ssid) {
        this.ssid = ssid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public String toJSONString(){
//        return "{ \"ssid\" :\""+ ssid + "\", " +
//                "\"username\" :\""+ username+ "\", " +
//                "\"password\" :\""+ password+ "\" "
//                + " }";
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
    public static WifiConfig loadWifiConfig(File file){
        WifiConfig wifiConfig=null;
        try {
            String content = new Scanner(file).useDelimiter("\\Z").next();
            JSONObject jsonObject = new JSONObject(content);
            String ssid = file.getName().substring(0,file.getName().indexOf('.'));
            wifiConfig = new WifiConfig(ssid);
            wifiConfig.setUsername(jsonObject.get("username").toString());
            wifiConfig.setPassword(jsonObject.get("password").toString());
            wifiConfig.setKeepAlive((Boolean) jsonObject.get("keepalive"));
        }
        catch (IOException ex){
            Log.e("",ex.getMessage());
        }
        catch (Exception ex){
            Log.e("",ex.getMessage());
        }
        return wifiConfig;
    }
}
