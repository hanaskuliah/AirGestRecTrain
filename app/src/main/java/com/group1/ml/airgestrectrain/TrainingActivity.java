package com.group1.ml.airgestrectrain;

import android.app.AlertDialog;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.opencsv.CSVWriter;


public class TrainingActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager = null;

    // angular speeds from gyro
    private float[] gyro = new float[3];

    // rotation matrix from gyro data
    private float[] gyroMatrix = new float[9];

    // orientation angles from gyro matrix
    private float[] gyroOrientation = new float[3];

    // magnetic field vector
    private float[] magnet = new float[3];

    // accelerometer vector
    private float[] accel = new float[3];

    // orientation angles from accel and magnet
    private float[] accMagOrientation = new float[3];

    // final orientation angles from sensor fusion
    private float[] fusedOrientation = new float[3];

    // accelerometer and magnetometer based rotation matrix
    private float[] rotationMatrix = new float[9];

    public static final float EPSILON = 0.000000001f;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private long timestamp;
    private boolean initState = true;

    public static final int TIME_CONSTANT = 50;
    public static final float FILTER_COEFFICIENT = 0.98f;
    private Timer fuseTimer = new Timer();

    // The following members are only for displaying the sensor output.
    public Handler mHandler;

    private int nTraining=0;

    private Button btnCancel;
    private ImageButton btnTrain;
    private EditText textName;
    private TextView textSuggest;

    public String name,a1,a2,a3,g1,g2,g3="";
    public boolean pressed1=false;
    public int berapa=0;

    DecimalFormat d = new DecimalFormat("#.##");
    private List<String[]> data = new ArrayList<String[]>();

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        gyroOrientation[0] = 0.0f;
        gyroOrientation[1] = 0.0f;
        gyroOrientation[2] = 0.0f;

        // initialise gyroMatrix with identity matrix
        gyroMatrix[0] = 1.0f; gyroMatrix[1] = 0.0f; gyroMatrix[2] = 0.0f;
        gyroMatrix[3] = 0.0f; gyroMatrix[4] = 1.0f; gyroMatrix[5] = 0.0f;
        gyroMatrix[6] = 0.0f; gyroMatrix[7] = 0.0f; gyroMatrix[8] = 1.0f;

        // get sensorManager and initialise sensor listeners
        mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        initListeners();

        // wait for one second until gyroscope and magnetometer/accelerometer
        // data is initialised then scedule the complementary filter task
        fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(),
                1000, TIME_CONSTANT);

        mHandler = new Handler();
        btnTrain=(ImageButton)findViewById(R.id.btnTrain);

        toolbar = (Toolbar) findViewById(R.id.tool_bar); // Attaching the layout to the toolbar object
       setSupportActionBar(toolbar);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        android.view.ViewGroup.LayoutParams params = btnTrain.getLayoutParams();
        params.height = width-20;
        params.width = width-20;
        btnTrain.setLayoutParams(params);


        btnCancel=(Button)findViewById(R.id.cancelBtn);
        textName=(EditText)findViewById(R.id.nameTxtBox);
        textSuggest=(TextView)findViewById(R.id.suggestTxt);

        d.setRoundingMode(RoundingMode.HALF_UP);
        d.setMaximumFractionDigits(3);
        d.setMinimumFractionDigits(3);

        btnTrain.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {


                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    btnTrain.setImageResource(0);
                    if(textName.getText().length()>0){
                        pressed1=true;
                        nTraining++;

                    }
                    else{
//                        AlertDialog alertDialog = new AlertDialog.Builder(TrainingActivity.this).create();
//                        alertDialog.setTitle("Alert");
//                        alertDialog.setMessage("please fill your name");
//                        alertDialog.show();
                    }
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    btnTrain.setImageResource(R.drawable.fingerprint);
                    pressed1=false;
                    if(nTraining>10) {
                        btnTrain.setEnabled(false);
                    }
                    else if(nTraining>0){
                        textSuggest.setText("training number:"+nTraining);
                        name=textName.getText().toString();
                        String csv = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
                        String fileName = name+nTraining+".csv";

                        String filePath = csv + File.separator + fileName;
                        Toast.makeText(getApplicationContext(),"nama file "+filePath,Toast.LENGTH_SHORT).show();
                        System.out.println("nama filenya "+filePath);
                        CSVWriter writer = null;

                        try {
                            writer = new CSVWriter(new FileWriter(filePath));
                            writer.writeAll(data);
                            writer.close();
                            AlertDialog alertDialog1 = new AlertDialog.Builder(TrainingActivity.this).create();
                            alertDialog1.setTitle("Alert");
                            alertDialog1.setMessage("Data Saved");
                            alertDialog1.show();
                            data.clear();
                        } catch (IOException e) {
                            e.printStackTrace();
                            AlertDialog alertDialog2 = new AlertDialog.Builder(TrainingActivity.this).create();
                            alertDialog2.setTitle("Alert");
                            alertDialog2.setMessage("Data unsaved");
                            alertDialog2.show();
                        }
                    }
                }
                return false;
            }
        });

    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        switch(event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                // copy new accelerometer data into accel array and calculate orientation
                System.arraycopy(event.values, 0, accel, 0, 3);
                calculateAccMagOrientation();
                break;

            case Sensor.TYPE_GYROSCOPE:
                // process gyro data
                gyroFunction(event);
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                // copy new magnetometer data into magnet array
                System.arraycopy(event.values, 0, magnet, 0, 3);
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void refreshData() {
        nTraining=0;
        btnTrain.setEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement


        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStop() {
        super.onStop();
        // unregister sensor listeners to prevent the activity from draining the device's battery.
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // unregister sensor listeners to prevent the activity from draining the device's battery.
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // restore the sensor listeners when user resumes the application.
        initListeners();
    }

    // This function registers sensor listeners for the accelerometer, magnetometer and gyroscope.
    public void initListeners(){
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_FASTEST);
    }


    // calculates orientation angles from accelerometer and magnetometer output
    public void calculateAccMagOrientation() {
        if(SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
            SensorManager.getOrientation(rotationMatrix, accMagOrientation);
        }
    }

    // This function is borrowed from the Android reference
    // at http://developer.android.com/reference/android/hardware/SensorEvent.html#values
    // It calculates a rotation vector from the gyroscope angular speed values.
    private void getRotationVectorFromGyro(float[] gyroValues,
                                           float[] deltaRotationVector,
                                           float timeFactor)
    {
        float[] normValues = new float[3];

        // Calculate the angular speed of the sample
        float omegaMagnitude =
                (float)Math.sqrt(gyroValues[0] * gyroValues[0] +
                        gyroValues[1] * gyroValues[1] +
                        gyroValues[2] * gyroValues[2]);

        // Normalize the rotation vector if it's big enough to get the axis
        if(omegaMagnitude > EPSILON) {
            normValues[0] = gyroValues[0] / omegaMagnitude;
            normValues[1] = gyroValues[1] / omegaMagnitude;
            normValues[2] = gyroValues[2] / omegaMagnitude;
        }

        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        float thetaOverTwo = omegaMagnitude * timeFactor;
        float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
        float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
        deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
        deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
        deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
        deltaRotationVector[3] = cosThetaOverTwo;
    }

    // This function performs the integration of the gyroscope data.
    // It writes the gyroscope based orientation into gyroOrientation.
    public void gyroFunction(SensorEvent event) {
        // don't start until first accelerometer/magnetometer orientation has been acquired
        if (accMagOrientation == null)
            return;

        // initialisation of the gyroscope based rotation matrix
        if(initState) {
            float[] initMatrix = new float[9];
            initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
            float[] test = new float[3];
            SensorManager.getOrientation(initMatrix, test);
            gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix);
            initState = false;
        }

        // copy the new gyro values into the gyro array
        // convert the raw gyro data into a rotation vector
        float[] deltaVector = new float[4];
        if(timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            System.arraycopy(event.values, 0, gyro, 0, 3);
            getRotationVectorFromGyro(gyro, deltaVector, dT / 2.0f);
        }

        // measurement done, save current time for next interval
        timestamp = event.timestamp;

        // convert rotation vector into rotation matrix
        float[] deltaMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);

        // apply the new rotation interval on the gyroscope based rotation matrix
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);

        // get the gyroscope based orientation from the rotation matrix
        SensorManager.getOrientation(gyroMatrix, gyroOrientation);
    }

    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];

        float sinX = (float)Math.sin(o[1]);
        float cosX = (float)Math.cos(o[1]);
        float sinY = (float)Math.sin(o[2]);
        float cosY = (float)Math.cos(o[2]);
        float sinZ = (float)Math.sin(o[0]);
        float cosZ = (float)Math.cos(o[0]);

        // rotation about x-axis (pitch)
        xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
        xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
        xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;

        // rotation about y-axis (roll)
        yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
        yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
        yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;

        // rotation about z-axis (azimuth)
        zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
        zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
        zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;

        // rotation order is y, x, z (roll, pitch, azimuth)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }

    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];

        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

        return result;
    }

    class calculateFusedOrientationTask extends TimerTask {
        public void run() {
            float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;

            // update sensor output in GUI
            mHandler.post(updateOreintationDisplayTask);
        }
    }


    // **************************** GUI FUNCTIONS *********************************


    public void updateOreintationDisplay() {
        if(pressed1) {
            berapa++;
            a1=""+d.format(accMagOrientation[0] * 180 / Math.PI);
            a2=""+d.format(accMagOrientation[1] * 180 / Math.PI);
            a3=""+d.format(accMagOrientation[2] * 180 / Math.PI);
            g1=""+d.format(gyroOrientation[0] * 180 / Math.PI);
            g2=""+d.format(gyroOrientation[1] * 180 / Math.PI);
            g3=""+d.format(gyroOrientation[2] * 180 / Math.PI);
            data.add(new String[]{a1, a2, a3, g1, g2, g3});
        }
    }

    private Runnable updateOreintationDisplayTask = new Runnable() {
        public void run() {
            updateOreintationDisplay();

        }
    };
}
