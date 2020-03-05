package fhaachen.snakegame.helper

import fhaachen.snakegame.model.Snake

object GameHelper {
    /**
     * Evaluates if the player is dead
     *
     * @return true if death is detected
     */
    fun detectDeath(snake: Snake, numBlocksWide: Int, numBlocksHigh: Int): Boolean {
        // Hit the screen edge
        if (snake.headX == -1 || snake.headX >= numBlocksWide + 1 || snake.headY == -1 || snake.headY == numBlocksHigh + 1) {
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

    //FIXME
    /*
     * Spawns food at a random location but not inside the snake
    fun spawnFood(numBlocksWide: Int, numBlocksHigh: Int, snake: Snake, snakeBlockSize: Int): Rect {
        val random = Random()
        var randomX: Int
        var randomY: Int
        do {
            randomX = random.nextInt(numBlocksWide - 1) + 1
            randomY = random.nextInt(numBlocksHigh - 1) + 1
        } while (positionInsideSnake(randomX, randomY, snake))
        val x = randomX * snakeBlockSize
        val y = randomY * snakeBlockSize
        val food = Rect()
        food[x, y, x + snakeBlockSize] = y + snakeBlockSize
        return food
    }

     * Checks if the given coordinates are included in the snake
     *
     * @param x coordinate to be checked
     * @param y coordinate to be checked
     * @return true if the coordinate is included
    private fun positionInsideSnake(x: Int, y: Int, snake: Snake): Boolean {
        return IntStream.of(*snake.bodyXs).anyMatch { snakeX: Int -> snakeX == x } && IntStream.of(*snake.bodyYs).anyMatch { snakeY: Int -> snakeY == y }
    }*/
}