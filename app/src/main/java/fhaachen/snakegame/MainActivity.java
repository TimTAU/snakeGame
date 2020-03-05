package fhaachen.snakegame;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import fhaachen.snakegame.ui.GameStage;

public class MainActivity extends AppCompatActivity implements SensorEventListener, GestureDetector.OnGestureListener {
    private GameStage gameStage;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private GestureDetectorCompat gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a game stage
        gameStage = new GameStage(this);

        // Set game stage as view
        setContentView(gameStage);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        } else {
            //Disable Tilt setting if accelerometer is not available
            TextView tiltSetting = findViewById(R.id.control_tilt_button);
            if (tiltSetting != null) {
                tiltSetting.setClickable(false);
            }
        }

        gestureDetector = new GestureDetectorCompat(this, this);
    }

    @Override
    public void onBackPressed() {
        gameStage.showPauseDialog();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        gameStage.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop game stage thread
        gameStage.pause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        gameStage.onSensorChanged(event, accelerometer);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Do nothing
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        //Do nothing
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        //Do nothing special
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        //Do nothing special
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return gameStage.onFling(velocityX, velocityY);
    }

    public void onControlSelected(View v) {
        gameStage.onControlSelected(v);
    }

    public void onThemeSelected(View v) {
        gameStage.onThemeSelected(v);
    }
}