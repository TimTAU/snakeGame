package snakegame.model;

import android.graphics.Rect;

public class Controls {

    public enum Button {
        UP,
        RIGHT,
        DOWN,
        LEFT
    }

    private int _posX;
    private int _posY;

    private Rect[] _buttons;

    public Controls(int posX, int posY, int buttonSize) {

        _posX = posX;
        _posY = posY;

        _buttons = new Rect[4];

        // Left
        _buttons[0] = new Rect(
                _posX,
                _posY + buttonSize,
                _posX + buttonSize,
                _posY + (buttonSize * 2));

        // Up
        _buttons[1] = new Rect(
                _posX + buttonSize,
                _posY,
                _posX + (buttonSize * 2),
                _posY + buttonSize);

        // Right
        _buttons[2] = new Rect(
                _posX + (buttonSize * 2),
                _posY + buttonSize,
                _posX + (buttonSize * 3),
                _posY + (buttonSize * 2));

        // Down
        _buttons[3] = new Rect(
                _posX + buttonSize,
                _posY + (buttonSize * 2),
                _posX + (buttonSize * 2),
                _posY + (buttonSize * 3));
    }

    public Rect getButton(Button button) {

        Rect btn;
        switch (button) {
            case LEFT:
                btn = _buttons[0];
                break;
            case UP:
                btn = _buttons[1];
                break;
            case RIGHT:
                btn = _buttons[2];
                break;
            default:
                btn = _buttons[3];
                break;
        }

        return btn;
    }

    public Rect[] getButtons() {
        return _buttons;
    }
}
