package com.fanny.traxivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemAsset;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * Listen to the changes in the Data Layer Event, used to send the collected data from the wear to the mobile
 */
public class ListenerService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private final String TAG="ListenerService";

    /**
     * String to define the path for retrieving the DataMapRequest.
     *
     * @see ListenerService#onDataChanged(DataEventBuffer)
     */
    private static final String WEARABLE_DATA_PATH = "/wearable_data";

    /**
     * The app data folder
     */
    private static final String DATA_FOLDER= "/Traxivity/data";

    private GoogleApiClient googleClient;

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            Class.forName("android.os.AsyncTask");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }


        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        googleClient.connect();
    }

    /**
     * When there is a change in the Data Layer Event, writes the new data in a file and call sendBroadcast to update the visualization in the main activity
     * @see ListenerService#sendBroadcast()
     * @param dataEvents
     */
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo(WEARABLE_DATA_PATH) == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    Long timestamp = dataMap.getLong("timestamp");
                    int stepcount = dataMap.getInt("stepcount");

                    Log.d(TAG,"Received data from the wearable");
                    Log.d(TAG,"Timestamp : "+ timestamp.toString());
                    Log.d(TAG,"Stepcount : "+Integer.toString(stepcount));

                    sendBroadcast();
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }





    }


    /**
     * Send the intent to the BroadcastReceiver dataReceiver in the main activity
     * @see MainActivity#dataReceiver
     */
    public void sendBroadcast() {
        Intent intent = new Intent("newData");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    /**
     * Check if the external storage is writable
     * @return boolean true if the external storage is writable.
     */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return(Environment.MEDIA_MOUNTED.equals(state));
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Wearable.DataApi.addListener(googleClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
