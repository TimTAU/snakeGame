package fhaachen.snakegame.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.content.pm.ActivityInfo
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import fhaachen.snakegame.R
import fhaachen.snakegame.model.Controls
import fhaachen.snakegame.model.Snake
import fhaachen.snakegame.model.Theme
import java.util.*
import java.util.stream.IntStream
import kotlin.math.abs
import kotlin.math.roundToInt

class GameStage
constructor(context: Context?) : SurfaceView(context), Runnable, DialogInterface.OnDismissListener {
    private var numBlocksWide = 40
    private var fps: Long = 7
    private val surfaceHolder: SurfaceHolder
    private val paint: Paint
    private var thread: Thread? = null

    @Volatile
    private var isRunning = false

    @Volatile
    private var isPlaying = false
    private var pauseMenuShown = false
    private val screenX: Int
    private val screenY: Int
    private val snakeBlockSize: Int
    private val numBlocksHigh: Int
    private var nextFrameTime: Long
    private val maxBlocksOnScreen: Int
    private val screen: Display
    private var controlMode: Controls.Mode? = null
    private val activity: AppCompatActivity = getContext() as AppCompatActivity
    private val sharedPref: SharedPreferences
    private val sharedPreferencesEditor: Editor
    private var snake: Snake? = null
    private var controls: Controls? = null
    private val food: Rect
    private var score = 0

    //Resource strings
    private val menuTitle: String
    private val currentScoreMsg: String

    //Colors
    private var scoreTextColor = 0
    private val snakeColor: Int
    private val foodColor: Int
    private val controllersColor: Int

    //Bitmaps
    private var backgroundBitmap: Bitmap? = null
    private var foodBitmap: Bitmap? = null
    private var snakeHeadBitmap: Bitmap? = null
    private var snakeBodyBitmap: Bitmap? = null
    override fun run() {
        while (isRunning) {
            if (updateRequired()) {
                if (isPlaying) {
                    update()
                }
                draw()
            }
        }
    }

    /**
     * Pauses the game
     */
    fun pause() {
        isRunning = false
        try {
            thread!!.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    /**
     * Resumes the game after counter hits zero
     */
    fun resume() {
        isRunning = true
        thread = Thread(this)
        thread!!.start()
    }

    @SuppressLint("InflateParams")
    fun showMenuDialog() {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        pauseMenuShown = true
        val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.ScoreTheme))
        // Inflate and set the layout for the dialog
        val view = activity.layoutInflater.inflate(R.layout.pause_menu, null)
        builder.setView(view)
                .setTitle(menuTitle)
                .setPositiveButton(R.string.button_play) { _: DialogInterface?, _: Int -> startGameAndClosePauseMenu() }
                .setNegativeButton(R.string.button_exit) { _: DialogInterface?, _: Int -> activity.finishAndRemoveTask() }
                .setOnDismissListener(this)
        //Scores
        val lastScore = view.findViewById<TextView>(R.id.your_score)
        val highScore = view.findViewById<TextView>(R.id.highscore)
        lastScore.text = getSharedPreference(R.string.save_lastscore, 0).toString()
        highScore.text = getSharedPreference(R.string.save_highscore, 0).toString()
        //Set theme radioButton
        val themeRadioButton: RadioButton
        val theme = Theme.valueOf(getSharedPreference(R.string.setting_theme, Theme.GRASS.toString())!!)
        themeRadioButton = when (theme) {
            Theme.WATER -> view.findViewById(R.id.theme_water_button)
            else -> view.findViewById(R.id.theme_grass_button)
        }
        runOnUiThread(Runnable { themeRadioButton.isChecked = true })
        val controlModeRadioButton: RadioButton
        val controlMode = Controls.Mode.valueOf(getSharedPreference(R.string.setting_control, Controls.Mode.GESTURES.toString())!!)
        controlModeRadioButton = when (controlMode) {
            Controls.Mode.BUTTONS -> view.findViewById(R.id.control_buttons_button)
            Controls.Mode.TILT -> view.findViewById(R.id.control_tilt_button)
            else -> view.findViewById(R.id.control_swype_button)
        }
        runOnUiThread(Runnable { controlModeRadioButton.isChecked = true })
        runOnUiThread(Runnable {
            builder.create()
            builder.show()
        })
    }

    override fun onDismiss(dialog: DialogInterface) {
        startGameAndClosePauseMenu()
    }

    private fun startGameAndClosePauseMenu() {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        startGame()
        pauseMenuShown = false
        isRunning = true
    }

    /**
     * Sets all variables needed for game start
     */
    private fun startGame() {
        if (!isPlaying) {
            snake = Snake(
                    numBlocksWide / 2,
                    numBlocksHigh / 2,
                    maxBlocksOnScreen)
            spawnFood()
            score = 0
            nextFrameTime = System.currentTimeMillis()
            isPlaying = true
            fps = 7
        }
    }

    /**
     * Spawns food at a random location but not inside the snake
     */
    private fun spawnFood() {
        val random = Random()
        var randomX: Int
        var randomY: Int
        do {
            randomX = random.nextInt(numBlocksWide - 1) + 1
            randomY = random.nextInt(numBlocksHigh - 1) + 1
        } while (positionInsideSnake(randomX, randomY))
        val x = randomX * snakeBlockSize
        val y = randomY * snakeBlockSize
        food[x, y, x + snakeBlockSize] = y + snakeBlockSize
    }

    /**
     * Checks if the given coordinates are included in the snake
     *
     * @param x coordinate to be checked
     * @param y coordinate to be checked
     * @return true if the coordinate is included
     */
    private fun positionInsideSnake(x: Int, y: Int): Boolean {
        return IntStream.of(*snake!!.bodyXs).anyMatch { snakeX: Int -> snakeX == x } && IntStream.of(*snake!!.bodyYs).anyMatch { snakeY: Int -> snakeY == y }
    }

    /**
     * Consumes food and spawns new
     */
    private fun eatFood() {
        score++
        if (score < maxBlocksOnScreen - 1) {
            spawnFood()
            snake!!.increaseSize()
            if (score != 0 && score % 4 == 0 && fps <= 20) {
                fps++
            }
        } else {
            isPlaying = false
        }
    }

    /**
     * Evaluates if a screen update is required
     *
     * @return true if update is required
     */
    private fun updateRequired(): Boolean {
        if (!pauseMenuShown && nextFrameTime <= System.currentTimeMillis()) {
            nextFrameTime = System.currentTimeMillis() + 1000 / fps
            return true
        }
        return false
    }

    /**
     * Updates the game state
     */
    private fun update() {
        if (snake!!.headX * snakeBlockSize == food.left && snake!!.headY * snakeBlockSize == food.top) {
            eatFood()
        }
        snake!!.moveSnake()
        if (detectDeath()) {
            saveScores(score)
            isPlaying = false
            snake = null
        }
    }

    /**
     * Evaluates if the player is dead
     *
     * @return true if death is detected
     */
    private fun detectDeath(): Boolean { // Hit the screen edge
        if (snake!!.headX == -1 || snake!!.headX >= numBlocksWide + 1 || snake!!.headY == -1 || snake!!.headY == numBlocksHigh + 1) {
            return true
        }
        // Hit itself
        for (i in snake!!.snakeLength downTo 1) {
            if (i > 4
                    && snake!!.headX == snake!!.getBodyX(i)
                    && snake!!.headY == snake!!.getBodyY(i)) {
                return true
            }
        }
        // Hit nothing
        return false
    }

    /**
     * Draws the game field
     */
    private fun draw() {
        if (surfaceHolder.surface.isValid) {
            val canvas = surfaceHolder.lockCanvas()
            // Set background image
            canvas.drawBitmap(backgroundBitmap!!, null, RectF(0F, 0F, screenX.toFloat(), screenY.toFloat()), null)
            if (isPlaying && !pauseMenuShown) {
                drawGame(canvas, paint)
            } else {
                showMenuDialog()
            }
            surfaceHolder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawGame(canvas: Canvas, paint: Paint) { // Set controls color
        paint.color = controllersColor
        // Draw controls if needed
        if (controlMode === Controls.Mode.BUTTONS && controls != null) {
            for (control in controls!!.buttons) {
                canvas.drawRect(
                        control!!.left.toFloat(),
                        control.top.toFloat(),
                        control.right.toFloat(),
                        control.bottom.toFloat(),
                        paint)
            }
        }
        // Draw food
        paint.color = foodColor
        val foodRect = Rect(food.left, food.top, food.right, food.bottom)
        canvas.drawBitmap(foodBitmap!!, null, foodRect, paint)
        // Set snake color
        paint.color = snakeColor
        // Draw the snake
        for (i in 0 until snake!!.snakeLength + 1) {
            val snakeRect = Rect(snake!!.getBodyX(i) * snakeBlockSize,
                    snake!!.getBodyY(i) * snakeBlockSize,
                    snake!!.getBodyX(i) * snakeBlockSize + snakeBlockSize,
                    snake!!.getBodyY(i) * snakeBlockSize + snakeBlockSize)
            if (i == 0) {
                canvas.drawBitmap(snakeHeadBitmap!!, null, snakeRect, paint)
            } else {
                canvas.drawBitmap(snakeBodyBitmap!!, null, snakeRect, paint)
            }
        }
        // Scale the HUD text
        paint.textSize = 70f
        paint.color = scoreTextColor
        canvas.drawText(String.format(currentScoreMsg, score), 10f, 60f, paint)
    }

    /**
     * Writes given score and updates highscore if required
     *
     * @param score of last game
     */
    private fun saveScores(score: Int) {
        val highscore = getSharedPreference(R.string.save_highscore, 0)
        setSharedPreference(R.string.save_lastscore, score)
        if (score > highscore) {
            setSharedPreference(R.string.save_highscore, score)
        }
    }

    /**
     * Updates the theme to the one stored in SavedPreferences
     */
    private fun updateTheme() {
        val applicationContext = context.applicationContext
        val contextResources = applicationContext.resources
        val contextTheme = applicationContext.theme
        val defaultTheme = contextResources.getString(R.string.setting_theme_default)
        when (Theme.valueOf(getSharedPreference(R.string.setting_theme, defaultTheme)!!)) {
            Theme.GRASS -> {
                backgroundBitmap = BitmapFactory.decodeResource(contextResources, R.drawable.background_grass)
                foodBitmap = BitmapFactory.decodeResource(contextResources, R.drawable.food_apple)
                snakeHeadBitmap = BitmapFactory.decodeResource(contextResources, R.drawable.snake_head)
                snakeBodyBitmap = BitmapFactory.decodeResource(contextResources, R.drawable.snake_body)
                scoreTextColor = contextResources.getColor(R.color.textColorDark, contextTheme)
            }
            Theme.WATER -> {
                backgroundBitmap = BitmapFactory.decodeResource(contextResources, R.drawable.background_water_new)
                foodBitmap = BitmapFactory.decodeResource(contextResources, R.drawable.food_fish)
                snakeHeadBitmap = BitmapFactory.decodeResource(contextResources, R.drawable.snake_water_head)
                snakeBodyBitmap = BitmapFactory.decodeResource(contextResources, R.drawable.snake_water_body)
                scoreTextColor = contextResources.getColor(R.color.textColorLight, contextTheme)
            }
        }
    }

    /**
     * Updates the control mode to the one stored in SavedPreferences
     */
    private fun updateControlMode() {
        val applicationContext = context.applicationContext
        val contextResources = applicationContext.resources
        val defaultControlMode = contextResources.getString(R.string.setting_control_default)
        controlMode = Controls.Mode.valueOf(sharedPref.getString(applicationContext.getString(R.string.setting_control), defaultControlMode)!!)
        controls = if (controlMode === Controls.Mode.BUTTONS) {
            val controlButtonSize = snakeBlockSize * 3
            val controlsY = screenY - controlButtonSize * 3 - snakeBlockSize
            Controls(snakeBlockSize, controlsY, controlButtonSize)
        } else {
            null
        }
    }

    /**
     * Method for button control
     *
     * @param motionEvent event from touch
     * @return result of super call
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        if (motionEvent.action == MotionEvent.ACTION_DOWN) {
            if (controlMode === Controls.Mode.BUTTONS && isPlaying) {
                val posX = motionEvent.x.roundToInt()
                val posY = motionEvent.y.roundToInt()
                when {
                    controls!!.getButton(Controls.Button.LEFT)!!.contains(posX, posY) -> {
                        snake!!.setDirectionLeft()
                    }
                    controls!!.getButton(Controls.Button.UP)!!.contains(posX, posY) -> {
                        snake!!.setDirectionUp()
                    }
                    controls!!.getButton(Controls.Button.RIGHT)!!.contains(posX, posY) -> {
                        snake!!.setDirectionRight()
                    }
                    controls!!.getButton(Controls.Button.DOWN)!!.contains(posX, posY) -> {
                        snake!!.setDirectionDown()
                    }
                }
            } else {
                startGame()
            }
        }
        return super.onTouchEvent(motionEvent)
    }

    /**
     * Method for tilt control
     *
     * @param event         event from the sensor
     * @param accelerometer Sensor that fired the event
     */
    fun onSensorChanged(event: SensorEvent, accelerometer: Sensor?) {
        if (event.sensor == accelerometer) {
            if (controlMode === Controls.Mode.TILT && isPlaying) {
                val x = event.values[0].roundToInt()
                val y = event.values[1].roundToInt()
                val xStrongerThanY = abs(x) > abs(y)
                @Suppress("SameParameterValue")
                when (screen.rotation) {
                    Surface.ROTATION_0 ->
                        //LEFT  : +x
                        //UP    : -y
                        //RIGHT : -x
                        //DOWN  : +y
                        if (xStrongerThanY) {
                            if (x > 0) {
                                snake!!.setDirectionLeft()
                            } else {
                                snake!!.setDirectionRight()
                            }
                        } else {
                            if (y > 0) {
                                snake!!.setDirectionDown()
                            } else {
                                snake!!.setDirectionUp()
                            }
                        }
                    Surface.ROTATION_90 ->
                        //LEFT  : -y
                        //UP    : -x
                        //RIGHT : +y
                        //DOWN  : +x
                        if (xStrongerThanY) {
                            if (x > 0) {
                                snake!!.setDirectionDown()
                            } else {
                                snake!!.setDirectionUp()
                            }
                        } else {
                            if (y > 0) {
                                snake!!.setDirectionRight()
                            } else {
                                snake!!.setDirectionLeft()
                            }
                        }
                    Surface.ROTATION_180 ->
                        //LEFT  : -x
                        //UP    : +y
                        //RIGHT : +x
                        //DOWN  : -y
                        if (xStrongerThanY) {
                            if (x > 0) {
                                snake!!.setDirectionRight()
                            } else {
                                snake!!.setDirectionLeft()
                            }
                        } else {
                            if (y > 0) {
                                snake!!.setDirectionUp()
                            } else {
                                snake!!.setDirectionDown()
                            }
                        }
                    Surface.ROTATION_270 ->
                        //LEFT  : +y
                        //UP    : +x
                        //RIGHT : -y
                        //DOWN  : -x
                        if (xStrongerThanY) {
                            if (x > 0) {
                                snake!!.setDirectionUp()
                            } else {
                                snake!!.setDirectionDown()
                            }
                        } else {
                            if (y > 0) {
                                snake!!.setDirectionLeft()
                            } else {
                                snake!!.setDirectionRight()
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
    fun onFling(velocityX: Float, velocityY: Float): Boolean {
        if (controlMode === Controls.Mode.GESTURES && isPlaying) {
            if (abs(velocityX) > abs(velocityY)) {
                if (velocityX > 0) {
                    snake!!.setDirectionRight()
                } else {
                    snake!!.setDirectionLeft()
                }
            } else {
                if (velocityY > 0) {
                    snake!!.setDirectionDown()
                } else {
                    snake!!.setDirectionUp()
                }
            }
        } else {
            return false
        }
        return true
    }

    /**
     * Assigns its [Controls.Mode] to the pressed radio button and updates the controls if required
     *
     * @param v pressed View
     */
    fun onControlSelected(v: View) {
        when (v.id) {
            R.id.control_buttons_button -> updateControlModeIfRequired(Controls.Mode.BUTTONS.toString())
            R.id.control_swype_button -> updateControlModeIfRequired(Controls.Mode.GESTURES.toString())
            R.id.control_tilt_button -> updateControlModeIfRequired(Controls.Mode.TILT.toString())
        }
    }

    /**
     * Checks if the setting is different from the previous one and triggers an update accordingly
     *
     * @param value of the [Controls.Mode] to be used
     */
    private fun updateControlModeIfRequired(value: String) {
        val defaultControl = context.applicationContext.resources.getString(R.string.setting_control_default)
        if (getSharedPreference(R.string.setting_control, defaultControl) != value) {
            setSharedPreference(R.string.setting_control, value)
            updateControlMode()
        }
    }

    /**
     * Assigns its [Theme] to the pressed radio button and updates the theme if required
     *
     * @param v pressed View
     */
    fun onThemeSelected(v: View) {
        when (v.id) {
            R.id.theme_grass_button -> updateThemeIfRequired(Theme.GRASS.toString())
            R.id.theme_water_button -> updateThemeIfRequired(Theme.WATER.toString())
        }
    }

    /**
     * Checks if the setting is different from the previous one and triggers an update accordingly
     *
     * @param value of the [Theme] to be used
     */
    private fun updateThemeIfRequired(value: String) {
        val defaultTheme = context.applicationContext.resources.getString(R.string.setting_theme_default)
        if (getSharedPreference(R.string.setting_theme, defaultTheme) != value) {
            setSharedPreference(R.string.setting_theme, value)
            updateTheme()
        }
    }

    /**
     * Gets the value to the given resourceStringValue-key
     *
     * @param resourceStringValue key to search by
     * @param defaultValue        will be returned if there is no value to given key
     * @return value to given key or default value
     */
    private fun getSharedPreference(resourceStringValue: Int, defaultValue: String): String? {
        return sharedPref.getString(context.applicationContext.getString(resourceStringValue), defaultValue)
    }

    /**
     * Gets the value to the given resourceStringValue-key
     *
     * @param resourceStringValue key to search by
     * @param defaultValue        will be returned if there is no value to given key
     * @return value to given key or default value
     */
    private fun getSharedPreference(resourceStringValue: Int, @Suppress("SameParameterValue") defaultValue: Int): Int {
        return sharedPref.getInt(context.applicationContext.getString(resourceStringValue), defaultValue)
    }

    /**
     * Puts the given key value pair as an [SharedPreferences]
     *
     * @param resourceStringValue Resource string value
     * @param value               String value to be matched
     */
    private fun setSharedPreference(resourceStringValue: Int, value: String) {
        sharedPreferencesEditor.putString(context.applicationContext.getString(resourceStringValue), value)
        sharedPreferencesEditor.apply()
    }

    /**
     * Puts the given key value pair as an [SharedPreferences]
     *
     * @param resourceStringValue Resource string value
     * @param value               int value to be matched
     */
    private fun setSharedPreference(resourceStringValue: Int, value: Int) {
        sharedPreferencesEditor.putInt(context.applicationContext.getString(resourceStringValue), value)
        sharedPreferencesEditor.apply()
    }

    companion object {
        private fun runOnUiThread(runnable: Runnable) {
            val uiHandler = Handler(Looper.getMainLooper())
            uiHandler.post(runnable)
        }
    }

    init {
        //Activity
        //Context variables for later use
        val contextResources = activity.resources
        val contextTheme = activity.theme
        //Shared preferences
        sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
        sharedPreferencesEditor = sharedPref.edit()
        //Get resource strings
        menuTitle = activity.getString(R.string.app_name)
        currentScoreMsg = activity.getString(R.string.label_current_score)
        //Get colors
        snakeColor = contextResources.getColor(R.color.snake, contextTheme)
        foodColor = contextResources.getColor(R.color.food, contextTheme)
        controllersColor = contextResources.getColor(R.color.controllers, contextTheme)
        //Theme switch
        updateTheme()
        //Orientation lock
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        // Get the pixel dimensions of the screen
        screen = activity.windowManager.defaultDisplay
        // Initialize the result into a Point object
        val size = Point()
        screen.getSize(size)
        //Set screen size
        screenX = size.x
        screenY = size.y
        surfaceHolder = holder
        paint = Paint()
        //Resize when using portrait mode
        if (screenX < screenY) {
            numBlocksWide = 20
        }
        snakeBlockSize = screenX / numBlocksWide
        numBlocksHigh = screenY / snakeBlockSize
        maxBlocksOnScreen = numBlocksWide * numBlocksHigh
        //Prepares the button draw if needed
        updateControlMode()
        food = Rect()
        nextFrameTime = System.currentTimeMillis()
    }
}