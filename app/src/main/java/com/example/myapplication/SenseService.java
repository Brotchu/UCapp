package com.example.myapplication;

import static android.content.Intent.getIntent;
import static android.content.Intent.getIntentOld;
import static android.content.Intent.parseIntent;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttp;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

@RequiresApi(api = Build.VERSION_CODES.O)
public class SenseService extends Service {

    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    private Context context;
    private HandlerThread thread;
    private boolean run;
    private String longitude;
    private String latitiude;
//    private Intent intent = getIntent();
    private String userid;

    private FusedLocationProviderClient fusedLocClient;

//    private final class LocationListener implements android.location.LocationListener{
//
//        @Override
//        public void onLocationChanged(@NonNull Location location) {
//            longitude= String.valueOf(location.getLongitude());
//            latitiude = String.valueOf(location.getLatitude());
//        }
//    };


    private final class ServiceHandler extends Handler implements SensorEventListener {
        private SensorManager sensorManager;
        private Sensor mLight;
        private LocalDateTime currTime;
        private String senseTime;
        private String FILENAME = "sensordata";
        private FileOutputStream fos;
        private final OkHttpClient client = new OkHttpClient();
        private final Integer workDuration = 2000;
        private final Integer sleepDuration = 5000;
        private final Integer cycleCount = 3;
        private Integer cycle = 0;
        private String dataAgg = "";
        private Integer count = 0;
        private double dataSum = 0.0;
//        private RequestQueue queue;
//        RequestQueue queue;


        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                currTime = java.time.LocalDateTime.now();
                String newFileName = currTime.toString() + FILENAME + ".csv";
                fos = openFileOutput(newFileName, Context.MODE_APPEND);
            } catch (FileNotFoundException e) {
                System.out.println("did not open file");
                e.printStackTrace();
            }
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            mLight = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            //todo: make a gps sensor
            try {
                do {
                    sensorManager.registerListener(this, mLight, 10000000);
                    //todo: register gps sensor
                    Thread.sleep(workDuration);
                    System.out.println("thread sleeping");
                    sensorManager.unregisterListener(this);
                    Thread.sleep(sleepDuration);
                    double avgData = dataSum / count;
                    dataAgg += java.time.LocalDateTime.now().toString() + "," + String.valueOf(avgData) + "\n";
                    count = 0;
                    dataSum = 0;
                    cycle += 1;
                    if (cycle == cycleCount) {
                        cycle = 0;
                        System.out.println("raise request here");
                        System.out.println("location data: " + latitiude + longitude);
                        //TODO: create a request to send this data to aws
                        MediaType JSON = MediaType.parse("application/json;charset=utf-8");
                        JSONObject sensorData = new JSONObject();
                        try {
                            sensorData.put("sensorData", dataAgg);
                            sensorData.put("userid", "Nithin");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        RequestBody reqbody = RequestBody.create(sensorData.toString(), JSON);
                        Request req = new Request.Builder()
                                .url("https://h1iyilg7u1.execute-api.us-east-2.amazonaws.com/dev")
                                .post(reqbody)
                                .build();
                        client.newCall(req).enqueue(new okhttp3.Callback() {
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                e.printStackTrace();
                            }

                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                if (response.isSuccessful()) {
                                    System.out.println("post success");
                                    //todo: empty dataAgg
                                    dataAgg = "";
                                } else {
                                    System.out.println("post failure");
                                }
                            }
                        });
                        //TODO: end of http request part
                        System.out.println("average data" + String.valueOf(avgData));
                        System.out.println(dataAgg);
                    }
                } while (run); //FIXME: previously while(true) --> while(true) was not stopping service
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            stopSelf(msg.arg1);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float lux = event.values[0];
            count += 1;
            dataSum += lux;
            String val = Float.toString(lux);
            senseTime = java.time.LocalDateTime.now().toString();
            System.out.println(Float.toString(lux));
            val = senseTime + "," + val + "\n";
//            dataAgg+= val;
            try {
                fos.write(val.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    @Override
    public void onCreate() {
//        fusedLocClient = LocationServices.getFusedLocationProviderClient(this);
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            System.out.println("no GPS permission");
//            return;
//        }
//        fusedLocClient.getLastLocation()
//                .addOnSuccessListener((Executor) this, new OnSuccessListener<Location>() {
//                    @Override
//                    public void onSuccess(Location location) {
//                        if(location != null ){
//                            latitiude= String.valueOf(location.getLatitude());
//                            longitude= String.valueOf(location.getLongitude());
//                        }
//                    }
//                });
//        Bundle extras = getIntent().getExtras();
        thread = new HandlerThread("ServiceStartArguments");
        thread.start();

        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        run= true;
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        serviceHandler.sendMessage(msg);
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
        System.out.println("Stopping service");
//        thread.quitSafely();
        thread.quit();
        run = false;
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
