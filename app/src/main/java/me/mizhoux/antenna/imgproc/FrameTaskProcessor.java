package me.mizhoux.antenna.imgproc;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import me.mizhoux.antenna.util.CompletedCallback;

/**
 * FrameTaskProcessor
 *
 * @author Mi Zhou <mizhoux@qq.com>
 */
public final class FrameTaskProcessor<I> {

    private ThreadPoolExecutor threadPool;

    private volatile boolean successful;

    FrameTaskProcessor(int threadNum) {
        if (threadNum < 1) {
            throw new IllegalArgumentException("线程池的线程数量必须大于 0");
        }

        threadPool = new ThreadPoolExecutor(
                threadNum, threadNum,
                0L, TimeUnit.MILLISECONDS,
                //new SynchronousQueue<Runnable>(),
                new ArrayBlockingQueue<Runnable>(1),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.DiscardPolicy());

        clear();
    }

    public void submit(final AbstractFrameTask<I, Double> task,
                       final CompletedCallback<FrameTaskResult> callback) {
        Objects.requireNonNull(callback);

        FutureTask<Double> futureTask = new FutureTask<Double>(task) {

            @Override
            protected void done() {
                if (successful) {  // 如果其他的任务已经成功，那么直接抛弃当前任务的结果
                    return;
                }

                synchronized (FrameTaskProcessor.this) {
                    if (task.isSuccessful()) {
                        successful = true;

                    } else {  // 任务不成功
                        return;
                    }
                }

                try {
                    Double resultAngle = get();
                    FrameTaskResult resultData = new FrameTaskResult(
                            task.getResultImagePath(), resultAngle, task.getTime());

                    callback.onCompleted(resultData);

                } catch (InterruptedException | ExecutionException e) {  // impossible
                    e.printStackTrace();
                }

            }
        };

        threadPool.submit(futureTask);
    }

    public void clear() {
        successful = false;
    }

    public void release() {
        threadPool.shutdownNow();
        threadPool = null;
    }

}
