package me.mizhoux.antenna.imgproc;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.Log;

import me.mizhoux.antenna.util.CompletedCallback;

/**
 * AbstractTaskHandler
 *
 * @author Mi Zhou <mizhoux@qq.com>
 */
public abstract class AbstractTaskHandler<I> {

    private int measureType;
    private CompletedCallback<FrameTaskResult> callback;
    private volatile boolean mDetecting;
    private FrameTaskProcessor<I> mFrameProcessor;

    public AbstractTaskHandler(int threadNum) {
        mFrameProcessor = new FrameTaskProcessor<>(threadNum);
    }

    /**
     * 判断当前是否处于检测中
     *
     * @return 如果处于检测中，返回 true；否则返回 false
     */
    public boolean isDetecting() {
        return mDetecting;
    }
    public boolean cancalDetecting(){
        return !mDetecting;
    }

    public int getMeasureType() {
        return measureType;
    }

    public void startDetecting(int measureType, @NonNull CompletedCallback<FrameTaskResult> callback) {
        if (mDetecting) {
            return;
        }

        this.measureType = measureType;
        this.callback = callback;

        mFrameProcessor.clear();

        // 必须放到最后设置，在此之前需要设置完毕上面的参数
        this.mDetecting = true;  // 开始检测的标记
    }

    protected abstract AbstractFrameTask<I, Double> createTask(I input,  Rect roi, double deviceAzimuth);

    public void handleFrame(I input, Rect roi, double deviceAzimuth) {
        //处理每一帧的数据
        if (!mDetecting) {
            return;
        }
        //处理帧任务
        AbstractFrameTask<I, Double> task = createTask(input, roi, deviceAzimuth);
        if (task == null) {
            Log.i("ljytest","zhoumi null");
            return;
        }

        mFrameProcessor.submit(task, new CompletedCallback<FrameTaskResult>() {

            @Override
            public void onCompleted(FrameTaskResult result) {



                callback.onCompleted(result);

                mDetecting = false;


            }

        });
    }

    public void release() {
        mFrameProcessor.release();
        mFrameProcessor = null;
    }
}
