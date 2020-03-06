package fhaachen.snakegame.helper

import fhaachen.snakegame.model.Snake

object SnakeHelper {
    /**
     * Evaluates if the player is dead
     *
     * @return true if death is detected
     */
    fun detectDeath(snake: Snake, numBlocksWide: Int, numBlocksHigh: Int): Boolean {
        // Hit the screen edge
        if (edgeHitDetection(snake, numBlocksWide, numBlocksHigh)) {
            return true
        }
        // Hit itself
        for (i in snake.snakeLength downTo 1) {
            if (i > 4
                    && snake.headX == snake.getBodyX(i)
                    && snake.headY == snake.getBodyY(i)) {
                return true
            }
        }
        // Hit nothing
        return false
    }

    private fun edgeHitDetection(snake: Snake, width: Int, height: Int): Boolean {
        return if (width > height) {
            snake.headX == -1 || snake.headX >= width + 1 || snake.headY == -1 || snake.headY == height + 1
        } else {
            snake.headX == -1 || snake.headX >= width || snake.headY == -1 || snake.headY == height + 1
        }
    }
}