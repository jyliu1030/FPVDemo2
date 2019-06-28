package me.mizhoux.antenna.imgproc.yuv;

import android.graphics.Rect;

import me.mizhoux.antenna.imgproc.Line;


/**
 * AzimuthYUVTask
 *
 * @author Mi Zhou <mizhoux@qq.com>
 */
public class AzimuthYUVTask extends YUVFrameTask<Double> {

    private double uavAzimuth;  // 无人机的朝向

    AzimuthYUVTask(YUVData input, Rect roi, double uavAzimuth) {
        super(input, roi);
        this.uavAzimuth = uavAzimuth;
    }

    @Override
    protected Double handleLongestLine(Line longestLine) {
        if (longestLine.yDiff() < HORIZONTAL_LINE_THRESHOLD) {
            return  (uavAzimuth + 180) % 360;  // uavAzimuth 的反方向
        }

        return null;
    }

}
