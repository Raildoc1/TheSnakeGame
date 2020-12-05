package ru.gaiduk.snake.game;

import me.ippolitov.fit.snakes.SnakesProto;
import ru.gaiduk.snake.math.Vector2;
import ru.gaiduk.snake.view.IUpdatable;

import java.util.*;

public class Board {

    private ArrayList<SnakesProto.GamePlayer> gamePlayers;
    private ArrayList<Snake> snakes;
    private ArrayList<Vector2> food;

    private Snake mySnake;
    private boolean lost = false;

    private Random random;

    private ArrayList<IUpdatable> toUpdate;

    SnakesProto.GameConfig gameConfig;

    private Timer timer;
    private TimerTask timerTask;

    private int delayMillis = 500;

    public int getWidth() { return gameConfig.getWidth(); }
    public int getHeight() {
        return gameConfig.getHeight();
    }

    public boolean isLost() { return lost; }

    public ArrayList<Vector2> getFood() { return (ArrayList<Vector2>) food.clone(); }
    public ArrayList<Snake> getSnakes() { return (ArrayList<Snake>) snakes.clone(); }
    public Snake getSnake() { return mySnake; };

    public Board (SnakesProto.GameConfig gameConfig) {
        this.gameConfig = gameConfig;
        toUpdate = new ArrayList<>();
        snakes = new ArrayList<>();
        food = new ArrayList<>();
        random = new Random();
        gamePlayers = new ArrayList<SnakesProto.GamePlayer>();
    }

    public void start() {

        // TEST
        snakes.add(new Snake(5, 3, getHeight(), getWidth()));
        snakes.add(new Snake(7, 15, getHeight(), getWidth()));

        mySnake = new Snake(15, 10, getHeight(), getWidth());

        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                update();
            }
        };

        timer.schedule(timerTask, delayMillis, gameConfig.getStateDelayMs());
    }

    public void registerUpdateCallback(IUpdatable updatable) {
        toUpdate.add(updatable);
    }

    public SnakesProto.GamePlayers getGamePlayers() {

        SnakesProto.GamePlayers.Builder builder = SnakesProto.GamePlayers.newBuilder();

        for (int i = 0; i < gamePlayers.size(); i++) {
            builder.setPlayers(i, gamePlayers.get(i));
        }

        return builder.build();

    }

    private void update() {

        // Move Snakes
        for (var snake : snakes) {
            snake.move();
        }

        if(!lost) {
            mySnake.move();
        }

        // TODO: check snakes intersect

        // Create new food
        if(food.size() < gameConfig.getFoodStatic() + gameConfig.getFoodPerPlayer() * (snakes.size() + 1)) {
            var newFoodEntity = Vector2.clamp(Vector2.Random(), getWidth(), getHeight());
            food.add(newFoodEntity);
        }

        // Eat food
        Iterator<Vector2> foodIterator = food.iterator();

        while (foodIterator.hasNext()) {

            var f = foodIterator.next();

            if(!lost && mySnake.eatOn(f)) {
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
            var snake = snakeIterator.next();
            if(snake.checkSelfCollision()) {
                Snake2Food(snake);
                snakeIterator.remove();
            }
        }

        if(!lost && mySnake.checkSelfCollision()) {
            lost = true;
            Snake2Food(mySnake);
        }

        // Update
        for (var updatable : toUpdate) {
            updatable.Update();
        }
    }

    private void Snake2Food(Snake snake) {
        for (var segment : snake.getSegments()) {
            if (isDeadFoodMustBeCreated()) {
                food.add(segment);
            }
        }
        snake.die();
    }

    public boolean isDeadFoodMustBeCreated() {
        float f = random.nextFloat();
        return f < gameConfig.getDeadFoodProb();
    }

    public void changeDirection(int x, int y) {
        if(lost) {
            return;
        }
        mySnake.ChangeDirection(new Vector2(sgn(x), sgn(y)));
    }

    public static int sgn(int x) {
        if(x == 0) return 0;
        if(x > 0) return 1;
        return -1;
    }

}
