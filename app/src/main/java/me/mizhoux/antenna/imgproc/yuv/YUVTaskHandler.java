package me.mizhoux.antenna.imgproc.yuv;

import android.graphics.Rect;
import android.util.Log;

import me.mizhoux.antenna.imgproc.AbstractFrameTask;
import me.mizhoux.antenna.imgproc.AbstractTaskHandler;
import me.mizhoux.antenna.imgproc.MeasureType;


/**
 * YUVTaskHandler
 *
 * @author Mi Zhou <mizhoux@qq.com>
 */
public class YUVTaskHandler extends AbstractTaskHandler<YUVData> {

    private static final String TAG = YUVTaskHandler.class.getSimpleName();

    public YUVTaskHandler(int threadNum) {
        super(threadNum);
    }

    @Override
    protected AbstractFrameTask<YUVData, Double> createTask(YUVData input, Rect roi, double deviceAzimuth) {
        switch (getMeasureType()) {
            case MeasureType.TYPE_PITCH:
                return new PitchYUVTask(input, roi);

            case MeasureType.TYPE_AZIMUTH:
                return new AzimuthYUVTask(input, roi, deviceAzimuth);

            default:
                Log.e(TAG, "不被当前自动测量支持的类型：" + getMeasureType());
                return null;
        }
    }

}
