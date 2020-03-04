package fhaachen.snakegame.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import fhaachen.snakegame.R;
import fhaachen.snakegame.model.ControlMode;
import fhaachen.snakegame.model.Controls;
import fhaachen.snakegame.model.Snake;
import fhaachen.snakegame.model.Theme;

import static androidx.core.content.ContextCompat.getSystemService;

@SuppressLint("ViewConstructor")
public class GameStage extends SurfaceView implements Runnable {
    private Context context;
    private int numBlocksWide = 40;
    private long fps = 7;
    private SurfaceHolder surfaceHolder;
    private Paint paint;
    private Thread thread = null;
    private volatile boolean isRunning;
    private volatile boolean isPlaying;
    private int screenX;
    private int screenY;
    private int snakeBlockSize;
    private int numBlocksHigh;
    private long nextFrameTime;
    private int maxBlocksOnScreen;
    private Display display;
    private final ControlMode controlMode = ControlMode.TILT;

    private Snake snake;
    private Controls controls;
    private Rect food;
    private int score;

    //Messages
    private String currentScoreMsg;
    private String lastScoreMsg;
    private String startPromptMsg;
    private String congratulationsMsg;

    //Colors
    private final int textColor;
    private int scoreTextColor;
    private final int snakeColor;
    private final int foodColor;
    private final int controllersColor;

    //Bitmaps
    private Bitmap backgroundBitmap;
    private Bitmap foodBitmap;
    private Bitmap snakeHeadBitmap;
    private Bitmap snakeBodyBitmap;

    private boolean died = false;

    public GameStage(Context context, Point size) {
        super(context);
        this.context = context;
        //Context variables for later use
        final Resources contextResources = getContext().getResources();
        final Resources.Theme contextTheme = getContext().getTheme();

        //Set messages
        currentScoreMsg = getContext().getString(R.string.current_score);
        lastScoreMsg = getContext().getString(R.string.last_score);
        startPromptMsg = getContext().getString(R.string.start_game_prompt);
        congratulationsMsg = getContext().getString(R.string.congratulations);

        //Set colors
        textColor = contextResources.getColor(R.color.text, contextTheme);
        snakeColor = contextResources.getColor(R.color.snake, contextTheme);
        foodColor = contextResources.getColor(R.color.food, contextTheme);
        controllersColor = contextResources.getColor(R.color.controllers, contextTheme);
        int textColorLight = contextResources.getColor(R.color.textColorLight, contextTheme);
        int textColorDark = contextResources.getColor(R.color.textColorDark, contextTheme);

        //Theme switch
        //TODO: Read theme setting
        Theme theme = Theme.GRASS;
        switch (theme) {
            //noinspection ConstantConditions TODO: Delete noinspect after setting implementation
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

        //Set screen size
        screenX = size.x;
        screenY = size.y;

        display = Objects.requireNonNull(getSystemService(context, WindowManager.class)).getDefaultDisplay();

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
        //noinspection ConstantConditions TODO: Delete noinspect after setting implementation
        if (controlMode == ControlMode.BUTTONS) {
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
        if (!isRunning) {
            isRunning = true;
            thread = new Thread(this);
            thread.start();
        }
    }

    /**
     * Sets all variables needed for game start
     */
    private void startGame() {
        snake = new Snake(
                numBlocksWide / 2,
                numBlocksHigh / 2,
                Snake.Direction.RIGHT,
                maxBlocksOnScreen);

        spawnFood();
        score = 0;
        nextFrameTime = System.currentTimeMillis();
        isPlaying = true;
        fps = 7;
    }

    /**
     * Spawns food at a random location but not inside the snake
     */
    private void spawnFood() {
        Random random = new Random();
        int rx;
        int ry;

        List xs = Collections.singletonList(snake.bodyXs);
        List ys = Collections.singletonList(snake.bodyYs);

        do {
            rx = random.nextInt(numBlocksWide - 1) + 1;
            ry = random.nextInt(numBlocksHigh - 1) + 1;
        } while (xs.contains(rx) && ys.contains(ry));

        int x = rx * snakeBlockSize;
        int y = ry * snakeBlockSize;
        food.set(
                x,
                y,
                x + snakeBlockSize,
                y + snakeBlockSize);
    }

    /**
     * Evaluates if a screen update is required
     *
     * @return true if update is required
     */
    private boolean updateRequired() {
        if (nextFrameTime <= System.currentTimeMillis()) {
            long MILLIS_PER_SECOND = 1000;
            nextFrameTime = System.currentTimeMillis() + MILLIS_PER_SECOND / fps;
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
            died = true;
            snake = null;
        }
    }

    /**
     * Consumes food and spawns new
     */
    private void eatFood() {
        score++;
        if (score < (maxBlocksOnScreen - 1)) {
            spawnFood();
            snake.increaseSize();
            if (score % 4 == 0 && fps <= 20) {
                fps++;
            }
        } else {
            isPlaying = false;
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
                    && (snake.getHeadX() == snake.bodyXs[i])
                    && (snake.getHeadY() == snake.bodyYs[i])) {
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

            if (isPlaying) {
                drawGame(canvas, paint);
            } else {
                drawStart();
            }

            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawGame(Canvas canvas, Paint paint) {
        // Set controls color
        paint.setColor(controllersColor);

        // Draw controls if needed
        if (controlMode == ControlMode.BUTTONS && controls != null) {
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
            Rect snakeRect = new Rect(snake.bodyXs[i] * snakeBlockSize,
                    (snake.bodyYs[i] * snakeBlockSize),
                    (snake.bodyXs[i] * snakeBlockSize) + snakeBlockSize,
                    (snake.bodyYs[i] * snakeBlockSize) + snakeBlockSize);
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


    private void drawStart() {

        if (died) {

            TextView tvl = findViewById(R.id.your_score_label);
            if (tvl != null) tvl.setVisibility(VISIBLE);
            TextView tv = findViewById(R.id.your_score);
            if (tv != null) {
                tv.setVisibility(VISIBLE);
                tv.setText(score);
            }
            showPauseDialog((AppCompatActivity) context);
            died = false;
        }

    }

    public void showPauseDialog(final AppCompatActivity currentActivity) {
        if (isPlaying)
            pause();

        AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);

        LayoutInflater inflater = currentActivity.getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder
                .setView(inflater.inflate(R.layout.pause_menu, null))
                .setPositiveButton(R.string.play, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (snake == null) {
                            startGame();
                        }
                        resume();

                    }
                })
                .setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        currentActivity.finishAndRemoveTask();
                    }
                });

        runOnUiThread(() -> {
            builder.create().show();
        });

    }

    public static void runOnUiThread(Runnable runnable) {
        final Handler UIHandler = new Handler(Looper.getMainLooper());
        UIHandler.post(runnable);
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
            if (controlMode == ControlMode.BUTTONS && isPlaying) {
                int posX = Math.round(motionEvent.getX());
                int posY = Math.round(motionEvent.getY());

                if (controls.getButton(Controls.Button.LEFT).contains(posX, posY)) {
                    snake.setCurrentDirection(Snake.Direction.LEFT);
                } else if (controls.getButton(Controls.Button.UP).contains(posX, posY)) {
                    snake.setCurrentDirection(Snake.Direction.UP);
                } else if (controls.getButton(Controls.Button.RIGHT).contains(posX, posY)) {
                    snake.setCurrentDirection(Snake.Direction.RIGHT);
                } else if (controls.getButton(Controls.Button.DOWN).contains(posX, posY)) {
                    snake.setCurrentDirection(Snake.Direction.DOWN);
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
            if (controlMode == ControlMode.TILT && isPlaying) {
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
                                snake.setCurrentDirection(Snake.Direction.LEFT);
                            } else {
                                snake.setCurrentDirection(Snake.Direction.RIGHT);
                            }
                        } else {
                            if (y > 0) {
                                snake.setCurrentDirection(Snake.Direction.DOWN);
                            } else {
                                snake.setCurrentDirection(Snake.Direction.UP);
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
                                snake.setCurrentDirection(Snake.Direction.DOWN);
                            } else {
                                snake.setCurrentDirection(Snake.Direction.UP);
                            }
                        } else {
                            if (y > 0) {
                                snake.setCurrentDirection(Snake.Direction.RIGHT);
                            } else {
                                snake.setCurrentDirection(Snake.Direction.LEFT);
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
                                snake.setCurrentDirection(Snake.Direction.RIGHT);
                            } else {
                                snake.setCurrentDirection(Snake.Direction.LEFT);
                            }
                        } else {
                            if (y > 0) {
                                snake.setCurrentDirection(Snake.Direction.UP);
                            } else {
                                snake.setCurrentDirection(Snake.Direction.DOWN);
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
                                snake.setCurrentDirection(Snake.Direction.UP);
                            } else {
                                snake.setCurrentDirection(Snake.Direction.DOWN);
                            }
                        } else {
                            if (y > 0) {
                                snake.setCurrentDirection(Snake.Direction.LEFT);
                            } else {
                                snake.setCurrentDirection(Snake.Direction.RIGHT);
                            }
                        }
                        break;
                }
            }
        }
    }
}