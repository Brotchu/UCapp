package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.w3c.dom.Text;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.*;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button start, stop, switchButton;
    private SensorManager sensorManager;
    private Sensor mLight, mStep;
    private Intent serviceIntent;
    private EditText userid;
    private Integer maxView= 20;

    private boolean graphFlag = false;

//    private boolean serviceState = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //get sensor data
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> deviceSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        mLight = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mStep = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
//        String string = string.toString()
        int i;
        String outString = "";
        for (i = 0; i< deviceSensors.size(); i++)
        {
            outString += deviceSensors.get(i).getName()+"\n";
        }
        outString += mLight.getName() + "\n" + mStep.getName();
        System.out.println(outString);
//        StringBuilder sb = new StringBuilder();



//        TextView outText = (TextView) findViewById(R.id.textView);
//        outText.setText(outString);

        serviceIntent = new Intent(this, SenseService.class);
//        outText.getText().toString();
        start = (Button) findViewById(R.id.buttonStart);
        stop = (Button) findViewById(R.id.buttonStop);
        switchButton= (Button) findViewById(R.id.switchButton);
        stop.setEnabled(false);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);
        switchButton.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        if (v==start) {

            userid = (EditText)findViewById(R.id.userid);
            if (userid.length()==0){
                Toast.makeText(this, "need a user name", Toast.LENGTH_SHORT).show();
                return;
            }

            stop.setEnabled(true);
            start.setEnabled(false);

            //get request

            //TODO: start get request
            OkHttpClient client = new OkHttpClient();
//            https://h1iyilg7u1.execute-api.us-east-2.amazonaws.com/dev
            SimpleDateFormat dtFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date();
            String DateString= dtFormat.format(date);
            System.out.println("Date -- " + DateString);
//            String url = "https://h1iyilg7u1.execute-api.us-east-2.amazonaws.com/dev?userid=Nithin&date=2021-11-27";
            String url = "https://h1iyilg7u1.execute-api.us-east-2.amazonaws.com/dev?userid=Nithin&date=" + DateString;
            Request request= new Request.Builder()
                    .url(url)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()){
                        String myresponse = response.body().string();
                        System.out.println("REQUEST RESULT"+myresponse);
                        try {
                            JSONObject obj = new JSONObject(myresponse);
                            TextView sunrise = (TextView)findViewById(R.id.sunrise);
                            long unixSunrise = Long.parseLong(obj.getString("sunrise"));
                            java.util.Date time = new java.util.Date(unixSunrise*1000);
                            sunrise.setText(new SimpleDateFormat("HH").format(time) +":"+ new SimpleDateFormat("mm").format(time));

                            TextView sunset = (TextView) findViewById(R.id.sunset);
                            long unixSunset = Long.parseLong(obj.getString("sunset"));
                            java.util.Date sunsettime = new java.util.Date(unixSunset*1000);
                            sunset.setText(new SimpleDateFormat("HH").format(sunsettime) +":"+ new SimpleDateFormat("mm").format(sunsettime));

                            int sunExp = Integer.parseInt(obj.getString("sunExposure"));
                            int uvExp = Integer.parseInt(obj.getString("uvExposure"));
                            float uvLevel = Float.parseFloat(obj.getString("uv"));

                            TextView SunLevel = (TextView) findViewById(R.id.sunprogress);
                            TextView UVLevel = (TextView) findViewById(R.id.uvprogress);
                            TextView UVIndicator = (TextView) findViewById(R.id.uvIndicator);
                            ProgressBar sunProgressBar = (ProgressBar) findViewById(R.id.sunprogressbar);
                            ProgressBar uvProgressBar = (ProgressBar) findViewById(R.id.uvprogressbar);

                            System.out.println("all good till here");
                            int sunMinute = sunExp / 60;
                            int sunSecond = sunExp%60;
                            int uvMinute = uvExp /60;
                            int uvSecond = uvExp %60;
                            UVIndicator.setText("Current UV Index "+ String.valueOf(uvLevel));
                            SunLevel.setText("Sun Exposure "+String.valueOf(sunMinute)+"mins "+String.valueOf(sunSecond)+"s");
                            UVLevel.setText("UV Exposure "+String.valueOf(uvMinute)+"mins "+String.valueOf(uvSecond)+"s");
                            sunProgressBar.setProgress((sunExp * 100)/(20*60));
                            uvProgressBar.setProgress((uvExp*100)/ (15*60) );

                            //setting sun position
                            long currentTime = System.currentTimeMillis()/1000;
                            ProgressBar sunPosBar = (ProgressBar) findViewById(R.id.sunPosition);
                            System.out.println("TIMES: "+ String.valueOf(currentTime) + " - "+ String.valueOf(unixSunrise)+" to "+ String.valueOf(unixSunset));
                            if (currentTime < unixSunrise){
//                                sunPosBar.set
                                sunPosBar.setProgress(0);
                            } else if (currentTime > unixSunset){
                                sunPosBar.setProgress(50);
                            }else {
                                long p =( (currentTime - unixSunrise) *100)/(unixSunset - unixSunrise);
                                System.out.println("SUN POSITION VALUE:" + String.valueOf(p));
                                sunPosBar.setProgress((int)(p/2));
                            }


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            //display on screen
            //TODO: end of get request

            serviceIntent.putExtra("userid", userid.getText().toString());
            startService( serviceIntent);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else if (v==stop) {
            stopService( serviceIntent);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            start.setEnabled(true);
            stop.setEnabled(false);

            //TEST

            //TEST TODO:
//            String urlString="https://brotchu.github.io/";
//            Intent testIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
//            testIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////            testIntent.setPackage("com.android.chrome");
//            startActivity(testIntent);
        } else if (v ==switchButton){
            userid = (EditText)findViewById(R.id.userid);
            if (userid.length()==0){
                Toast.makeText(this, "need a user name", Toast.LENGTH_SHORT).show();
                return;
            }
            LinearLayout top = (LinearLayout) findViewById(R.id.linearLayout);
            TextView sunText = (TextView) findViewById(R.id.sunprogress);
            TextView uvText = (TextView) findViewById(R.id.uvprogress);
            TextView uvVal = (TextView) findViewById(R.id.uvIndicator);
            ProgressBar sunBar = (ProgressBar) findViewById(R.id.sunprogressbar);
            ProgressBar uvBar = (ProgressBar) findViewById(R.id.uvprogressbar);
            GraphView graphview = (GraphView) findViewById(R.id.GraphView);
            if (!graphFlag) {
                graphFlag = true;
                top.setVisibility(View.GONE);
                sunText.setVisibility(View.GONE);
                uvText.setVisibility(View.GONE);
                uvVal.setVisibility(View.GONE);
                sunBar.setVisibility(View.GONE);
                uvBar.setVisibility(View.GONE);
                graphview.setVisibility(View.VISIBLE);

                graphview.setTitle("light exposure");
                graphview.setTitleTextSize(50);
//                graphview.setTitleColor(R.);

                LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(new DataPoint[]{});
//                LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(new DataPoint[]{});
                //TODO: make get request to get last 10 values from history, add in below series
                OkHttpClient client = new OkHttpClient();
                SimpleDateFormat dtFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date date = new Date();
                String DateString= dtFormat.format(date);
                System.out.println("Date -- " + DateString);
//                String url = "https://h1iyilg7u1.execute-api.us-east-2.amazonaws.com/dev?userid=Nithin&date=" + DateString;
//                ArrayList<DataPoint> datapoints=new ArrayList<DataPoint>();
                String url ="https://h1iyilg7u1.execute-api.us-east-2.amazonaws.com/dev/chart?userid=Nithin&date=" + DateString;

                Request requestGraph= new Request.Builder()
                        .url(url)
                        .build();
                client.newCall(requestGraph).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if(response.isSuccessful()){
                            String myresponse = response.body().string();
                            try {
                                JSONArray respObj = new JSONArray(myresponse);
//                                ArrayList<Object> valList = new ArrayList<Object>();


                                if (respObj != null){
                                    for(int i=0;i<respObj.length();i++){
//                                        valList.add(respObj.get(i));
                                        JSONObject entry = new JSONObject(respObj.get(i).toString());
                                        System.out.println(entry.getString("time") + "--" + entry.getString("val"));
                                        int timeEntry = Integer.parseInt(entry.getString("time"));
                                        maxView = timeEntry;
                                        float valEntry = Float.parseFloat(entry.getString("val"));
                                        series.appendData(
                                                new DataPoint(timeEntry,valEntry),
                                                true,
                                                20
                                        );
                                    }
                                    System.out.println("response list");
//                                    System.out.println(valList);
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
//                client.newCall(requestGraph).enqueue();
                //TODO: end of get request
//                DataPoint[] dataArray = new Datapoint[datapoints.size()];
//                LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(datapoints.toArray(dataArray);
                graphview.getViewport().setMaxX(maxView);
                graphview.getViewport().setMinY(0.0);
                graphview.getGridLabelRenderer().setPadding(10);
                graphview.getViewport().setMaxXAxisSize(maxView);
                graphview.getViewport().setScrollable(true);
                graphview.getViewport().setXAxisBoundsManual(true);
                graphview.addSeries(series);
            } else {
                graphFlag = false;
                top.setVisibility(View.VISIBLE);
                sunText.setVisibility(View.VISIBLE);
                uvText.setVisibility(View.VISIBLE);
                uvVal.setVisibility(View.VISIBLE);
                sunBar.setVisibility(View.VISIBLE);
                uvBar.setVisibility(View.VISIBLE);
                graphview.setVisibility(View.GONE);
            }
        }
    }

}