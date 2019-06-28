package me.mizhoux.antenna.imgproc.yuv;

import android.graphics.Rect;

import me.mizhoux.antenna.imgproc.Line;


/**
 * PitchYUVTask
 *
 * @author Mi Zhou <mizhoux@qq.com>
 */
public class PitchYUVTask extends YUVFrameTask<Double> {

    PitchYUVTask(YUVData input, Rect roi) {
        super(input, roi);
    }

    @Override
    protected Double handleLongestLine(Line longestLine) {
        if (longestLine.yDiff() < HORIZONTAL_LINE_THRESHOLD) {  // 水平直线
            return null;
        }

        double angle = getAngleOfLine(longestLine);

        if (angle < 5 || angle > 80) {  // 角度小于 5 度或者大于 80 度
            return null;
        }

        return angle;
    }
}
