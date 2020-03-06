package fhaachen.snakegame.model

import fhaachen.snakegame.enums.SnakeDirection

class Snake(initX: Int, initY: Int, maxLength: Int) {
    /**
     * Returns all x coordinates where a part of the snake is present
     *
     * @return Array of x coordinates
     */
    val bodyXs: IntArray = IntArray(maxLength)

    /**
     * Returns all y coordinates where a part of the snake is present
     *
     * @return Array of y coordinates
     */
    val bodyYs: IntArray = IntArray(maxLength)

    /**
     * Returns the current length of the snake
     *
     * @return snake length
     */
    var snakeLength: Int
        private set

    /**
     * Direction the snake is moving to
     */
    private var currentDirection: SnakeDirection

    /**
     * Increases the snake length by one
     */
    fun increaseSize() {
        snakeLength++
    }

    /**
     * Moves the snake on step to the current direction
     */
    fun moveSnake() { // Move the body
        for (i in snakeLength downTo 1) {
            // Start at the back and move it to the position of the segment in front of it
            bodyXs[i] = bodyXs[i - 1]
            bodyYs[i] = bodyYs[i - 1]
            // Exclude the head because the head has nothing in front of it
        }
        when (getCurrentDirection()) {
            SnakeDirection.UP -> bodyYs[0]--
            SnakeDirection.RIGHT -> bodyXs[0]++
            SnakeDirection.DOWN -> bodyYs[0]++
            SnakeDirection.LEFT -> bodyXs[0]--
        }
    }

    // Getters / Setters =========================================================================
    /**
     * Returns the x coordinate of the snake head
     *
     * @return the x coordinate of the head
     */
    val headX: Int
        get() = getBodyX(0)

    /**
     * Returns the y coordinate of the snake head
     *
     * @return the y coordinate of the head
     */
    val headY: Int
        get() = getBodyY(0)

    fun getBodyX(index: Int): Int {
        return bodyXs[index]
    }

    fun getBodyY(index: Int): Int {
        return bodyYs[index]
    }

    /**
     * Sets the new direction. Prevents if the direction is opposite the current one
     *
     * @param direction new direction
     */
    private fun setCurrentDirection(direction: SnakeDirection) {
        // shouldn't change direction if new direction is opposite from previous
        when (direction) {
            SnakeDirection.UP -> if (getCurrentDirection() != SnakeDirection.DOWN) {
                currentDirection = direction
            }
            SnakeDirection.RIGHT -> if (getCurrentDirection() != SnakeDirection.LEFT) {
                currentDirection = direction
            }
            SnakeDirection.DOWN -> if (getCurrentDirection() != SnakeDirection.UP) {
                currentDirection = direction
            }
            SnakeDirection.LEFT -> if (getCurrentDirection() != SnakeDirection.RIGHT) {
                currentDirection = direction
            }
        }
    }

    /**
     * Sets the snake direction to left
     */
    fun setDirectionLeft() {
        setCurrentDirection(SnakeDirection.LEFT)
    }

    /**
     * Sets the snake direction to up
     */
    fun setDirectionUp() {
        setCurrentDirection(SnakeDirection.UP)
    }

    /**
     * Sets the snake direction to right
     */
    fun setDirectionRight() {
        setCurrentDirection(SnakeDirection.RIGHT)
    }

    /**
     * Sets the snake direction to down
     */
    fun setDirectionDown() {
        setCurrentDirection(SnakeDirection.DOWN)
    }

    /**
     * Returns the direction in which the snake currently points
     *
     * @return current direction
     */
    private fun getCurrentDirection(): SnakeDirection {
        return currentDirection
    }

    /**
     * Default constructor to initialize a new Snake
     * x value of start point
     * y value of start point
     * Max length that can be reached before the screen is full
     */
    init {
        bodyXs[0] = initX
        bodyYs[0] = initY
        currentDirection = SnakeDirection.RIGHT
        snakeLength = 0
    }
}