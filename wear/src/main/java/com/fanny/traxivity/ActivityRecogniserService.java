package com.fanny.traxivity;


import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;


/**
 * Created by Sadiq on 01/03/2017.
 */

public class ActivityRecogniserService extends IntentService {

    private SharedPreferences settings;

    private final String TAG="RecogniserService";

    private static final String DATA_FOLDER= "/Traxivity/data";

    private File recordFile;

    private Handler handler;

    public ActivityRecogniserService(){
        super("ActivityRecogniserService");
    }

    public void onCreate(){
        super.onCreate();

        handler = new Handler(Looper.getMainLooper());
        initRecordFile();
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            DetectedActivity activity = result.getMostProbableActivity();
            String strActivity = getActivityName(activity.getType());
            //Log.d(TAG, "The activity is : " + strActivity);
            settings = PreferenceManager.getDefaultSharedPreferences(this);
            String tmps = settings.getString("activities","");
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("activities",tmps+Calendar.getInstance().getTime()+ " / " +strActivity+" \n");
            editor.commit();
            Log.d(TAG,settings.getString("activities",""));

            Context context = getApplicationContext();
            int duration = Toast.LENGTH_SHORT;
            DisplayToast toast = new DisplayToast(context, strActivity, duration);
            handler.post(toast);


            //Current time in milliseconds
            Long currentTime = System.currentTimeMillis();
            String output = currentTime + "," + strActivity + "\n";


            //Writting the activity in a .csv file

            if (recordFile != null) {

                FileWriter filewriter = null;
                try {
                    recordFile.createNewFile();
                    filewriter = new FileWriter(recordFile, true);
                    filewriter.write(output);
                    //Log.d(TAG,"The output was written into recordFile");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG,"Couldn't write the output into recordFile");
                }finally {
                    if (filewriter != null) {
                        try {
                            filewriter.close();
                        }catch(IOException e){
                            e.printStackTrace();
                        }
                    }
                }

            }


        }


    }

    private String getActivityName(int activity){
        switch(activity){
            case DetectedActivity.IN_VEHICLE: return "In Vehicle";
            case DetectedActivity.ON_BICYCLE: return "Cycling";
            case DetectedActivity.RUNNING: return "Running";
            case DetectedActivity.WALKING: return "Walking";
            case DetectedActivity.STILL: return "Inactive";
            case DetectedActivity.ON_FOOT: return "Walking";
            default: return "Unknown";
        }
    }

    private void initRecordFile(){
        if (isExternalStorageWritable()) {

            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
            String filename = df.format(c.getTime());


            //File myDir = new File(Environment.getExternalStorageDirectory() + DATA_FOLDER);
            File myDir = new File(getFilesDir() + DATA_FOLDER);

            Boolean success = true;

            if (!myDir.exists()) {
                success = myDir.mkdir();
            }

            if (success) {
                //File file = new File(Environment.getExternalStorageDirectory().getPath() + DATA_FOLDER, filename);
                recordFile = new File(myDir, filename);
                //Log.d(TAG,"recordFile was initialized");
            } else {
                Log.d(TAG,"recordFile couldn't be initialized");

            }

        } else {
            Log.d(TAG,"The external storage is not writable");

        }

    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }



}
