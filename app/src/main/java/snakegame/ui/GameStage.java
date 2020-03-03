package snakegame.ui;

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

public class GameStage extends SurfaceView implements Runnable {

    private final long MILLIS_PER_SECOND = 1000;
    private final int NUM_BLOCKS_WIDE = 40;
    private long FPS = 10;
    private final SurfaceHolder _surfaceHolder;
    private final Paint _paint;
    private Thread _thread = null;
    private Canvas _canvas;
    private volatile boolean _isRunning;
    private volatile boolean _isPlaying;
    private int _screenX;
    private int _screenY;
    private int _snakeBlockSize;
    private int _controlButtonSize;
    private int _numBlocksHigh;
    private long _nextFrameTime;
    private int _maxBlocksOnScreen;

    private Snake _snake;
    private Controls _controls;
    private Rect _food;
    private int _score;

    private String _currentScoreMsg;
    private String _lastScoreMsg;
    private String _startPromptMsg;
    private String _congratulationsMsg;

    private int _backgroundColor;
    private int _textColor;
    private int _snakeColor;
    private int _foodColor;
    private int _controllersColor;

    public GameStage(Context context, Point size) {
        super(context);

        _currentScoreMsg = getContext().getString(R.string.current_score);
        _lastScoreMsg = getContext().getString(R.string.last_score);
        _startPromptMsg = getContext().getString(R.string.start_game_prompt);
        _congratulationsMsg = getContext().getString(R.string.congratulations);

        _backgroundColor = getContext().getResources().getColor(R.color.background);
        _textColor = getContext().getResources().getColor(R.color.text);
        _snakeColor = getContext().getResources().getColor(R.color.snake);
        _foodColor = getContext().getResources().getColor(R.color.food);
        _controllersColor = getContext().getResources().getColor(R.color.controllers);

        _screenX = size.x;
        _screenY = size.y;

        _surfaceHolder = getHolder();
        _paint = new Paint();

        _snakeBlockSize = _screenX / NUM_BLOCKS_WIDE;

        _numBlocksHigh = _screenY / _snakeBlockSize;

        _maxBlocksOnScreen = NUM_BLOCKS_WIDE * _numBlocksHigh;

        _controlButtonSize = _snakeBlockSize * 3;

        int controlsY = _screenY - (_controlButtonSize * 3) - _snakeBlockSize;

        _controls = new Controls(_snakeBlockSize, controlsY, _controlButtonSize);

        _food = new Rect();

        _nextFrameTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        while (_isRunning) {
            if (updateRequired()) {
                if (_isPlaying) {
                    update();
                }
                draw();
            }
        }
    }

    public void pause() {
        _isRunning = false;
        try {
            _thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void resume() {
        _isRunning = true;
        _thread = new Thread(this);
        _thread.start();
    }

    private void startGame() {
        _snake = new Snake(
                NUM_BLOCKS_WIDE / 2,
                _numBlocksHigh / 2,
                Snake.Direction.RIGHT,
                _maxBlocksOnScreen);

        spawnFood();
        _score = 0;
        _nextFrameTime = System.currentTimeMillis();
        _isPlaying = true;
    }

    private void spawnFood() {
        Random random = new Random();
        int rx;
        int ry;

        List xs = Collections.singletonList(_snake.bodyYs);
        List ys = Collections.singletonList(_snake.bodyYs);

        do {
            rx = random.nextInt(NUM_BLOCKS_WIDE - 1) + 1;
            ry = random.nextInt(_numBlocksHigh - 1) + 1;
        } while (xs.contains(rx) && ys.contains(ry));

        int x = rx * _snakeBlockSize;
        int y = ry * _snakeBlockSize;
        _food.set(
                x,
                y,
                x + _snakeBlockSize,
                y + _snakeBlockSize);
    }

    public boolean updateRequired() {
        if (_nextFrameTime <= System.currentTimeMillis()) {
            _nextFrameTime = System.currentTimeMillis() + MILLIS_PER_SECOND / FPS;
            return true;
        }
        return false;
    }

    public void update() {
        if ((_snake.getHeadX() * _snakeBlockSize) == _food.left && (_snake.getHeadY() * _snakeBlockSize) == _food.top) {
            eatFood();
        }

        _snake.moveSnake();

        if (detectDeath()) {
            _isPlaying = false;
        }
    }

    private void eatFood() {
        _score++;
        if (_score < (_maxBlocksOnScreen - 1)) {
            spawnFood();
            _snake.increaseSize();
            if (_score % 2 == 0) {
                FPS++;
            }
        } else {
            _isPlaying = false;
        }
    }

    private boolean detectDeath() {
        boolean dead = false;

        // Hit the screen edge
        if (_snake.getHeadX() == -1) dead = true;
        if (_snake.getHeadX() >= NUM_BLOCKS_WIDE + 1) dead = true;
        if (_snake.getHeadY() == -1) dead = true;
        if (_snake.getHeadY() == _numBlocksHigh + 1) dead = true;

        // Hit itself
        for (int i = _snake.getSnakeLength(); i > 0; i--) {
            if ((i > 4)
                    && (_snake.getHeadX() == _snake.bodyXs[i])
                    && (_snake.getHeadY() == _snake.bodyYs[i])) {
                dead = true;
            }
        }
        return dead;
    }

    private void draw() {
        if (_surfaceHolder.getSurface().isValid()) {
            _canvas = _surfaceHolder.lockCanvas();
            // Set background color
            _canvas.drawColor(_backgroundColor);

            if (_isPlaying) {
                drawGame(_canvas, _paint);
            } else {
                drawStart(_canvas, _paint);
            }

            _surfaceHolder.unlockCanvasAndPost(_canvas);
        }
    }

    private void drawGame(Canvas canvas, Paint paint) {
        // Set controls color
        paint.setColor(_controllersColor);

        // Draw controls
        for (Rect control : _controls.getButtons()) {
            canvas.drawRect(
                    control.left,
                    control.top,
                    control.right,
                    control.bottom,
                    paint);
        }

        // Set food color
        paint.setColor(_foodColor);
        canvas.drawRect(
                _food.left,
                _food.top,
                _food.right,
                _food.bottom,
                paint);

        // Set snake color
        paint.setColor(_snakeColor);

        // Draw the snake
        for (int i = 0; i < _snake.getSnakeLength() + 1; i++) {
            canvas.drawRect(_snake.bodyXs[i] * _snakeBlockSize,
                    (_snake.bodyYs[i] * _snakeBlockSize),
                    (_snake.bodyXs[i] * _snakeBlockSize) + _snakeBlockSize,
                    (_snake.bodyYs[i] * _snakeBlockSize) + _snakeBlockSize,
                    paint);
        }

        // Scale the HUD text
        paint.setTextSize(70);
        canvas.drawText(String.format(_currentScoreMsg, _score), 10, 60, paint);
    }

    private void drawStart(Canvas canvas, Paint paint) {
        // Set text color
        paint.setColor(_textColor);
        paint.setTextSize(70);

        int halfScreen = _screenX / 2;
        int halfText;
        if (_score > 0) {
            String msgScore = String.format(_lastScoreMsg, _score);
            float scoreMeasure = paint.measureText(msgScore);

            halfText = Math.round(scoreMeasure / 2);

            canvas.drawText(
                    msgScore,
                    halfScreen - halfText,
                    (_screenY / 2) - 100, paint);
        }

        if (_score >= (_maxBlocksOnScreen - 1)) {
            float congratsMeasure = paint.measureText(_congratulationsMsg);

            halfText = Math.round(congratsMeasure / 2);

            canvas.drawText(
                    _congratulationsMsg,
                    halfScreen - halfText,
                    (_screenY / 2) - 200, paint);
        }

        float startMeasure = paint.measureText(_startPromptMsg);

        halfText = Math.round(startMeasure / 2);

        canvas.drawText(_startPromptMsg, halfScreen - halfText, _screenY / 2, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            if (_isPlaying) {
                int posX = Math.round(motionEvent.getX());
                int posY = Math.round(motionEvent.getY());

                if (_controls.getButton(Controls.Button.LEFT).contains(posX, posY)) {
                    _snake.setCurrentDirection(Snake.Direction.LEFT);
                } else if (_controls.getButton(Controls.Button.UP).contains(posX, posY)) {
                    _snake.setCurrentDirection(Snake.Direction.UP);
                } else if (_controls.getButton(Controls.Button.RIGHT).contains(posX, posY)) {
                    _snake.setCurrentDirection(Snake.Direction.RIGHT);
                } else if (_controls.getButton(Controls.Button.DOWN).contains(posX, posY)) {
                    _snake.setCurrentDirection(Snake.Direction.DOWN);
                }
            } else {
                startGame();
            }
        }

        return super.onTouchEvent(motionEvent);
    }
}
