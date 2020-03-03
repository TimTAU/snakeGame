package snakegame.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import br.com.mxel.snakegame.R;
import snakegame.model.Controls;
import snakegame.model.Snake;

@SuppressLint("ViewConstructor")
public class GameStage extends SurfaceView implements Runnable {
    private int numBlocksWide = 40;
    private long fps = 7;
    private final SurfaceHolder surfaceHolder;
    private final Paint paint;
    private Thread thread = null;
    private volatile boolean isRunning;
    private volatile boolean isPlaying;
    private int screenX;
    private int screenY;
    private int snakeBlockSize;
    private int numBlocksHigh;
    private long nextFrameTime;
    private int maxBlocksOnScreen;

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
    private int backgroundColor;
    private int textColor;
    private int snakeColor;
    private int foodColor;
    private int controllersColor;

    public GameStage(Context context, Point size) {
        super(context);

        //Set messages
        currentScoreMsg = getContext().getString(R.string.current_score);
        lastScoreMsg = getContext().getString(R.string.last_score);
        startPromptMsg = getContext().getString(R.string.start_game_prompt);
        congratulationsMsg = getContext().getString(R.string.congratulations);

        //Set colors
        backgroundColor = getContext().getResources().getColor(R.color.background);
        textColor = getContext().getResources().getColor(R.color.text);
        snakeColor = getContext().getResources().getColor(R.color.snake);
        foodColor = getContext().getResources().getColor(R.color.food);
        controllersColor = getContext().getResources().getColor(R.color.controllers);

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

        int controlButtonSize = snakeBlockSize * 3;
        int controlsY = screenY - (controlButtonSize * 3) - snakeBlockSize;
        controls = new Controls(snakeBlockSize, controlsY, controlButtonSize);

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
        //TODO: Timer on restart
        isRunning = true;
        thread = new Thread(this);
        thread.start();
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
    public boolean updateRequired() {
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
    public void update() {
        if ((snake.getHeadX() * snakeBlockSize) == food.left && (snake.getHeadY() * snakeBlockSize) == food.top) {
            eatFood();
        }

        snake.moveSnake();

        if (detectDeath()) {
            isPlaying = false;
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
            // Set background color
            canvas.drawColor(backgroundColor);

            if (isPlaying) {
                drawGame(canvas, paint);
            } else {
                drawStart(canvas, paint);
            }

            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawGame(Canvas canvas, Paint paint) {
        // Set controls color
        paint.setColor(controllersColor);

        // Draw controls
        for (Rect control : controls.getButtons()) {
            canvas.drawRect(
                    control.left,
                    control.top,
                    control.right,
                    control.bottom,
                    paint);
        }

        // Set food color
        paint.setColor(foodColor);
        canvas.drawRect(
                food.left,
                food.top,
                food.right,
                food.bottom,
                paint);

        // Set snake color
        paint.setColor(snakeColor);

        // Draw the snake
        for (int i = 0; i < snake.getSnakeLength() + 1; i++) {
            canvas.drawRect(snake.bodyXs[i] * snakeBlockSize,
                    (snake.bodyYs[i] * snakeBlockSize),
                    (snake.bodyXs[i] * snakeBlockSize) + snakeBlockSize,
                    (snake.bodyYs[i] * snakeBlockSize) + snakeBlockSize,
                    paint);
        }

        // Scale the HUD text
        paint.setTextSize(70);
        canvas.drawText(String.format(currentScoreMsg, score), 10, 60, paint);
    }

    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    private void drawStart(Canvas canvas, Paint paint) {
        // Set text color
        paint.setColor(textColor);
        paint.setTextSize(70);

        int halfScreen = screenX / 2;
        int halfText;
        if (score > 0) {
            String msgScore = String.format(lastScoreMsg, score);
            float scoreMeasure = paint.measureText(msgScore);

            halfText = Math.round(scoreMeasure / 2);

            canvas.drawText(
                    msgScore,
                    halfScreen - halfText,
                    (screenY / 2) - 100, paint);
        }

        if (score >= (maxBlocksOnScreen - 1)) {
            float congratsMeasure = paint.measureText(congratulationsMsg);

            halfText = Math.round(congratsMeasure / 2);

            canvas.drawText(
                    congratulationsMsg,
                    halfScreen - halfText,
                    (screenY / 2) - 200, paint);
        }

        float startMeasure = paint.measureText(startPromptMsg);

        halfText = Math.round(startMeasure / 2);

        canvas.drawText(startPromptMsg, halfScreen - halfText, screenY / 2, paint);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            if (isPlaying) {
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
}