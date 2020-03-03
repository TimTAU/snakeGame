package snakegame.model;

import android.graphics.Rect;

public class Controls {
    public enum Button {
        UP,
        RIGHT,
        DOWN,
        LEFT
    }

    private final Rect[] buttons;

    public Controls(int posX, int posY, int buttonSize) {
        buttons = new Rect[4];

        // Left
        buttons[0] = new Rect(
                posX,
                posY + buttonSize,
                posX + buttonSize,
                posY + (buttonSize * 2));

        // Up
        buttons[1] = new Rect(
                posX + buttonSize,
                posY,
                posX + (buttonSize * 2),
                posY + buttonSize);

        // Right
        buttons[2] = new Rect(
                posX + (buttonSize * 2),
                posY + buttonSize,
                posX + (buttonSize * 3),
                posY + (buttonSize * 2));

        // Down
        buttons[3] = new Rect(
                posX + buttonSize,
                posY + (buttonSize * 2),
                posX + (buttonSize * 2),
                posY + (buttonSize * 3));
    }

    public Rect getButton(Button button) {
        Rect btn;
        switch (button) {
            case LEFT:
                btn = buttons[0];
                break;
            case UP:
                btn = buttons[1];
                break;
            case RIGHT:
                btn = buttons[2];
                break;
            default:
                btn = buttons[3];
                break;
        }
        return btn;
    }

    public Rect[] getButtons() {
        return buttons;
    }
}
