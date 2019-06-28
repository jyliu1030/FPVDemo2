package me.mizhoux.antenna.util;

/**
 * Maths
 *
 * @author Mi Zhou <mizhoux@qq.com>
 */
public final class Maths {

    public static int toInt(double v) {
        return (int) v;
    }

    public static int toInt(float v) {
        return (int) v;
    }

    public static int getDistance(int x1, int y1, int x2, int y2) {
        return toInt(Math.sqrt( (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2) ));
    }

    public static double getAngle(int oX, int oY, int sX, int sY, int eX, int eY) {

        double dsx = sX - oX;
        double dsy = sY - oY;
        double dex = eX - oX;
        double dey = eY - oY;

        double cosfi = dsx * dex + dsy * dey;
        double norm = (dsx * dsx + dsy * dsy) * (dex * dex + dey * dey);
        cosfi /= Math.sqrt(norm);

        if (cosfi >= 1.0) return 0;

        if (cosfi <= -1.0) return Math.PI;

        double fi = Math.acos(cosfi);

        if (180 * fi / Math.PI < 180) {
            return 180 * fi / Math.PI;
        } else {
            return 360 - 180 * fi / Math.PI;
        }
    }

    /**
     * 表示 y = a*x + b 的函数
     */
    public static class Function {

        private final double a;  // 斜率
        private final double b;  // 常数

        public static Function solve(int x1, int y1, int x2, int y2) {
            if (x1 - x2 == 0) {
                throw new IllegalArgumentException("竖直线无法表示（斜率不存在）");
            }

            if (y1 - y2 == 0) {
                return new Function(0, y1);
            }

            double a = 1.0 * (y1 - y2) / (x1 - x2);
            double b = y1 - a * x1;

            return new Function(a, b);
        }

        public Function(double a, double b) {
            this.a = a;
            this.b = b;
        }

        public int getY(int x) {
            return Maths.toInt(a * x + b);
        }

        public int getX(int y) {
            return Maths.toInt((y - b) / a);
        }

    }

    private Maths() {}

}
