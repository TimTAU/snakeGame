package fhaachen.snakegame.model

import android.graphics.Rect
import fhaachen.snakegame.enums.ControlButton

class Controls(posX: Int, posY: Int, buttonSize: Int) {


    val buttons: Array<Rect?> = arrayOfNulls(4)

    fun getButton(button: ControlButton): Rect? {
        return when (button) {
            ControlButton.LEFT -> buttons[0]
            ControlButton.UP -> buttons[1]
            ControlButton.RIGHT -> buttons[2]
            ControlButton.DOWN -> buttons[3]
        }
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