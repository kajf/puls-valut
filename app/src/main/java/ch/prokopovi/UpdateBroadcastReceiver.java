package ch.prokopovi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.JobIntentService;
import android.util.Log;

public class UpdateBroadcastReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = "UpdateBR";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(LOG_TAG, "received " + intent);

        if (IntentFactory.ACTION_FORCE_UPDATE.equals(intent.getAction())) {

            Intent serviceIntent = new Intent(context, UpdateService.class);
            serviceIntent.setAction(intent.getAction());
            serviceIntent.putExtras(intent);

            JobIntentService.enqueueWork(context, UpdateService.class, 100, serviceIntent);
        }
    }
}
