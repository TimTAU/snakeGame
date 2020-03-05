package fhaachen.snakegame.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;

import java.util.Random;
import java.util.stream.IntStream;

import fhaachen.snakegame.R;
import fhaachen.snakegame.model.Controls;
import fhaachen.snakegame.model.Snake;
import fhaachen.snakegame.model.Theme;

@SuppressLint("ViewConstructor")
public class GameStage extends SurfaceView implements Runnable, DialogInterface.OnDismissListener {
    private int numBlocksWide = 40;
    private long fps = 7;
    private final SurfaceHolder surfaceHolder;
    private final Paint paint;
    private Thread thread = null;
    private volatile boolean isRunning;
    private volatile boolean isPlaying;
    private boolean pauseMenuShown;
    private final int screenX;
    private final int screenY;
    private final int snakeBlockSize;
    private final int numBlocksHigh;
    private long nextFrameTime;
    private final int maxBlocksOnScreen;
    private final Display display;
    private Controls.Mode controlMode;
    private final AppCompatActivity activity;

    private final SharedPreferences sharedPref;
    private final SharedPreferences.Editor sharedPreferencesEditor;

    private Snake snake;
    private Controls controls;
    private final Rect food;
    private int score;

    //Resource strings
    private final String menuTitle;
    private final String currentScoreMsg;

    //Colors
    private int scoreTextColor;
    private final int snakeColor;
    private final int foodColor;
    private final int controllersColor;

    //Bitmaps
    private Bitmap backgroundBitmap;
    private Bitmap foodBitmap;
    private Bitmap snakeHeadBitmap;
    private Bitmap snakeBodyBitmap;

    @SuppressLint("CommitPrefEdits") //Commitment will be done later
    public GameStage(Context context) {
        super(context);
        //Activity
        activity = (AppCompatActivity) getContext();

        //Context variables for later use
        Resources contextResources = activity.getResources();
        Resources.Theme contextTheme = activity.getTheme();

        //Shared preferences
        sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        sharedPreferencesEditor = sharedPref.edit();

        //Get resource strings
        menuTitle = activity.getString(R.string.app_name);
        currentScoreMsg = activity.getString(R.string.label_current_score);

        //Get colors
        snakeColor = contextResources.getColor(R.color.snake, contextTheme);
        foodColor = contextResources.getColor(R.color.food, contextTheme);
        controllersColor = contextResources.getColor(R.color.controllers, contextTheme);

        //Theme switch
        updateTheme();

        //Orientation lock
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        // Get the pixel dimensions of the screen
        display = activity.getWindowManager().getDefaultDisplay();
        // Initialize the result into a Point object
        Point size = new Point();
        display.getSize(size);
        //Set screen size
        screenX = size.x;
        screenY = size.y;

        surfaceHolder = getHolder();
        paint = new Paint();

        //Resize when using portrait mode
        if (screenX < screenY) {
            numBlocksWide = 20;
        }
        snakeBlockSize = screenX / numBlocksWide;
        numBlocksHigh = screenY / snakeBlockSize;
        maxBlocksOnScreen = numBlocksWide * numBlocksHigh;

        //Prepares the button draw if needed
        updateControlMode();

        food = new Rect();

        nextFrameTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        while (isRunning) {
            if (updateRequired()) {
                if (isPlaying) {
                    update();
                }
                draw();
            }
        }
    }

    /**
     * Pauses the game
     */
    public void pause() {
        isRunning = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Resumes the game after counter hits zero
     */
    public void resume() {
        isRunning = true;
        thread = new Thread(this);
        thread.start();
    }

    @SuppressLint("InflateParams")
    // Pass null as the parent view because its going in the dialog layout
    public void showMenuDialog() {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        pauseMenuShown = true;
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getContext(), R.style.ScoreTheme));

        // Inflate and set the layout for the dialog
        View view = activity.getLayoutInflater().inflate(R.layout.pause_menu, null);
        builder.setView(view)
                .setTitle(menuTitle)
                .setPositiveButton(R.string.button_play, (dialog, id) -> startGameAndClosePauseMenu())
                .setNegativeButton(R.string.button_exit, (dialog, id) -> activity.finishAndRemoveTask())
                .setOnDismissListener(this);

        //Scores
        TextView lastScore = view.findViewById(R.id.your_score);
        TextView highScore = view.findViewById(R.id.highscore);
        lastScore.setText(String.valueOf(getSharedPreference(R.string.save_lastscore, 0)));
        highScore.setText(String.valueOf(getSharedPreference(R.string.save_highscore, 0)));

        //Set theme radioButton
        RadioButton themeRadioButton;
        Theme theme = Theme.valueOf(getSharedPreference(R.string.setting_theme, Theme.GRASS.toString()));
        //Maybe there will be more themes
        //noinspection SwitchStatementWithTooFewBranches
        switch (theme) {
            case WATER:
                themeRadioButton = view.findViewById(R.id.theme_water_button);
                break;
            default:
                themeRadioButton = view.findViewById(R.id.theme_grass_button);
        }
        runOnUiThread(() -> themeRadioButton.setChecked(true));

        RadioButton controlModeRadioButton;
        Controls.Mode controlMode = Controls.Mode.valueOf(getSharedPreference(R.string.setting_control, Controls.Mode.GESTURES.toString()));
        switch (controlMode) {
            case BUTTONS:
                controlModeRadioButton = view.findViewById(R.id.control_buttons_button);
                break;
            case TILT:
                controlModeRadioButton = view.findViewById(R.id.control_tilt_button);
                break;
            default:
                controlModeRadioButton = view.findViewById(R.id.control_swype_button);
        }
        runOnUiThread(() -> controlModeRadioButton.setChecked(true));

        runOnUiThread(() -> {
            builder.create();
            builder.show();
        });
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        startGameAndClosePauseMenu();
    }

    private static void runOnUiThread(Runnable runnable) {
        Handler UIHandler = new Handler(Looper.getMainLooper());
        UIHandler.post(runnable);
    }

    private void startGameAndClosePauseMenu() {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        startGame();
        pauseMenuShown = false;
        isRunning = true;
    }

    /**
     * Sets all variables needed for game start
     */
    private void startGame() {
        if (!isPlaying) {
            snake = new Snake(
                    numBlocksWide / 2,
                    numBlocksHigh / 2,
                    maxBlocksOnScreen);

            spawnFood();
            score = 0;
            nextFrameTime = System.currentTimeMillis();
            isPlaying = true;
            fps = 7;
        }
    }

    /**
     * Spawns food at a random location but not inside the snake
     */
    private void spawnFood() {
        Random random = new Random();
        int randomX;
        int randomY;

        do {
            randomX = random.nextInt(numBlocksWide - 1) + 1;
            randomY = random.nextInt(numBlocksHigh - 1) + 1;
        } while (positionInsideSnake(randomX, randomY));

        int x = randomX * snakeBlockSize;
        int y = randomY * snakeBlockSize;
        food.set(
                x,
                y,
                x + snakeBlockSize,
                y + snakeBlockSize);
    }

    /**
     * Checks if the given coordinates are included in the snake
     *
     * @param x coordinate to be checked
     * @param y coordinate to be checked
     * @return true if the coordinate is included
     */
    private boolean positionInsideSnake(int x, int y) {
        return IntStream.of(snake.getBodyXs()).anyMatch(snakeX -> snakeX == x) && IntStream.of(snake.getBodyYs()).anyMatch(snakeY -> snakeY == y);
    }

    /**
     * Consumes food and spawns new
     */
    private void eatFood() {
        score++;
        if (score < (maxBlocksOnScreen - 1)) {
            spawnFood();
            snake.increaseSize();
            if (score != 0 && score % 4 == 0 && fps <= 20) {
                fps++;
            }
        } else {
            isPlaying = false;
        }
    }

    /**
     * Evaluates if a screen update is required
     *
     * @return true if update is required
     */
    private boolean updateRequired() {
        if (!pauseMenuShown && nextFrameTime <= System.currentTimeMillis()) {
            nextFrameTime = System.currentTimeMillis() + 1000 / fps;
            return true;
        }
        return false;
    }

    /**
     * Updates the game state
     */
    private void update() {
        if ((snake.getHeadX() * snakeBlockSize) == food.left && (snake.getHeadY() * snakeBlockSize) == food.top) {
            eatFood();
        }

        snake.moveSnake();

        if (detectDeath()) {
            saveScores(score);
            isPlaying = false;
            snake = null;
        }
    }

    /**
     * Evaluates if the player is dead
     *
     * @return true if death is detected
     */
    private boolean detectDeath() {
        // Hit the screen edge
        if (snake.getHeadX() == -1 || snake.getHeadX() >= numBlocksWide + 1 || snake.getHeadY() == -1 || snake.getHeadY() == numBlocksHigh + 1) {
            return true;
        }

        // Hit itself
        for (int i = snake.getSnakeLength(); i > 0; i--) {
            if ((i > 4)
                    && (snake.getHeadX() == snake.getBodyX(i))
                    && (snake.getHeadY() == snake.getBodyY(i))) {
                return true;
            }
        }

        // Hit nothing
        return false;
    }

    /**
     * Draws the game field
     */
    private void draw() {
        if (surfaceHolder.getSurface().isValid()) {
            Canvas canvas = surfaceHolder.lockCanvas();
            // Set background image
            canvas.drawBitmap(backgroundBitmap, null, new RectF(0, 0, screenX, screenY), null);

            if (isPlaying && !pauseMenuShown) {
                drawGame(canvas, paint);
            } else {
                showMenuDialog();
            }

            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawGame(Canvas canvas, Paint paint) {
        // Set controls color
        paint.setColor(controllersColor);

        // Draw controls if needed
        if (controlMode == Controls.Mode.BUTTONS && controls != null) {
            for (Rect control : controls.getButtons()) {
                canvas.drawRect(
                        control.left,
                        control.top,
                        control.right,
                        control.bottom,
                        paint);
            }
        }

        // Draw food
        paint.setColor(foodColor);
        Rect foodRect = new Rect(food.left, food.top, food.right, food.bottom);
        canvas.drawBitmap(foodBitmap, null, foodRect, paint);

        // Set snake color
        paint.setColor(snakeColor);

        // Draw the snake
        for (int i = 0; i < snake.getSnakeLength() + 1; i++) {
            Rect snakeRect = new Rect(snake.getBodyX(i) * snakeBlockSize,
                    (snake.getBodyY(i) * snakeBlockSize),
                    (snake.getBodyX(i) * snakeBlockSize) + snakeBlockSize,
                    (snake.getBodyY(i) * snakeBlockSize) + snakeBlockSize);
            if (i == 0) {
                canvas.drawBitmap(snakeHeadBitmap, null, snakeRect, paint);
            } else {
                canvas.drawBitmap(snakeBodyBitmap, null, snakeRect, paint);
            }
        }

        // Scale the HUD text
        paint.setTextSize(70);
        paint.setColor(scoreTextColor);
        canvas.drawText(String.format(currentScoreMsg, score), 10, 60, paint);
    }

    /**
     * Writes given score and updates highscore if required
     *
     * @param score of last game
     */
    private void saveScores(int score) {
        int highscore = getSharedPreference(R.string.save_highscore, 0);

        setSharedPreference(R.string.save_lastscore, score);

        if (score > highscore) {
            setSharedPreference(R.string.save_highscore, highscore);
        }
    }

    /**
     * Updates the theme to the one stored in SavedPreferences
     */
    private void updateTheme() {
        Context applicationContext = getContext().getApplicationContext();
        Resources contextResources = applicationContext.getResources();
        Resources.Theme contextTheme = applicationContext.getTheme();
        String defaultTheme = contextResources.getString(R.string.setting_theme_default);

        Theme theme = Theme.valueOf(getSharedPreference(R.string.setting_theme, defaultTheme));
        switch (theme) {
            case GRASS:
                backgroundBitmap = BitmapFactory.decodeResource(contextResources, R.drawable.background_grass);
                foodBitmap = BitmapFactory.decodeResource(contextResources, R.drawable.food_apple);
                snakeHeadBitmap = BitmapFactory.decodeResource(contextResources, R.drawable.snake_head);
                snakeBodyBitmap = BitmapFactory.decodeResource(contextResources, R.drawable.snake_body);
                scoreTextColor = contextResources.getColor(R.color.textColorDark, contextTheme);
                break;
            case WATER:
                backgroundBitmap = BitmapFactory.decodeResource(contextResources, R.drawable.background_water_new);
                foodBitmap = BitmapFactory.decodeResource(contextResources, R.drawable.food_fish);
                snakeHeadBitmap = BitmapFactory.decodeResource(contextResources, R.drawable.snake_water_head);
                snakeBodyBitmap = BitmapFactory.decodeResource(contextResources, R.drawable.snake_water_body);
                scoreTextColor = contextResources.getColor(R.color.textColorLight, contextTheme);
                break;
        }
    }

    /**
     * Updates the control mode to the one stored in SavedPreferences
     */
    private void updateControlMode() {
        Context applicationContext = getContext().getApplicationContext();
        Resources contextResources = applicationContext.getResources();
        String defaultControlMode = contextResources.getString(R.string.setting_control_default);

        controlMode = Controls.Mode.valueOf(sharedPref.getString(applicationContext.getString(R.string.setting_control), defaultControlMode));
        if (controlMode == Controls.Mode.BUTTONS) {
            int controlButtonSize = snakeBlockSize * 3;
            int controlsY = screenY - (controlButtonSize * 3) - snakeBlockSize;
            controls = new Controls(snakeBlockSize, controlsY, controlButtonSize);
        } else {
            controls = null;
        }
    }

    /**
     * Method for button control
     *
     * @param motionEvent event from touch
     * @return result of super call
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            if (controlMode == Controls.Mode.BUTTONS && isPlaying) {
                int posX = Math.round(motionEvent.getX());
                int posY = Math.round(motionEvent.getY());

                if (controls.getButton(Controls.Button.LEFT).contains(posX, posY)) {
                    snake.setDirectionLeft();
                } else if (controls.getButton(Controls.Button.UP).contains(posX, posY)) {
                    snake.setDirectionUp();
                } else if (controls.getButton(Controls.Button.RIGHT).contains(posX, posY)) {
                    snake.setDirectionRight();
                } else if (controls.getButton(Controls.Button.DOWN).contains(posX, posY)) {
                    snake.setDirectionDown();
                }
            } else {
                startGame();
            }
        }
        return super.onTouchEvent(motionEvent);
    }

    /**
     * Method for tilt control
     *
     * @param event         event from the sensor
     * @param accelerometer Sensor that fired the event
     */
    public void onSensorChanged(SensorEvent event, Sensor accelerometer) {
        if (event.sensor == accelerometer) {
            if (controlMode == Controls.Mode.TILT && isPlaying) {
                int x = Math.round(event.values[0]);
                int y = Math.round(event.values[1]);
                boolean xStrongerThanY = Math.abs(x) > Math.abs(y);

                switch (display.getRotation()) {
                    case Surface.ROTATION_0:
                        //LEFT  : +x
                        //UP    : -y
                        //RIGHT : -x
                        //DOWN  : +y
                        if (xStrongerThanY) {
                            if (x > 0) {
                                snake.setDirectionLeft();
                            } else {
                                snake.setDirectionRight();
                            }
                        } else {
                            if (y > 0) {
                                snake.setDirectionDown();
                            } else {
                                snake.setDirectionUp();
                            }
                        }
                        break;
                    case Surface.ROTATION_90:
                        //LEFT  : -y
                        //UP    : -x
                        //RIGHT : +y
                        //DOWN  : +x
                        if (xStrongerThanY) {
                            if (x > 0) {
                                snake.setDirectionDown();
                            } else {
                                snake.setDirectionUp();
                            }
                        } else {
                            if (y > 0) {
                                snake.setDirectionRight();
                            } else {
                                snake.setDirectionLeft();
                            }
                        }
                        break;
                    case Surface.ROTATION_180:
                        //LEFT  : -x
                        //UP    : +y
                        //RIGHT : +x
                        //DOWN  : -y
                        if (xStrongerThanY) {
                            if (x > 0) {
                                snake.setDirectionRight();
                            } else {
                                snake.setDirectionLeft();
                            }
                        } else {
                            if (y > 0) {
                                snake.setDirectionUp();
                            } else {
                                snake.setDirectionDown();
                            }
                        }
                        break;
                    case Surface.ROTATION_270:
                        //LEFT  : +y
                        //UP    : +x
                        //RIGHT : -y
                        //DOWN  : -x
                        if (xStrongerThanY) {
                            if (x > 0) {
                                snake.setDirectionUp();
                            } else {
                                snake.setDirectionDown();
                            }
                        } else {
                            if (y > 0) {
                                snake.setDirectionLeft();
                            } else {
                                snake.setDirectionRight();
                            }
                        }
                        break;
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
    public boolean onFling(float velocityX, float velocityY) {
        if (controlMode == Controls.Mode.GESTURES) {
            if (Math.abs(velocityX) > Math.abs(velocityY)) {
                if (velocityX > 0) {
                    snake.setDirectionRight();
                } else {
                    snake.setDirectionLeft();
                }
            } else {
                if (velocityY > 0) {
                    snake.setDirectionDown();
                } else {
                    snake.setDirectionUp();
                }
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * Assigns its {@link Controls.Mode} to the pressed radio button and updates the controls if required
     *
     * @param v pressed View
     */
    public void onControlSelected(View v) {
        switch (v.getId()) {
            case R.id.control_buttons_button:
                updateControlModeIfRequired(Controls.Mode.BUTTONS.toString());
                break;
            case R.id.control_swype_button:
                updateControlModeIfRequired(Controls.Mode.GESTURES.toString());
                break;
            case R.id.control_tilt_button:
                updateControlModeIfRequired(Controls.Mode.TILT.toString());
        }
    }

    /**
     * Checks if the setting is different from the previous one and triggers an update accordingly
     *
     * @param value of the {@link Controls.Mode} to be used
     */
    private void updateControlModeIfRequired(String value) {
        String defaultControl = getContext().getApplicationContext().getResources().getString(R.string.setting_control_default);

        if (!(getSharedPreference(R.string.setting_control, defaultControl).equals(value))) {
            setSharedPreference(R.string.setting_control, value);
            updateControlMode();
        }
    }

    /**
     * Assigns its {@link Theme} to the pressed radio button and updates the theme if required
     *
     * @param v pressed View
     */
    public void onThemeSelected(View v) {
        switch (v.getId()) {
            case R.id.theme_grass_button:
                updateThemeIfRequired(Theme.GRASS.toString());
                break;
            case R.id.theme_water_button:
                updateThemeIfRequired(Theme.WATER.toString());
                break;
        }
    }

    /**
     * Checks if the setting is different from the previous one and triggers an update accordingly
     *
     * @param value of the {@link Theme} to be used
     */
    private void updateThemeIfRequired(String value) {
        String defaultTheme = getContext().getApplicationContext().getResources().getString(R.string.setting_theme_default);

        if (!(getSharedPreference(R.string.setting_theme, defaultTheme).equals(value))) {
            setSharedPreference(R.string.setting_theme, value);
            updateTheme();
        }
    }

    /**
     * Gets the value to the given resourceStringValue-key
     *
     * @param resourceStringValue key to search by
     * @param defaultValue        will be returned if there is no value to given key
     * @return value to given key or default value
     */
    private String getSharedPreference(int resourceStringValue, String defaultValue) {
        return sharedPref.getString(getContext().getApplicationContext().getString(resourceStringValue), defaultValue);
    }

    /**
     * Gets the value to the given resourceStringValue-key
     *
     * @param resourceStringValue key to search by
     * @param defaultValue        will be returned if there is no value to given key
     * @return value to given key or default value
     */
    private int getSharedPreference(int resourceStringValue, @SuppressWarnings("SameParameterValue") int defaultValue) {
        return sharedPref.getInt(getContext().getApplicationContext().getString(resourceStringValue), defaultValue);
    }

    /**
     * Puts the given key value pair as an {@link SharedPreferences}
     *
     * @param resourceStringValue Resource string value
     * @param value               String value to be matched
     */
    private void setSharedPreference(int resourceStringValue, String value) {
        sharedPreferencesEditor.putString(getContext().getApplicationContext().getString(resourceStringValue), value);
        sharedPreferencesEditor.apply();
    }

    /**
     * Puts the given key value pair as an {@link SharedPreferences}
     *
     * @param resourceStringValue Resource string value
     * @param value               int value to be matched
     */
    private void setSharedPreference(int resourceStringValue, int value) {
        sharedPreferencesEditor.putInt(getContext().getApplicationContext().getString(resourceStringValue), value);
        sharedPreferencesEditor.apply();
    }
}