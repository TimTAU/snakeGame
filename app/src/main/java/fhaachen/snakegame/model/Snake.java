package fhaachen.snakegame.model;

public class Snake {
    private enum Direction {
        UP,
        RIGHT,
        DOWN,
        LEFT
    }

    private final int[] bodyXs;
    private final int[] bodyYs;
    private int snakeLength;

    private Direction currentDirection;

    /**
     * Default constructor to initialize a new Snake
     *
     * @param initX     x value of start point
     * @param initY     y value of start point
     * @param maxLength Max length that can be reached before the screen is full
     */
    public Snake(int initX, int initY, int maxLength) {
        bodyXs = new int[maxLength];
        bodyYs = new int[maxLength];
        bodyXs[0] = initX;
        bodyYs[0] = initY;
        currentDirection = Direction.RIGHT;
        snakeLength = 0;
    }

    /**
     * Increases the snake length by one
     */
    public void increaseSize() {
        snakeLength++;
    }

    /**
     * Moves the snake on step to the current direction
     */
    public void moveSnake() {
        // Move the body
        for (int i = getSnakeLength(); i > 0; --i) {
            // Start at the back and move it
            // to the position of the segment in front of it
            bodyXs[i] = bodyXs[i - 1];
            bodyYs[i] = bodyYs[i - 1];

            // Exclude the head because
            // the head has nothing in front of it
        }

        // Move the head in the appropriate direction
        switch (getCurrentDirection()) {
            case UP:
                bodyYs[0]--;
                break;

            case RIGHT:
                bodyXs[0]++;
                break;

            case DOWN:
                bodyYs[0]++;
                break;

            case LEFT:
                bodyXs[0]--;
                break;
        }
    }

    // Getters / Setters =========================================================================

    /**
     * Returns the x coordinate of the snake head
     *
     * @return the x coordinate of the head
     */
    public int getHeadX() {
        return getBodyX(0);
    }

    /**
     * Returns the y coordinate of the snake head
     *
     * @return the y coordinate of the head
     */
    public int getHeadY() {
        return getBodyY(0);
    }

    /**
     * Returns all x coordinates where a part of the snake is present
     *
     * @return Array of x coordinates
     */
    public int[] getBodyXs() {
        return bodyXs;
    }

    /**
     * Returns all y coordinates where a part of the snake is present
     *
     * @return Array of y coordinates
     */
    public int[] getBodyYs() {
        return bodyYs;
    }

    public int getBodyX(int index) {
        return bodyXs[index];
    }

    public int getBodyY(int index) {
        return bodyYs[index];
    }

    /**
     * Returns the current length of the snake
     *
     * @return snake length
     */
    public int getSnakeLength() {
        return snakeLength;
    }

    /**
     * Sets the new direction. Prevents if the direction is opposite the current one
     *
     * @param direction new direction
     */
    private void setCurrentDirection(Direction direction) {
        // shouldn't change direction if new direction is opposite from previous
        switch (direction) {
            case UP:
                if (getCurrentDirection() != Direction.DOWN) {
                    currentDirection = direction;
                }
                break;
            case RIGHT:
                if (getCurrentDirection() != Direction.LEFT) {
                    currentDirection = direction;
                }
                break;
            case DOWN:
                if (getCurrentDirection() != Direction.UP) {
                    currentDirection = direction;
                }
                break;
            case LEFT:
                if (getCurrentDirection() != Direction.RIGHT) {
                    currentDirection = direction;
                }
                break;
        }
    }

    /**
     * Sets the snake direction to left
     */
    public void setDirectionLeft() {
        setCurrentDirection(Direction.LEFT);
    }

    /**
     * Sets the snake direction to up
     */
    public void setDirectionUp() {
        setCurrentDirection(Direction.UP);
    }

    /**
     * Sets the snake direction to right
     */
    public void setDirectionRight() {
        setCurrentDirection(Direction.RIGHT);
    }

    /**
     * Sets the snake direction to down
     */
    public void setDirectionDown() {
        setCurrentDirection(Direction.DOWN);
    }

    /**
     * Returns the direction in which the snake currently points
     *
     * @return current direction
     */
    private Direction getCurrentDirection() {
        return currentDirection;
    }
}
