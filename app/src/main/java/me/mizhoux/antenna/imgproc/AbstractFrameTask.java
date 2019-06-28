package me.mizhoux.antenna.imgproc;

import android.graphics.Rect;

import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import me.mizhoux.antenna.util.Maths;

import static org.bytedeco.javacpp.opencv_imgproc.Canny;
import static org.bytedeco.javacpp.opencv_imgproc.createLineSegmentDetector;

/**
 * AbstractFrameTask
 *
 * @param <I> Frame 任务输入的图像数据
 * @param <R> Frame 任务的角度结果
 *
 * @author Mi Zhou <mizhoux@qq.com>
 */
public abstract class AbstractFrameTask<I, R> implements Callable<R> {

    public static final int HORIZONTAL_LINE_THRESHOLD = 3;

    // 输入
    private final I input;
    private final Rect roi;

    // 输出
    private boolean successful;
    private String resultImagePath;
    private int time;  // ms

    public AbstractFrameTask(I input, Rect roi) {
        this.input = input;
        this.roi = roi;
    }


    protected abstract Mat convert(I data);

    protected abstract R handleLongestLine(Line longestLine);


    public String getResultImagePath() {
        return resultImagePath;
    }

    public boolean isSuccessful() {
        return successful;
    }

    /**
     * 返回任务运行的时间，以毫秒计
     *
     * @return 任务运行的时间
     */
    public int getTime() {
        return time;
    }

    protected I getInput() {
        return input;
    }

    protected Rect getRoi() {
        return roi;
    }


    protected void setSuccessful() {
        this.successful = true;
    }

    protected void setResultImagePath(String resultImagePath) {
        this.resultImagePath = resultImagePath;
    }

    protected void setTime(int time) {
        this.time = time;
    }


    protected Line findLongestLine(List<Line> lines) {
        //从 Line的 list 中找到最长的线
        if (lines.isEmpty()) {
            return null;
        }

        Line longestLine = null;
        int maxWeight = Integer.MIN_VALUE;

        for (Line line : lines) {
            int weight = line.getWeight();

            if (weight > maxWeight) {
                maxWeight = weight;
                longestLine = line;
            }
        }

        return longestLine;
    }

    protected List<Line> detectLines(Mat imgMat) {
        //使用 canny 检测边直线 并作为坐标放入 list 中

        Mat cannyMat = new Mat();
        Canny(imgMat, cannyMat, 50, 200, 3, false);

        Mat linesMat = new Mat();
        opencv_imgproc.LineSegmentDetector detector = createLineSegmentDetector(); //LSD直线快速检测算法
        detector.detect(cannyMat, linesMat);

        Indexer indexer = linesMat.createIndexer();//创建索引范围 mat 里的元素

        int rows = Maths.toInt(indexer.rows());
        ArrayList<Line> lines = new ArrayList<>(rows);

        for (int i = 0; i < rows; i++) {
            int x1 = Maths.toInt(indexer.getDouble(i, 0, 0));
            int y1 = Maths.toInt(indexer.getDouble(i, 0, 1));
            int x2 = Maths.toInt(indexer.getDouble(i, 0, 2));
            int y2 = Maths.toInt(indexer.getDouble(i, 0, 3));

            lines.add(new Line(x1, y1, x2, y2));
        }

        return lines;
    }

    protected double getAngleOfLine(Line line) {
        int oX, oY;  // 中间点
        int sX, sY;
        int eX, eY;

        if (line.y1 < line.y2) {
            oX = line.x1;
            oY = line.y1;

            eX = line.x2;
            eY = line.y2;
        } else {
            oX = line.x2;
            oY = line.y2;

            eX = line.x1;
            eY = line.y1;
        }

        sX = oX;
        sY = eY;

        return Maths.getAngle(oX, oY, sX, sY, eX, eY);
    }

}
