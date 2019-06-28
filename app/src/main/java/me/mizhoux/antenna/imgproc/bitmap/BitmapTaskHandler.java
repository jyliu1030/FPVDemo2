package me.mizhoux.antenna.imgproc.bitmap;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import me.mizhoux.antenna.imgproc.AbstractFrameTask;
import me.mizhoux.antenna.imgproc.AbstractTaskHandler;
import me.mizhoux.antenna.imgproc.MeasureType;

/**
 * BitmapTaskHandler
 *
 * @author Mi Zhou <mizhoux@qq.com>
 */
public class BitmapTaskHandler extends AbstractTaskHandler<Bitmap>{

    private static final String TAG = BitmapTaskHandler.class.getSimpleName();

    public BitmapTaskHandler(int threadNum) {
        super(threadNum);
    }

    @Override
    protected AbstractFrameTask<Bitmap, Double> createTask(Bitmap input, Rect roi, double deviceAzimuth) {
        switch (getMeasureType()) {
            case MeasureType.TYPE_PITCH:
                return new PitchBitmapFrameTask(input, roi);
                //测俯仰角

            case MeasureType.TYPE_AZIMUTH:
                return new AzimuthBitmapFrameTask(input, roi, deviceAzimuth);
                //测方位角

            default:
                Log.e(TAG, "不被当前自动测量支持的类型：" + getMeasureType());
                return null;
        }
    }

}
