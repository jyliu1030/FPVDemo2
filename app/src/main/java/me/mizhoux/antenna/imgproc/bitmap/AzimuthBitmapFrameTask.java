package me.mizhoux.antenna.imgproc.bitmap;

import android.graphics.Bitmap;
import android.graphics.Rect;

import me.mizhoux.antenna.imgproc.Line;

/**
 * AzimuthBitmapFrameTask
 *
 * @author Mi Zhou <mizhoux@qq.com>
 */
public class AzimuthBitmapFrameTask extends BitmapFrameTask<Double> {

    private double uavAzimuth;  // 无人机的朝向

    AzimuthBitmapFrameTask(Bitmap input, Rect roi, double uavAzimuth) {
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
