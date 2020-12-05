package ru.gaiduk.snake.game;

import ru.gaiduk.snake.math.Vector2;
import ru.gaiduk.snake.view.IUpdatable;

import java.awt.image.AreaAveragingScaleFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

public class Board {

    private ArrayList<Snake> snakes;
    private ArrayList<Vector2> food;

    private Snake mySnake;
    private boolean lost = false;

    private ArrayList<IUpdatable> toUpdate;

    private int width = 30;
    private int height = 30;

    private int foodAmount = 300;

    private Timer timer;
    private TimerTask timerTask;

    private int deltaTimeMillis;
    private int targetFPS = 5;
    private int delayMillis = 500;

    public int getWidth() { return width; }
    public int getHeight() {
        return height;
    }

    public boolean isLost() { return lost; }

    public ArrayList<Vector2> getFood() { return (ArrayList<Vector2>) food.clone(); }
    public ArrayList<Snake> getSnakes() { return (ArrayList<Snake>) snakes.clone(); }
    public Snake getSnake() { return mySnake; };

    public Board () {
        toUpdate = new ArrayList<>();
        snakes = new ArrayList<>();
        food = new ArrayList<>();
    }

    public void start() {

        // TEST
        snakes.add(new Snake(5, 3, height, width));
        snakes.add(new Snake(7, 15, height, width));

        mySnake = new Snake(15, 10, height, width);

        init();

        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                update();
            }
        };

        deltaTimeMillis = 1000/targetFPS;
        timer.schedule(timerTask, delayMillis, deltaTimeMillis);
    }

    public void registerUpdateCallback(IUpdatable updatable) {
        toUpdate.add(updatable);
    }

    private void init() {

    }

    private void update() {

        // Move Snakes
        for (var snake : snakes) {
            snake.move();
        }

        mySnake.move();

        // TODO: check snakes intersect

        // Create new food
        if(food.size() < foodAmount) {
            var newFoodEntity = Vector2.clamp(Vector2.Random(), width, height);
            food.add(newFoodEntity);
        }

        // Eat food
        Iterator<Vector2> foodIterator = food.iterator();

        while (foodIterator.hasNext()) {

            var f = foodIterator.next();

            if(mySnake.eatOn(f)) {
                foodIterator.remove();
                break;
            }
        }

        for (var snake : snakes) {
            foodIterator = food.iterator();
            while (foodIterator.hasNext()) {
                var f = foodIterator.next();
                if(snake.eatOn(f)) {
                    foodIterator.remove();
                    break;
                }
            }
        }


        // Rotate snakes
        for (var snake : snakes) {
            snake.ChangeDirection(Vector2.RandomDirection());
        }

        // Check self collision

        Iterator<Snake> snakeIterator = snakes.iterator();

        while (snakeIterator.hasNext()) {
            if(snakeIterator.next().checkSelfCollision()) {
                snakeIterator.remove();
            }
        }

        if(mySnake.checkSelfCollision()) {
            lost = true;
        }

        // Update
        for (var updatable : toUpdate) {
            updatable.Update();
        }
    }

    public void changeDirection(int x, int y) {
        mySnake.ChangeDirection(new Vector2(sgn(x), sgn(y)));
    }

    public static int sgn(int x) {
        if(x == 0) return 0;
        if(x > 0) return 1;
        return -1;
    }

}
