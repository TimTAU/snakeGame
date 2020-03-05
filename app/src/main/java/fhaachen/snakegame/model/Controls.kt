package fhaachen.snakegame.model

import android.graphics.Rect

class Controls(posX: Int, posY: Int, buttonSize: Int) {
    enum class Mode {
        BUTTONS, TILT, GESTURES
    }

    enum class Button {
        UP, RIGHT, DOWN, LEFT
    }

    val buttons: Array<Rect?> = arrayOfNulls(4)
    fun getButton(button: Button?): Rect? {
        when (button) {
            Button.LEFT -> return buttons[0]
            Button.UP -> return buttons[1]
            Button.RIGHT -> return buttons[2]
            Button.DOWN -> return buttons[3]
        }
        throw RuntimeException("Button couldn't be evaluated")
    }

    init {
        // Left
        buttons[0] = Rect(
                posX,
                posY + buttonSize,
                posX + buttonSize,
                posY + buttonSize * 2)
        // Up
        buttons[1] = Rect(
                posX + buttonSize,
                posY,
                posX + buttonSize * 2,
                posY + buttonSize)
        // Right
        buttons[2] = Rect(
                posX + buttonSize * 2,
                posY + buttonSize,
                posX + buttonSize * 3,
                posY + buttonSize * 2)
        // Down
        buttons[3] = Rect(
                posX + buttonSize,
                posY + buttonSize * 2,
                posX + buttonSize * 2,
                posY + buttonSize * 3)
    }
}