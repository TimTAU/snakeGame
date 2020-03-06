package fhaachen.snakegame

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import fhaachen.snakegame.ui.GameStage

class MainActivity : AppCompatActivity(), SensorEventListener, GestureDetector.OnGestureListener {
    private lateinit var gameStage: GameStage
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private lateinit var gestureDetector: GestureDetectorCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create a game stage
        gameStage = GameStage(this)
        // Set game stage as view
        setContentView(gameStage)

        //Sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        if (sensorManager != null) {
            accelerometer = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        } else {
            //Disable Tilt setting if accelerometer is not available
            val tiltSetting = findViewById<TextView>(R.id.control_tilt_button)
            if (tiltSetting != null) {
                tiltSetting.isClickable = false
            }
        }
        gestureDetector = GestureDetectorCompat(this, this)
    }

    override fun onBackPressed() {
        gameStage.showMenuDialog()
    }

    override fun onResume() {
        super.onResume()
        if (accelerometer != null) {
            sensorManager!!.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        }
        gameStage.resume()
    }

    override fun onPause() {
        super.onPause()
        // Stop game stage thread
        gameStage.pause()
        sensorManager!!.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        gameStage.onSensorChanged(event, accelerometer!!)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        //Do nothing
    }

    override fun onPointerCaptureChanged(hasCapture: Boolean) {
        //Do nothing
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun onDown(e: MotionEvent): Boolean {
        return false
    }

    override fun onShowPress(e: MotionEvent) {
        //Do nothing special
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return false
    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent) {
        //Do nothing special
    }

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        return gameStage.onFling(velocityX, velocityY)
    }

    fun onControlSelected(v: View) {
        gameStage.onControlSelected(v)
    }

    fun onThemeSelected(v: View) {
        gameStage.onThemeSelected(v)
    }
}