package fhaachen.snakegame;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Display;

import androidx.appcompat.app.AppCompatActivity;
import fhaachen.snakegame.ui.GameStage;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private GameStage gameStage;
    private SensorManager sensorManager;
    private Sensor accelerometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the pixel dimensions of the screen
        Display display = getWindowManager().getDefaultDisplay();

        // Initialize the result into a Point object
        Point size = new Point();
        display.getSize(size);

        // Create a game stage
        gameStage = new GameStage(this, size);

        // Set game stage as view
        setContentView(gameStage);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        //TODO: else Disable Gyros cope setting
    }

    @Override
    public void onBackPressed() {
        gameStage.showPauseDialog(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        gameStage.showPauseDialog(this);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        // Start game stage thread
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


}
