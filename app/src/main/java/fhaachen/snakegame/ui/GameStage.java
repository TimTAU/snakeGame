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
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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
    private final Controls.Mode controlMode;
    private final AppCompatActivity activity;

    private Snake snake;
    private final Controls controls;
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

    public GameStage(Context context) {
        super(context);
        //Activity
        activity = (AppCompatActivity) getContext();

        //Context variables for later use
        Resources contextResources = activity.getResources();
        Resources.Theme contextTheme = activity.getTheme();

        //Shared preferences
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);

        //Set resource strings
        menuTitle = activity.getString(R.string.app_name);
        currentScoreMsg = activity.getString(R.string.current_score);
        String settingTheme = activity.getString(R.string.setting_theme);

        //Set colors
        snakeColor = contextResources.getColor(R.color.snake, contextTheme);
        foodColor = contextResources.getColor(R.color.food, contextTheme);
        controllersColor = contextResources.getColor(R.color.controllers, contextTheme);
        int textColorLight = contextResources.getColor(R.color.textColorLight, contextTheme);
        int textColorDark = contextResources.getColor(R.color.textColorDark, contextTheme);

        //Default settings
        String defaultTheme = contextResources.getString(R.string.setting_theme_default);
        String defaultControlMode = contextResources.getString(R.string.setting_control_default);

        //FIXME: Example for saving theme setting
        /*SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(activity.getString(R.string.setting_theme), "WATER");
        editor.apply();*/

        //Theme switch
        Theme theme = Theme.valueOf(sharedPref.getString(activity.getString(R.string.setting_theme), defaultTheme));
        switch (theme) {
            case GRASS:
                backgroundBitmap = BitmapFactory.decodeResource(contextResources, R.drawable.background_grass);
                foodBitmap = BitmapFactory.decodeResource(contextResources, R.drawable.food_apple);
                snakeHeadBitmap = BitmapFactory.decodeResource(contextResources, R.drawable.snake_head);
                snakeBodyBitmap = BitmapFactory.decodeResource(contextResources, R.drawable.snake_body);
                scoreTextColor = textColorDark;
                break;
            case WATER:
                backgroundBitmap = BitmapFactory.decodeResource(contextResources, R.drawable.background_water_new);
                foodBitmap = BitmapFactory.decodeResource(contextResources, R.drawable.food_fish);
                snakeHeadBitmap = BitmapFactory.decodeResource(contextResources, R.drawable.snake_water_head);
                snakeBodyBitmap = BitmapFactory.decodeResource(contextResources, R.drawable.snake_water_body);
                scoreTextColor = textColorLight;
                break;
        }

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

        //FIXME: Example for saving control setting
        /*SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(activity.getString(R.string.setting_control), "GESTURES");
        editor.apply();*/

        //Prepares the button draw if needed
        controlMode = Controls.Mode.valueOf(sharedPref.getString(activity.getString(R.string.setting_control), defaultControlMode));
        if (controlMode == Controls.Mode.BUTTONS) {
            int controlButtonSize = snakeBlockSize * 3;
            int controlsY = screenY - (controlButtonSize * 3) - snakeBlockSize;
            controls = new Controls(snakeBlockSize, controlsY, controlButtonSize);
        } else {
            controls = null;
        }

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
    public void showPauseDialog() {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        pauseMenuShown = true;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        // Inflate and set the layout for the dialog
        View view = activity.getLayoutInflater().inflate(R.layout.pause_menu, null);
        builder.setView(view)
                .setTitle(menuTitle)
                .setPositiveButton(R.string.play, (dialog, id) -> startGameAndClosePauseMenu())
                .setNegativeButton(R.string.exit, (dialog, id) -> activity.finishAndRemoveTask())
                .setOnDismissListener(this);

        TextView lastScoreLabel = view.findViewById(R.id.your_score_label);
        TextView lastScore = view.findViewById(R.id.your_score);
        if (score != 0) {
            lastScoreLabel.setVisibility(VISIBLE);
            lastScore.setVisibility(VISIBLE);
            lastScore.setText(String.valueOf(score));
        } else {
            lastScoreLabel.setVisibility(INVISIBLE);
            lastScore.setVisibility(INVISIBLE);
        }

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
                drawStart();
            }

            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawStart() {

        showPauseDialog();


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
}