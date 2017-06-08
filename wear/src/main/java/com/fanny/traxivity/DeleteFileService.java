package com.fanny.traxivity;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.File;

/**
 * Created by Yan'Shin on 07/06/2017.
 */

public class DeleteFileService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private final String TAG="DeleteFileService";

    private static final String WEARABLE_DATA_PATH = "/wearable_data";

    private GoogleApiClient googleClient;


    public void onCreate(){
        super.onCreate();

        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        googleClient.connect();


    }

    /**
     * When there is a change in the Data Layer Event, writes the new data in a file and call sendBroadcast to update the visualization in the main activity
     * @param dataEvents
     */
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo(WEARABLE_DATA_PATH) == 0) {
                    Log.d(TAG, "Receiving data...");
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    String str = dataMap.getString("confirmation");

                        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

                        File fileDelete = new File(settings.getString("path",""), settings.getString("fileName",""));
                        fileDelete.delete();
                        Log.d(TAG,"These files were deleted...\n"+settings.getString("fileName",""));
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString("path", "");
                        editor.putString("fileName", "");
                        editor.commit();
                        Log.d(TAG,"SharedPreferences are now cleared ! ");



                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }

        stopSelf();

    }


    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(googleClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
