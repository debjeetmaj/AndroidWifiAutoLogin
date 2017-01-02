package net.debjeetmaj.androidwifiautologin;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

class WifiUtil {
    static boolean isWifiConnected(Context context){
        try {
            ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getActiveNetworkInfo();

            return mWifi != null && mWifi.getType() == ConnectivityManager.TYPE_WIFI && mWifi.isConnected();
        } catch (Exception ignored) { }
        return false;
    }
}
