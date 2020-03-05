package fhaachen.snakegame.model

class Snake(initX: Int, initY: Int, maxLength: Int) {
    private enum class Direction {
        UP, RIGHT, DOWN, LEFT
    }

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
    private var currentDirection: Direction

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
            Direction.UP -> bodyYs[0]--
            Direction.RIGHT -> bodyXs[0]++
            Direction.DOWN -> bodyYs[0]++
            Direction.LEFT -> bodyXs[0]--
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
    private fun setCurrentDirection(direction: Direction) {
        // shouldn't change direction if new direction is opposite from previous
        when (direction) {
            Direction.UP -> if (getCurrentDirection() != Direction.DOWN) {
                currentDirection = direction
            }
            Direction.RIGHT -> if (getCurrentDirection() != Direction.LEFT) {
                currentDirection = direction
            }
            Direction.DOWN -> if (getCurrentDirection() != Direction.UP) {
                currentDirection = direction
            }
            Direction.LEFT -> if (getCurrentDirection() != Direction.RIGHT) {
                currentDirection = direction
            }
        }
    }

    /**
     * Sets the snake direction to left
     */
    fun setDirectionLeft() {
        setCurrentDirection(Direction.LEFT)
    }

    /**
     * Sets the snake direction to up
     */
    fun setDirectionUp() {
        setCurrentDirection(Direction.UP)
    }

    /**
     * Sets the snake direction to right
     */
    fun setDirectionRight() {
        setCurrentDirection(Direction.RIGHT)
    }

    /**
     * Sets the snake direction to down
     */
    fun setDirectionDown() {
        setCurrentDirection(Direction.DOWN)
    }

    /**
     * Returns the direction in which the snake currently points
     *
     * @return current direction
     */
    private fun getCurrentDirection(): Direction {
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
        currentDirection = Direction.RIGHT
        snakeLength = 0
    }
}