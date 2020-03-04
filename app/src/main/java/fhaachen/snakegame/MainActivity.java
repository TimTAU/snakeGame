package fhaachen.snakegame;

import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;

import androidx.appcompat.app.AppCompatActivity;

import fhaachen.snakegame.ui.GameStage;

public class MainActivity extends AppCompatActivity {
    private GameStage gameStage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the pixel dimensions of the screen
        Display display = getWindowManager().getDefaultDisplay();

        // Initialize the result into a Point object
        Point size = new Point();
        display.getSize(size);

        // Create a game stage
        gameStage = new GameStage(this, size);

        // Set game stage as view
        setContentView(gameStage);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Start game stage thread
        gameStage.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop game stage thread
        gameStage.pause();
    }
}
