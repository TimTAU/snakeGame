package snakegame;

import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;

import snakegame.ui.GameStage;

public class MainActivity extends AppCompatActivity {

    private GameStage _gameStage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the pixel dimensions of the screen
        Display display = getWindowManager().getDefaultDisplay();

        // Initialize the result into a Point object
        Point size = new Point();
        display.getSize(size);

        // Create a game stage
        _gameStage = new GameStage(this, size);

        // Set game stage as view
        setContentView(_gameStage);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Start game stage thread
        _gameStage.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop game stage thread
        _gameStage.pause();
    }
}
