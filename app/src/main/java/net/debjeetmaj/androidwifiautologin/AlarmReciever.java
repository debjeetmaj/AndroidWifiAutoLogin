package net.debjeetmaj.androidwifiautologin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Debjeet on 06-01-2017.
 */

public class AlarmReciever extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(AutoLoginService.LOG_TAG,"Alarm receiver called");
        Intent intent1 = new Intent(context, AutoLoginService.class);
        context.startService(intent1);
    }
}
