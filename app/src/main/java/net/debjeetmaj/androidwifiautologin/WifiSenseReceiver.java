package net.debjeetmaj.androidwifiautologin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * Created by hp 1 on 27-12-2016.
 */

public class WifiSenseReceiver extends BroadcastReceiver {
    public final static String LOG_TAG = "WifiSenseReciever";
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = conMan.getActiveNetworkInfo();
            if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI)
                Log.d(LOG_TAG, "Have Wifi Connection");
            else
                Log.d(LOG_TAG, "Don't have Wifi Connection");
        }
        catch (Exception ex){
            Log.e(LOG_TAG,ex.getMessage());
        }
    }
}
