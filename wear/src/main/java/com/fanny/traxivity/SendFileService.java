package com.fanny.traxivity;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.List;


/**
 * Need to modify it so that we can send only the stepcount stored in the sharedpreferences with a timestamp added just before sending the data.
 * It means really simplifying this code so that we only send one value and one timestamp.
 */

public class SendFileService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private static final String PREFERENCE_NAME = "PreferenceFile";

    private final String TAG="SendFileService";

    /**
     * String to define the path for sending the DataMapRequest.
     */
    private static final String WEARABLE_DATA_PATH = "/wearable_data";

    /**
     * The app data folder
     */
    private static final String DATA_FOLDER= "/Traxivity/data";
    private GoogleApiClient googleClient;


    public SendFileService() {
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

        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Node> connectedNodes = Wearable.NodeApi.getConnectedNodes(googleClient).await().getNodes();

                Log.d(TAG,"Listing nodes...");

                for (Node node:connectedNodes) {

                    if(node.isNearby()){

                        Log.d(TAG, "Sending data to " + node.getDisplayName() + "...");

                        sendFileContent();
                    }else{
                        Log.d(TAG, node.getDisplayName() + "isn't nearby, can't send data...");
                    }

                }


            }
        }).start();

    }


    public void sendStepcount(){

        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(WEARABLE_DATA_PATH);
        putDataMapRequest.getDataMap().putInt("stepcount",PreferenceManager.getDefaultSharedPreferences(this).getInt("stepCount",0));
        //putDataMapRequest.getDataMap().putInt("stepDiff",PreferenceManager.getDefaultSharedPreferences(this).getInt("stepDiff",0));
        //putDataMapRequest.getDataMap().putLong("lastUpdateTime",PreferenceManager.getDefaultSharedPreferences(this).getLong("lastUpdateTime",0));

        //Sending the current time in milliseconds
        putDataMapRequest.getDataMap().putLong("timestamp",System.currentTimeMillis());
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
        Log.d(TAG,"Data sent from Wearable");


        stopSelf();

    }

    public void sendFileContent(){
        Log.d(TAG,"Beginning of SendFileContent");
        String line = "";
        String tmp;
        String fileName;

        BufferedReader buffreader = null;

        //String path = Environment.getExternalStorageDirectory().toString()+DATA_FOLDER;
        String path = getFilesDir().toString()+DATA_FOLDER;
        Log.d("Files", "Path: " + path);
        File f = new File(path);
        File files[] = f.listFiles();
        if(files!=null){
            Log.d("Files", "Size: "+ files.length);
        }else{
            Log.d("Files", "Size: 0");
        }
        DataMap dataMap = new DataMap();
        for (int i=0; (files!=null) && (i < files.length); i++) {
            fileName = files[i].getName();

            Asset asset = null;

            try {

                buffreader = new BufferedReader(new FileReader(path + "/" + fileName));
                // read every line of the file into the line-variable, one line at the time
                while (null != (tmp = buffreader.readLine())) {
                    line += tmp + "\n";
                }

                dataMap.putString("path", path);
                dataMap.putString("fileName", fileName);

                dataMap.putLong("time", new Date().getTime());
                dataMap.putString("data", line);
                dataMap.putInt("stepcount",PreferenceManager.getDefaultSharedPreferences(this).getInt("stepCount",0));
                dataMap.putLong("timestamp",System.currentTimeMillis());




                SendToDataLayerThread sendToDLT = new SendToDataLayerThread(WEARABLE_DATA_PATH, dataMap, googleClient, asset);
                sendToDLT.start();
                sendBroadcast(false);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (buffreader != null) {
                    try {
                        buffreader.close();
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

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

    public void sendBroadcast(boolean bool) {
        Intent intent = new Intent("button");
        if (bool) {
            intent.putExtra("bool", true);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }else{
            intent.putExtra("bool", false);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    class SendToDataLayerThread extends Thread {
        GoogleApiClient googleClient;

        String path;
        DataMap dataMap;
        Asset asset;


        /**
         * Constructor for sending data objects to the data layer.
         * @param p path
         * @param data dataMap
         * @param googleClient google api client
         */
        SendToDataLayerThread(String p, DataMap data, GoogleApiClient googleClient, Asset asset) {
            path = p;
            dataMap = data;
            this.googleClient = googleClient;
            this.asset = asset;
        }

        /**
         * Construct a DataRequest and send over the data layer
         */
        public void run() {
            PutDataMapRequest putDMR = PutDataMapRequest.create(path);
            putDMR.getDataMap().putAll(dataMap);
            if (asset != null) {
                putDMR.getDataMap().putAsset("data", asset);
                System.out.println("Sending asset: " + asset.getUri());
            }
            //PutDataRequest request = putDMR.asPutDataRequest().setUrgent();
            PutDataRequest request = putDMR.asPutDataRequest();

            DataApi.DataItemResult result = Wearable.DataApi.putDataItem(googleClient, request).await();
            if (result.getStatus().isSuccess()) {
                Log.v("myTag", "DataMap: " + dataMap + " sent successfully to data layer ");
                File fileDelete = new File(dataMap.getString("path"), dataMap.getString("fileName"));
                fileDelete.delete();
            }
            else {
                // Log an error
                Log.v("myTag", "ERROR: failed to send DataMap to data layer");
            }
            Log.d(TAG,"End of the Service");
            stopSelf();
        }

    }




}