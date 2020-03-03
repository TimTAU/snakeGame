package snakegame.model;

public class Snake {

    public enum Direction {
        UP,
        RIGHT,
        DOWN,
        LEFT
    }

    public int[] bodyXs;
    public int[] bodyYs;
    private int _snakeLength;

    private Direction _currentDirection;

    public Snake(int initX, int initY, Direction direction, int maxLength) {

        bodyXs = new int[maxLength];
        bodyYs = new int[maxLength];
        bodyXs[0] = initX;
        bodyYs[0] = initY;
        _currentDirection = direction;
        _snakeLength = 0;
    }

    public void increaseSize() {
        _snakeLength++;
    }

    public void moveSnake(){

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
    public int getHeadX() {
        return bodyXs[0];
    }

    public int getHeadY() {
        return bodyYs[0];
    }

    public int getSnakeLength() {
        return _snakeLength;
    }

    public void setCurrentDirection(Direction direction) {

        // shouldn't change direction if new direction is opposite from previews
        switch(direction){
            case UP:
                if(getCurrentDirection() != Direction.DOWN) {
                    _currentDirection = direction;
                }
                break;
            case RIGHT:
                if(getCurrentDirection() != Direction.LEFT) {
                    _currentDirection = direction;
                }
                break;
            case DOWN:
                if(getCurrentDirection() != Direction.UP) {
                    _currentDirection = direction;
                }
                break;
            case LEFT:
                if(getCurrentDirection() != Direction.RIGHT) {
                    _currentDirection = direction;
                }
                break;
        }
    }

    private Direction getCurrentDirection() {
        return _currentDirection;
    }
}
