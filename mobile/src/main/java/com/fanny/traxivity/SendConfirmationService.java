package com.fanny.traxivity;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.Date;
import java.util.List;

/**
 * Created by Yan'Shin on 06/06/2017.
 */

public class SendConfirmationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    /**
     * String to define the path for sending the DataMapRequest.
     */
    private static final String WEARABLE_DATA_PATH = "/wearable_data";

    private final String TAG="SendConfirmationService";

    private GoogleApiClient googleClient;

    public SendConfirmationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Build a new GoogleApiClient and connects
     */
    public void onCreate() {
        super.onCreate();

        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        googleClient.connect();


    }

    /**
     * Read the file in memory to construct the dataMap
     * Send the dataMap when the data layer connection is successful.
     * @param connectionHint
     */
    @Override
    public void onConnected(Bundle connectionHint) {


        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(WEARABLE_DATA_PATH);
        putDataMapRequest.getDataMap().putString("confirmation","ok");
        putDataMapRequest.getDataMap().putLong("time", new Date().getTime());
        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        request.setUrgent();
        Wearable.DataApi.putDataItem(googleClient, request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        if (!dataItemResult.getStatus().isSuccess()) {
                        Log.e(TAG, "buildWatchOnlyNotification(): Failed to set the data, "
                            + "status: " + dataItemResult.getStatus().getStatusCode());
                        }
                        }
                });
        Log.d(TAG,"Confirmation sent from Mobile...");
        stopSelf();

    }

    /**
     * Disconnect from the data layer when the Activity stops
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != googleClient && googleClient.isConnected()) {
            googleClient.disconnect();
        }
    }


    /**
     * Placeholder for required connection callbacks
     * @param cause
     */
    @Override
    public void onConnectionSuspended(int cause) { }

    /**
     * Placeholder for required connection callbacks
     * @param connectionResult
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) { }

}
