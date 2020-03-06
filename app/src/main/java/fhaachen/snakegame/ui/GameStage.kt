package fhaachen.snakegame.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
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
import androidx.core.content.ContextCompat.startActivity
import fhaachen.snakegame.R
import fhaachen.snakegame.enums.ControlMode
import fhaachen.snakegame.enums.Theme
import fhaachen.snakegame.helper.ControlsHelper
import fhaachen.snakegame.helper.ScoreHelper.getHighScore
import fhaachen.snakegame.helper.ScoreHelper.getLastScore
import fhaachen.snakegame.helper.ScoreHelper.saveScore
import fhaachen.snakegame.helper.SharedPreferencesHelper.getSharedPreference
import fhaachen.snakegame.helper.SharedPreferencesHelper.setSharedPreference
import fhaachen.snakegame.helper.SnakeHelper.detectDeath
import fhaachen.snakegame.model.Controls
import fhaachen.snakegame.model.Snake
import java.util.*
import java.util.stream.IntStream

class GameStage
constructor(context: Context?) : SurfaceView(context), Runnable, DialogInterface.OnDismissListener {
    //Misc
    private val surfaceHolder: SurfaceHolder
    private val paint: Paint = Paint()
    private var thread: Thread? = null
    private val activity: AppCompatActivity = getContext() as AppCompatActivity
    private val applicationContext: Context = getContext().applicationContext

    //Sizing
    private var numBlocksWide = 40
    private val screenX: Int
    private val screenY: Int
    private val snakeBlockSize: Int
    private val numBlocksHigh: Int
    private val maxBlocksOnScreen: Int
    private val screen: Display = activity.windowManager.defaultDisplay

    //Timing
    private var fps: Long = 7
    private var nextFrameTime: Long

    //Game states
    @Volatile
    private var isRunning = false

    @Volatile
    private var isPlaying = false
    private var pauseMenuShown = false

    //Game elements
    private var snake: Snake? = null
    private var controls: Controls? = null
    private lateinit var controlMode: ControlMode
    private var food: Rect = Rect()
    private var score = 0

    //Colors
    private var scoreTextColor = 0

    //Bitmaps
    private lateinit var backgroundBitmap: Bitmap
    private lateinit var foodBitmap: Bitmap
    private lateinit var snakeHeadBitmap: Bitmap
    private lateinit var snakeBodyBitmap: Bitmap

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
                .setTitle(applicationContext.getString(R.string.app_name))
                .setPositiveButton(R.string.button_play) { _: DialogInterface?, _: Int -> startGameAndClosePauseMenu() }
                .setNegativeButton(R.string.button_exit) { _: DialogInterface?, _: Int -> activity.finishAndRemoveTask() }
                .setOnDismissListener(this)
        //Scores
        val lastScore = view.findViewById<TextView>(R.id.your_score)
        val highScore = view.findViewById<TextView>(R.id.highscore)

        highScore.setOnClickListener {
            composeMessage(applicationContext.getString(R.string.share_highscore, getSharedPreference(context, R.string.save_highscore, 0)))
        }

        lastScore.text = getLastScore(context)
        highScore.text = getHighScore(context)
        //Set theme radioButton
        val themeRadioButton: RadioButton
        val theme = Theme.valueOf(getSharedPreference(context, R.string.setting_theme, Theme.GRASS.toString()))
        themeRadioButton = when (theme) {
            Theme.WATER -> view.findViewById(R.id.theme_water_button)
            else -> view.findViewById(R.id.theme_grass_button)
        }
        runOnUiThread(Runnable { themeRadioButton.isChecked = true })
        val controlModeRadioButton: RadioButton
        val controlMode = ControlMode.valueOf(getSharedPreference(context, R.string.setting_control, ControlMode.GESTURES.toString()))
        controlModeRadioButton = when (controlMode) {
            ControlMode.BUTTONS -> view.findViewById(R.id.control_buttons_button)
            ControlMode.TILT -> view.findViewById(R.id.control_tilt_button)
            else -> view.findViewById(R.id.control_swype_button)
        }
        runOnUiThread(Runnable { controlModeRadioButton.isChecked = true })
        runOnUiThread(Runnable {
            builder.create()
            builder.show()
        })
    }

    /**
     * Share message
     */
    private fun composeMessage(message: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, message)
            type = "text/plain"
        }
        startActivity(context, Intent.createChooser(intent, "Share"), null)
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
            // Screen is full with the snake
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
        if (detectDeath(snake!!, numBlocksWide, numBlocksHigh)) {
            saveScore(context, score)
            isPlaying = false
            snake = null
        }
    }

    /**
     * Draws the game field
     */
    private fun draw() {
        if (surfaceHolder.surface.isValid) {
            val canvas = surfaceHolder.lockCanvas()
            drawBackgroundImage(canvas)
            // Draw game or menu?
            if (isPlaying && !pauseMenuShown) {
                drawGame(canvas)
            } else {
                showMenuDialog()
            }
            surfaceHolder.unlockCanvasAndPost(canvas)
        }
    }

    /**
     * Draws the background image
     */
    private fun drawBackgroundImage(canvas: Canvas) {
        // Set background image
        canvas.drawBitmap(backgroundBitmap, null, RectF(0F, 0F, screenX.toFloat(), screenY.toFloat()), null)
    }

    private fun drawGame(canvas: Canvas) {
        drawControls(canvas)

        drawFood(canvas)

        drawSnake(canvas)

        drawCurrentScore(canvas)
    }

    /**
     * Draws the Control buttons depending on current [controlMode]
     */
    private fun drawControls(canvas: Canvas) {
        // Set controls color
        paint.color = applicationContext.getColor(R.color.controllers)
        // Draw controls if needed
        if (controlMode === ControlMode.BUTTONS && controls != null) {
            for (control in controls!!.buttons) {
                canvas.drawRect(
                        control!!.left.toFloat(),
                        control.top.toFloat(),
                        control.right.toFloat(),
                        control.bottom.toFloat(),
                        paint)
            }
        }
    }

    /**
     * Draws current [food]
     */
    private fun drawFood(canvas: Canvas) {
        // Draw food
        paint.color = applicationContext.getColor(R.color.food)
        val foodRect = Rect(food.left, food.top, food.right, food.bottom)
        canvas.drawBitmap(foodBitmap, null, foodRect, paint)
    }

    /**
     * Draws current [snake]
     */
    private fun drawSnake(canvas: Canvas) {
        // Set snake color
        paint.color = applicationContext.getColor(R.color.snake)
        // Draw the snake
        for (i in 0 until snake!!.snakeLength + 1) {
            val snakeRect = Rect(snake!!.getBodyX(i) * snakeBlockSize,
                    snake!!.getBodyY(i) * snakeBlockSize,
                    snake!!.getBodyX(i) * snakeBlockSize + snakeBlockSize,
                    snake!!.getBodyY(i) * snakeBlockSize + snakeBlockSize)
            if (i == 0) {
                canvas.drawBitmap(snakeHeadBitmap, null, snakeRect, paint)
            } else {
                canvas.drawBitmap(snakeBodyBitmap, null, snakeRect, paint)
            }
        }
    }

    /**
     * Draws the Current score label
     */
    private fun drawCurrentScore(canvas: Canvas) {
        // Set text properties
        paint.textSize = 70f
        paint.color = scoreTextColor
        // Draw the text
        canvas.drawText(String.format(applicationContext.getString(R.string.label_current_score), score), 10f, 60f, paint)
    }

    /**
     * Updates the theme to the one stored in SavedPreferences
     */
    private fun updateTheme() {
        val applicationContext = context.applicationContext
        val contextResources = applicationContext.resources
        val contextTheme = applicationContext.theme
        val defaultTheme = contextResources.getString(R.string.setting_theme_default)
        when (Theme.valueOf(getSharedPreference(context, R.string.setting_theme, defaultTheme))) {
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
        controlMode = ControlMode.valueOf(getSharedPreference(context, R.string.setting_control, defaultControlMode))
        controls = if (controlMode === ControlMode.BUTTONS) {
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
        if (snake != null && ControlsHelper.onTouchEvent(motionEvent, controlMode, isPlaying, controls, snake!!)) {
            startGame()
        }
        return super.onTouchEvent(motionEvent)
    }

    /**
     * Method for tilt control
     *
     * @param event         event from the sensor
     * @param accelerometer Sensor that fired the event
     */
    fun onSensorChanged(event: SensorEvent, accelerometer: Sensor) {
        if (snake != null && isPlaying) {
            ControlsHelper.onSensorChanged(event, accelerometer, controlMode, screen.rotation, snake!!)
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
        if (snake != null) {
            return ControlsHelper.onFling(velocityX, velocityY, controlMode, isPlaying, snake!!)
        }
        return false
    }

    /**
     * Assigns its [Controls.ControlMode] to the pressed radio button and updates the controls if required
     *
     * @param v pressed View
     */
    fun onControlSelected(v: View) {
        when (v.id) {
            R.id.control_buttons_button -> updateControlModeIfRequired(ControlMode.BUTTONS.toString())
            R.id.control_swype_button -> updateControlModeIfRequired(ControlMode.GESTURES.toString())
            R.id.control_tilt_button -> updateControlModeIfRequired(ControlMode.TILT.toString())
        }
    }

    /**
     * Checks if the setting is different from the previous one and triggers an update accordingly
     *
     * @param value of the [Controls.ControlMode] to be used
     */
    private fun updateControlModeIfRequired(value: String) {
        val defaultControl = context.applicationContext.resources.getString(R.string.setting_control_default)
        if (getSharedPreference(context, R.string.setting_control, defaultControl) != value) {
            setSharedPreference(context, R.string.setting_control, value)
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
        if (getSharedPreference(context, R.string.setting_theme, defaultTheme) != value) {
            setSharedPreference(context, R.string.setting_theme, value)
            updateTheme()
        }
    }

    companion object {
        private fun runOnUiThread(runnable: Runnable) {
            val uiHandler = Handler(Looper.getMainLooper())
            uiHandler.post(runnable)
        }
    }

    init {
        //Theme switch
        updateTheme()

        //Orientation lock
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED

        // Set screen size
        val size = Point()
        screen.getSize(size)
        screenX = size.x
        screenY = size.y

        surfaceHolder = holder

        if (screenX < screenY) {
            //Resize when using portrait mode
            numBlocksWide = 20
        }
        snakeBlockSize = screenX / numBlocksWide
        numBlocksHigh = screenY / snakeBlockSize
        maxBlocksOnScreen = numBlocksWide * numBlocksHigh
        //Prepares the button draw if needed
        updateControlMode()

        nextFrameTime = System.currentTimeMillis()
    }
}