package com.example.alex.astm_facedetector;

//android imports
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.content.Context;
import android.widget.Toast;

//java imports
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

//opencv imports
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.objdetect.CascadeClassifier;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2
{

    private static final String TAG="MainActivity";
    private JavaCameraView javaCameraView;
    private Mat mRgba;
    private File mCascadeFile;
    private CascadeClassifier mJavaDetector;
    public float rotationValue;
    //face detector
    private FaceDetector fd;
    //Rotation sensors
    private static SensorManager sensorService;
    private Sensor sensor;

    BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this)
    {
        @Override
        public void onManagerConnected(int status)
        {
            switch (status)
            {
                case LoaderCallbackInterface.SUCCESS:
                {
                    try
                    {
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty())
                        {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        }
                        else
                        {
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
                            fd = new FaceDetector(mJavaDetector);
                        }
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    javaCameraView.enableView();
                    break;
                }
                default:
                {
                    super.onManagerConnected(status);
                    break;
                }
            }
            super.onManagerConnected(status);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //java camera view
        javaCameraView = (JavaCameraView)findViewById(R.id.java_camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);

        //allocate sensors
        sensorService = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorService.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        if (sensor != null)
        {
            sensorService.registerListener(mySensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            Log.i("Compass MainActivity", "Registerered for ORIENTATION Sensor");
        } else
        {
            Log.e("Compass MainActivity", "Registerered for ORIENTATION Sensor");
            Toast.makeText(this, "ORIENTATION Sensor not found", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private SensorEventListener mySensorEventListener = new SensorEventListener()
    {
        private float update(float[] vectors)
        {
            final int SENSOR_DELAY = 500 * 1000; //500ms
            final int FROM_RADS_TO_DEGS = -57;

            float[] rotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, vectors);
            int worldAxisX = SensorManager.AXIS_X;
            int worldAxisZ = SensorManager.AXIS_Z;
            float[] adjustedRotationMatrix = new float[9];
            SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisX, worldAxisZ, adjustedRotationMatrix);
            float[] orientation = new float[3];
            SensorManager.getOrientation(adjustedRotationMatrix, orientation);
            float pitch = orientation[1] * FROM_RADS_TO_DEGS;
            float roll = orientation[2] * FROM_RADS_TO_DEGS;

            return roll;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event)
        {
            if (event.values.length > 4)
            {
                float[] truncatedRotationVector = new float[4];
                System.arraycopy(event.values, 0, truncatedRotationVector, 0, 4);
                rotationValue = update(truncatedRotationVector);
            }
            else
            {
                rotationValue = update(event.values);
            }
        }
    };

    @Override
    protected void onPause()
    {
        super.onPause();
        if (javaCameraView != null)
        {
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (javaCameraView != null)
        {
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (OpenCVLoader.initDebug())
        {
            Log.d(TAG, "OpenCV loaded");
            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else
        {
            Log.d(TAG, "OpenCV not loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallBack);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height)
    {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped()
    {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame)
    {

        mRgba = inputFrame.rgba();
        fd.setFrame(mRgba);
        //rotationValue = 180;
        fd.setRotation(rotationValue);
        mRgba = fd.detectFaces();

        return mRgba;
    }
}