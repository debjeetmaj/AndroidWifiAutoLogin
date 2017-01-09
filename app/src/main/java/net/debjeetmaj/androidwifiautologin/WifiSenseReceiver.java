package net.debjeetmaj.androidwifiautologin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class WifiSenseReceiver extends BroadcastReceiver {
    public final static String LOG_TAG = "WifiSenseReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, AutoLoginService.class);
        try {
            ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = conMan.getActiveNetworkInfo();
            if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                Log.d(LOG_TAG, "Have Wifi Connection");
                AutoLoginService.setState(context,LoginState.START);
                context.startService(serviceIntent);
            }
            else {
                Log.d(LOG_TAG, "Don't have Wifi Connection");
                //destroy the on going service if present
                AutoLoginService.setState(context,LoginState.STOPPED);
                context.stopService(serviceIntent);
            }
        }
        catch (Exception ex){
            Log.e(LOG_TAG, ex.getMessage());
        }
    }
}
