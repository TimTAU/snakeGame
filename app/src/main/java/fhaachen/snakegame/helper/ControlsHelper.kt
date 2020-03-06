package fhaachen.snakegame.helper

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.view.MotionEvent
import android.view.Surface
import fhaachen.snakegame.model.Controls
import fhaachen.snakegame.model.Snake
import kotlin.math.abs
import kotlin.math.roundToInt

object ControlsHelper {
    /**
     * Method for button control
     *
     * @param motionEvent event from touch
     * @return true if game needs to be started or a different control mode is selected
     */
    fun onTouchEvent(motionEvent: MotionEvent, controlMode: Controls.Mode, isPlaying: Boolean, controls: Controls?, snake: Snake): Boolean {
        if (motionEvent.action == MotionEvent.ACTION_DOWN) {
            if (controlMode === Controls.Mode.BUTTONS && isPlaying) {
                val posX = motionEvent.x.roundToInt()
                val posY = motionEvent.y.roundToInt()
                when {
                    controls!!.getButton(Controls.Button.LEFT)!!.contains(posX, posY) -> {
                        snake.setDirectionLeft()
                    }
                    controls.getButton(Controls.Button.UP)!!.contains(posX, posY) -> {
                        snake.setDirectionUp()
                    }
                    controls.getButton(Controls.Button.RIGHT)!!.contains(posX, posY) -> {
                        snake.setDirectionRight()
                    }
                    controls.getButton(Controls.Button.DOWN)!!.contains(posX, posY) -> {
                        snake.setDirectionDown()
                    }
                }
            } else {
                return true
            }
        }
        return false
    }

    /**
     * Method for tilt control
     *
     * @param event         event from the sensor
     * @param accelerometer Sensor that fired the event
     */
    fun onSensorChanged(event: SensorEvent, accelerometer: Sensor, controlMode: Controls.Mode, screenRotation: Int, snake: Snake) {
        if (event.sensor == accelerometer) {
            if (controlMode === Controls.Mode.TILT) {
                val x = event.values[0].roundToInt()
                val y = event.values[1].roundToInt()
                val xStrongerThanY = abs(x) > abs(y)
                @Suppress("SameParameterValue")
                when (screenRotation) {
                    Surface.ROTATION_0 ->
                        //LEFT  : +x
                        //UP    : -y
                        //RIGHT : -x
                        //DOWN  : +y
                        if (xStrongerThanY) {
                            if (x > 0) {
                                snake.setDirectionLeft()
                            } else {
                                snake.setDirectionRight()
                            }
                        } else {
                            if (y > 0) {
                                snake.setDirectionDown()
                            } else {
                                snake.setDirectionUp()
                            }
                        }
                    Surface.ROTATION_90 ->
                        //LEFT  : -y
                        //UP    : -x
                        //RIGHT : +y
                        //DOWN  : +x
                        if (xStrongerThanY) {
                            if (x > 0) {
                                snake.setDirectionDown()
                            } else {
                                snake.setDirectionUp()
                            }
                        } else {
                            if (y > 0) {
                                snake.setDirectionRight()
                            } else {
                                snake.setDirectionLeft()
                            }
                        }
                    Surface.ROTATION_180 ->
                        //LEFT  : -x
                        //UP    : +y
                        //RIGHT : +x
                        //DOWN  : -y
                        if (xStrongerThanY) {
                            if (x > 0) {
                                snake.setDirectionRight()
                            } else {
                                snake.setDirectionLeft()
                            }
                        } else {
                            if (y > 0) {
                                snake.setDirectionUp()
                            } else {
                                snake.setDirectionDown()
                            }
                        }
                    Surface.ROTATION_270 ->
                        //LEFT  : +y
                        //UP    : +x
                        //RIGHT : -y
                        //DOWN  : -x
                        if (xStrongerThanY) {
                            if (x > 0) {
                                snake.setDirectionUp()
                            } else {
                                snake.setDirectionDown()
                            }
                        } else {
                            if (y > 0) {
                                snake.setDirectionLeft()
                            } else {
                                snake.setDirectionRight()
                            }
                        }
                }
            }
        }
    }

    /**
     * Method for fling/gesture control
     *
     * @param velocityX range swiped on x axis
     * @param velocityY range swiped on y axis
     * @return true when event is consumed
     */
    fun onFling(velocityX: Float, velocityY: Float, controlMode: Controls.Mode, isPlaying: Boolean, snake: Snake): Boolean {
        if (controlMode === Controls.Mode.GESTURES && isPlaying) {
            if (abs(velocityX) > abs(velocityY)) {
                if (velocityX > 0) {
                    snake.setDirectionRight()
                } else {
                    snake.setDirectionLeft()
                }
            } else {
                if (velocityY > 0) {
                    snake.setDirectionDown()
                } else {
                    snake.setDirectionUp()
                }
            }
        } else {
            return false
        }
        return true
    }
}