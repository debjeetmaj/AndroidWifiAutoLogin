package net.debjeetmaj.androidwifiautologin;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by hp 1 on 27-12-2016.
 */

public class WifiUtil {
//    Context context;
//
//    public WifiUtil(Context context) {
//        this.context = context;
//    }

    public static boolean isWifiConnected(Context context){
        try {
            ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getActiveNetworkInfo();

            if (mWifi!=null && mWifi.getType()== ConnectivityManager.TYPE_WIFI && mWifi.isConnected()) {
                return true;
            }
        }catch (Exception ex){}
        return false;
    }
}
