package me.mizhoux.antenna.imgproc.bitmap;

import android.graphics.Bitmap;
import android.graphics.Rect;

import me.mizhoux.antenna.imgproc.Line;

/**
 * PitchBitmapFrameTask
 *
 * @author Mi Zhou <mizhoux@qq.com>
 */
public class PitchBitmapFrameTask extends BitmapFrameTask<Double> {

    PitchBitmapFrameTask(Bitmap input, Rect roi) {
        super(input, roi);
    }

    @Override
    protected Double handleLongestLine(Line longestLine) {
        if (longestLine.yDiff() < HORIZONTAL_LINE_THRESHOLD) {  // 水平直线
            return null;
        }

        double angle = getAngleOfLine(longestLine);

        if (angle < 1 || angle > 89) {  // 角度小于 1 度或者大于 89 度
            return null;
        }

        return angle;
    }
}
