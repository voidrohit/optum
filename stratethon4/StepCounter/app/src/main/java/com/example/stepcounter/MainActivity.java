package com.example.stepcounter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;

import static java.math.BigDecimal.*;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // experimental values for hi and lo magnitude limits
    private final double HI_STEP = 11.0;     // upper mag limit
    private final double LO_STEP = 8.0;      // lower mag limit
    boolean highLimit = false;      // detect high limit
    int counter = 0;                // step counter

    TextView tvx, tvy, tvz, tvMag, tvSteps;
    Button button;
    private SensorManager mSensorManager;
    private Sensor mSensor;

    FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvx = findViewById(R.id.tvX);
        tvy = findViewById(R.id.tvY);
        tvz = findViewById(R.id.tvZ);
        tvMag = findViewById(R.id.tvMag);
        tvSteps = findViewById(R.id.tvSteps);
        button = findViewById(R.id.button);
        // we are going to use the sensor service

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        
        button.setOnClickListener(v->{
            updateData();
        });

    }

    private void updateData() {
        long timeStamp = System.currentTimeMillis();
        HashMap<String,Object> map = new HashMap<>();
        map.put("timestamp",timeStamp);
        map.put("steps",counter);
        map.put("distance",counter*0.0005);
        map.put("calories",counter*0.04);
        map.put("heart_rate",80);
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseFirestore.collection("Data").document(timeStamp+"").set(map)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(MainActivity.this, "Uploaded Successfully", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Failed to upload given data.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    /*
     * When the app is brought to the foreground - using app on screen
     */
    protected void onResume() {
        super.onResume();
        // turn on the sensor
        mSensorManager.registerListener(this, mSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    /*
     * App running but not on screen - in the background
     */
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);    // turn off listener to save power
    }


    @Override
    public void onSensorChanged(SensorEvent event) {

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];


        /*tvy.setText(String.valueOf(x*0.04));*/
        tvz.setText(String.valueOf(z));

        // get a magnitude number using Pythagorus's Theorem
        double mag = round(Math.sqrt((x*x) + (y*y) + (z*z)), 2);
        tvMag.setText(String.valueOf(mag));

        // for me! if msg > 11 and then drops below 9, we have a step
        // you need to do your own mag calculating
        if ((mag > HI_STEP) && (highLimit == false)) {
            highLimit = true;
        }
        if ((mag < LO_STEP) && (highLimit == true)) {
            // we have a step
            counter++;
            tvx.setText(String.valueOf(counter*0.04));
            tvy.setText(String.valueOf(counter*0.0005)+" miles");
            tvSteps.setText(String.valueOf(counter));
            highLimit = false;
        }


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not used
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
}