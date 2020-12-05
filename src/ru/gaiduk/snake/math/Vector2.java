package ru.gaiduk.snake.math;

import java.util.Objects;
import java.util.Random;

public class Vector2 {

    private int x;
    private int y;

    public Vector2(int x, int y) {
        this.x = x;
        this.y = y;
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