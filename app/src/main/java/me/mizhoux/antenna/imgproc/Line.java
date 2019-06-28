package me.mizhoux.antenna.imgproc;

import org.bytedeco.javacpp.opencv_core.Point;

/**
 * Line
 *
 * @author Michael Chow <mizhoux@gmail.com>
 */
public final class Line {

    public final int x1;
    public final int y1;
    public final int x2;
    public final int y2;

    public Line(int x1, int y1, int x2, int y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public int xDiff() {
        return Math.abs(x1 - x2);
    }

    public int yDiff() {
        return Math.abs(y1 - y2);
    }

    public Point getPt1() {
        return new Point(x1, y1);
    }

    public Point getPt2() {
        return new Point(x2, y2);
    }

    public Point getPt1(int xOffset, int yOffset) {
        return new Point(x1 + xOffset, y1 + yOffset);
    }

    public Point getPt2(int xOffset, int yOffset) {
        return new Point(x2 + xOffset, y2 + yOffset);
    }

    public int getWeight() {
        int xDiff = xDiff();
        int yDiff = yDiff();

        return xDiff * xDiff + yDiff * yDiff;
    }

}
