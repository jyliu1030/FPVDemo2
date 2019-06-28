package me.mizhoux.antenna.imgproc;

import java.io.Serializable;

/**
 * FrameTaskResult
 *
 * @author Mi Zhou <mizhoux@qq.com>
 */
public final class FrameTaskResult implements Serializable {

    public final String resultImagePath;
    public final double resultAngle;

    public final int time;

    public FrameTaskResult(String resultImagePath, double resultAngle, int time) {
        this.resultImagePath = resultImagePath;
        this.resultAngle = resultAngle;
        this.time = time;
    }

}
