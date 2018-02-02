package com.example.alex.astm_facedetector;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Toast;

public class SensorMonitor extends Activity implements SensorEventListener
{

    private SensorManager mSensorManager;
    private Sensor mRotationSensor;
    private long lastUpdate;
    public float rotationValue;

    private static final int SENSOR_DELAY = 500 * 1000; //500ms

    //Create activity
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try
        {
            mSensorManager = (SensorManager)getSystemService(Activity.SENSOR_SERVICE);
            mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            mSensorManager.registerListener(this, mRotationSensor, SENSOR_DELAY);
            rotationValue = 90;
            lastUpdate = System.currentTimeMillis();
        }
        catch (Exception e)
        {
            Toast.makeText(this, "Hardware compat issue", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        // TODO Auto-generated method stub
    }


    @Override
    public void onSensorChanged(SensorEvent event)
    {
        //stub
    }


    @Override
    protected void onResume() {
        super.onResume();
        //register listener
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        // unregister listener
        super.onPause();
        mSensorManager.unregisterListener(this);
    }
}