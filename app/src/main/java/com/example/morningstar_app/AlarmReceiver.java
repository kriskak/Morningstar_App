package com.example.morningstar_app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
    public static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG,"Alarm triggered");
        context.sendBroadcast(new Intent("START_RECEIVING"));
        /*Intent i = new Intent(context, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //--------------- Testing
        context.startActivity(i);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                context.sendBroadcast(new Intent("START_RECEIVING"));
            }
        },30);
*/
    }

    @Override
    public IBinder peekService(Context myContext, Intent service) {
        return super.peekService(myContext, service);
    }
}
