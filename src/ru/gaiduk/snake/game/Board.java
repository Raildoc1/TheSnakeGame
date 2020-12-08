package ru.gaiduk.snake.game;

import me.ippolitov.fit.snakes.SnakesProto;
import ru.gaiduk.snake.math.Vector2;
import ru.gaiduk.snake.view.IUpdatable;

import java.util.*;

public class Board {

    private ArrayList<SnakesProto.GamePlayer> gamePlayers;
    private ArrayList<Snake> snakes;
    private ArrayList<Vector2> food;

    private int boardOwnerPlayerId;
    //private Snake mySnake;
    private boolean lost = false;

    private Random random;

    private ArrayList<IUpdatable> toUpdate;

    SnakesProto.GameConfig gameConfig;

    private Timer timer;
    private TimerTask timerTask;

    private int delayMillis = 500;

    private int stateOrder = 0;

    private boolean active = false;

    public int getWidth() { return gameConfig.getWidth(); }
    public int getHeight() {
        return gameConfig.getHeight();
    }

    public int getStateOrder() { return stateOrder; }

    public boolean isLost() { return lost; }
    public boolean isActive() { return active;}

    public ArrayList<Vector2> getFood() { return (ArrayList<Vector2>) food.clone(); }
    public ArrayList<Snake> getSnakes() { return (ArrayList<Snake>) snakes.clone(); }
    public int getBoardOwnerPlayerId() { return boardOwnerPlayerId; };

    public Board (SnakesProto.GameConfig gameConfig) {
        this.gameConfig = gameConfig;
        toUpdate = new ArrayList<>();
        snakes = new ArrayList<>();
        food = new ArrayList<>();
        random = new Random();
        gamePlayers = new ArrayList<SnakesProto.GamePlayer>();
    }

    public void startNewGame() {
        snakes.add(new Snake(5, 3, getHeight(), getWidth(), 0));
        boardOwnerPlayerId = 0;
        active = true;
    }

    public void start() {
        active = true;
    }

    public Snake getBoardOwnerSnake() {
        for (var snake : snakes) {
            if(snake.getPlayerId() == boardOwnerPlayerId) {
                return snake;
            }
        }
        lost = true;
        return null;
    }

    public void setOwnerSnake(int playerId) {
        boardOwnerPlayerId = playerId;
    }

    public int addSnake() {
        // TODO: if cannot create return -1

        int player_id = snakes.size() + 1;

        snakes.add(new Snake(5, 3, getHeight(), getWidth(), player_id));

        return  player_id;
    }

    public void updatePlayer(SnakesProto.GamePlayer player) {

        for (var p : gamePlayers) {
            if(p.getId() == player.getId()) {
                p = player;
                return;
            }
        }

        gamePlayers.add(player);
    }

    public void registerUpdateCallback(IUpdatable updatable) {
        toUpdate.add(updatable);
    }

    public SnakesProto.GamePlayers getGamePlayers() {

        SnakesProto.GamePlayers.Builder builder = SnakesProto.GamePlayers.newBuilder();

        for (var player : gamePlayers) {
            builder.addPlayers(player);
        }

        return builder.build();

    }

    public void update() {

        stateOrder++;

        // Move Snakes
        for (var snake : snakes) {
            snake.move();
        }

        // TODO: check snakes intersect

        // Create new food
        if(food.size() < gameConfig.getFoodStatic() + gameConfig.getFoodPerPlayer() * (snakes.size() + 1)) {
            var newFoodEntity = Vector2.clamp(Vector2.Random(), getWidth(), getHeight());
            food.add(newFoodEntity);
        }

        // Eat food
        Iterator<Vector2> foodIterator = food.iterator();

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
//        for (var snake : snakes) {
//            if(snake.getPlayerId() == boardOwnerPlayerId) {
//                continue;
//            }
//            snake.changeDirection(Vector2.RandomDirection());
//        }

        // Check self collision

        Iterator<Snake> snakeIterator = snakes.iterator();

        while (snakeIterator.hasNext()) {
            var snake = snakeIterator.next();
            if(snake.checkSelfCollision()) {
                Snake2Food(snake);
                snakeIterator.remove();
            }
        }

//        if(!lost && mySnake.checkSelfCollision()) {
//            lost = true;
//            Snake2Food(mySnake);
//        }

        // Update
        UpdateUpdatables();
    }

    private void UpdateUpdatables() {
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

    public Snake getSnake(int playerId) {
        for (var snake: snakes) {
            if(snake.getPlayerId() == playerId) {
                return snake;
            }
        }
        return null;
    }

    public void changeOwnerDirection(int x, int y) {
        if(lost) {
            return;
        }

        var mySnake = getBoardOwnerSnake();

        if(mySnake == null) {
            return;
        }

        mySnake.changeDirection(new Vector2(sgn(x), sgn(y)));
    }

    public void changeDirection(SnakesProto.Direction direction, int playerId) {

        var snake = getSnake(playerId);

        if(snake == null) {
            return;
        }

        snake.changeDirection(Vector2.direction2vector(direction));
    }

    public List<SnakesProto.GameState.Snake> getSnakesList() {

        ArrayList<SnakesProto.GameState.Snake> result = new ArrayList<>();

        for (var snake : snakes) {
            result.add(snake.convertToProtoSnake());
        }

        return result;
    }

    public List<SnakesProto.GameState.Coord> getFoodList() {

        ArrayList<SnakesProto.GameState.Coord> result = new ArrayList<>();

        for (var f : food) {
            result.add(f.convertToCoord());
        }

        return result;
    }

    public void printKeyPoints() {

        System.out.println("KEY POINTS:");

        for (var p : getBoardOwnerSnake().getKeyPoints()) {
            System.out.println("( " + p.getX() + "," + p.getY() + ")");
        }
    }

    public SnakesProto.GameState getGameState() {

        var stateBuilder = SnakesProto.GameState.newBuilder().setStateOrder(stateOrder);

        for (var snake : getSnakesList()) {
            stateBuilder.addSnakes(snake);
        }

        for (var food : getFoodList()) {
            stateBuilder.addFoods(food);
        }

        stateBuilder.setPlayers(getGamePlayers());

        stateBuilder.setConfig(gameConfig);

        return stateBuilder.build();
    }

    public void applyState(SnakesProto.GameState state) {
        stateOrder = state.getStateOrder();
        setProtoFood(state.getFoodsList());
        setProtoPlayers(state.getPlayers());
        gameConfig = state.getConfig();

        var protoSnakes = state.getSnakesList();

        snakes.clear();

        for (var protoSnake : protoSnakes) {
            snakes.add(new Snake(getHeight(), getWidth(), protoSnake));
        }

        UpdateUpdatables();
    }

    public void setProtoFood(List<SnakesProto.GameState.Coord> food) {
        this.food.clear();

        for (var f : food) {
            this.food.add(new Vector2(f.getX(), f.getY()));
        }
    }
    public void setProtoPlayers(SnakesProto.GamePlayers players) {
        System.out.println("SET PROTO PLAYERS : " + players.getPlayersList().size());
        gamePlayers.clear();

        for (var p : players.getPlayersList()) {
            System.out.println("ADD GAME PLAYER");
            gamePlayers.add(p);
        }
    }

    public static int sgn(int x) {
        if(x == 0) return 0;
        if(x > 0) return 1;
        return -1;
    }

}
