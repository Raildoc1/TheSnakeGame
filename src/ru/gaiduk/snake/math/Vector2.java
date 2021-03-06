package ru.gaiduk.snake.math;

import me.ippolitov.fit.snakes.SnakesProto;

import java.util.Objects;
import java.util.Random;

public class Vector2 {

    private int x;
    private int y;

    public Vector2(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Vector2(SnakesProto.GameState.Coord coord) {
        this.x = coord.getX();
        this.y = coord.getY();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public static Vector2 Random() {

        Random random = new Random();

        return new Vector2(random.nextInt(), random.nextInt());
    }

    public static Vector2 RandomDirection() {

        Random random = new Random();

        int x = random.nextBoolean() ? 0 : (random.nextBoolean() ? 1 : -1);

        int y = x == 0 ? (random.nextBoolean() ? 1 : -1) : 0;

        return new Vector2(x, y);
    }

    public static Vector2 clamp(Vector2 v, int x, int y) {
        return new Vector2((v.getX() % x + x) % x, (v.getY() % y + y) % y);
    }

    public SnakesProto.GameState.Coord convertToCoord() {
        return SnakesProto.GameState.Coord.newBuilder().setX(getX()).setY(getY()).build();
    }

    public static Vector2 Sub(Vector2 a, Vector2 b) {
        return new Vector2(a.getX() - b.getX(), a.getY() - b.getY());
    }

    public static Vector2 Add(Vector2 a, Vector2 b) {
        return new Vector2(a.getX() + b.getX(), a.getY() + b.getY());
    }

    public static Vector2 Mul(Vector2 a, int c) {
        return new Vector2(a.getX() * c, a.getY() * c);
    }

    public static SnakesProto.Direction vector2direction(Vector2 vector) {
        if(vector.getX() == 1) {
            return SnakesProto.Direction.RIGHT;
        } else if (vector.getX() == -1) {
            return SnakesProto.Direction.LEFT;
        } else if(vector.getY() == 1) {
            return SnakesProto.Direction.UP;
        } else {
            return SnakesProto.Direction.DOWN;
        }
    }

    public static Vector2 direction2vector(SnakesProto.Direction direction) {
        switch (direction) {
            case UP:
                return new Vector2(0, 1);
            case DOWN:
                return new Vector2(0, -1);
            case LEFT:
                return new Vector2(-1, 0);
            default:
                return new Vector2(1, 0);
        }
    }

    public static Vector2 clampDirection(Vector2 direction, int a, int b) {
        if(direction.getX() > 1) {
            return new Vector2(direction.getX() - a, direction.getY());
        } else if(direction.getX() < -1) {
            return new Vector2(direction.getX() + a, direction.getY());
        }

        if(direction.getY() > 1) {
            return new Vector2(direction.getX(), direction.getY() - b);
        } else if(direction.getY() < -1) {
            return new Vector2(direction.getX(), direction.getY() + b);
        }

        return direction;
    }

    @Override
    public String toString() {
        return "Vector2{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vector2 vector2 = (Vector2) o;
        return x == vector2.x &&
                y == vector2.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
