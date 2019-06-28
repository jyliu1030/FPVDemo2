package me.mizhoux.antenna.imgproc.yuv;

import org.bytedeco.javacpp.opencv_core.Size;

/**
 * YUVData
 *
 * @author Mi Zhou <mizhoux@qq.com>
 */
public class YUVData {

    public final byte[] frameData;
    public final Size imageSize;

    public YUVData(byte[] frameData, Size imageSize) {
        this.frameData = frameData;
        this.imageSize = imageSize;
    }

}
