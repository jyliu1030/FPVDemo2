package me.mizhoux.antenna.imgproc.yuv;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Environment;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_imgcodecs;
import org.bytedeco.javacpp.opencv_imgproc;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;

import me.mizhoux.antenna.imgproc.AbstractFrameTask;
import me.mizhoux.antenna.imgproc.Line;
import me.mizhoux.antenna.util.Maths;

import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;
import static org.bytedeco.javacpp.opencv_imgproc.line;
import static org.bytedeco.javacpp.opencv_imgproc.rectangle;

/**
 * YUVFrameTask
 *
 * @author Mi Zhou <mizhoux@qq.com>
 */
public abstract class YUVFrameTask<R> extends AbstractFrameTask<YUVData, R> {

    private static final String TAG = YUVFrameTask.class.getSimpleName();

    private static final String RESULT_IMAGE_NAME = "am-task-yuv.jpg";

    private static final int IMAGE_QUALITY = 90;
    private static final int IMAGE_FILL_SIZE = Maths.toInt(1024 * 1024 * 0.6); // 0.6M
    private static final int IMAGE_ROI_SIZE = Maths.toInt(1024 * 1024 * 0.2);  // 0.2M

    private static final int LINE_WIDTH = 8;
    private static final opencv_core.Scalar LINE_COLOR = new opencv_core.Scalar(54, 132, 244, 255);  // 橙色

    private static final int RECT_WIDTH = 8;
    private static final opencv_core.Scalar RECT_COLOR = new opencv_core.Scalar(0, 255, 0, 255);  // 绿色

    private final int frameSize;
    private final Size imageSize;

    private byte[] nv21Data;

    public YUVFrameTask(YUVData input, Rect roi) {
        super(input, roi);

        imageSize = input.imageSize;
        frameSize = input.frameData.length;
    }

    @Override
    protected Mat convert(YUVData data) {
        return jpegToMat(yuvToJpegFull());
    }

    private String saveMat(Mat mat, String imgName) {
        File sdCard = Environment.getExternalStorageDirectory();
        File destImgFile = new File(sdCard, imgName);
        String destImgPath = destImgFile.getAbsolutePath();

        imwrite(destImgPath, mat);

        return destImgPath;
    }

    @Override
    public R call() throws Exception {
        long begin = System.currentTimeMillis();

        nv21Data = getNV21Data();
        if (nv21Data == null) {
            return null;
        }

        ByteBufferOutputStream jpegStream = yuvToJpegROI();
        Mat roiMat = jpegToMat(jpegStream); // 将 jpeg 流转为 Mat

        List<Line> lines = detectLines(roiMat);
        Line ll = findLongestLine(lines);

        if (ll != null) {

            R result = handleLongestLine(ll);  // 本张图片不符合要求
            if (result == null) {
                return null;
            }

            Mat imgMat = convert(getInput());
            drawROIAndLine(imgMat, getRoi(), ll);

            setResultImagePath(saveMat(imgMat, RESULT_IMAGE_NAME));
            setTime((int) (System.currentTimeMillis() - begin));
            setSuccessful();

            return result;
        }

        return null;
    }

    private void drawROIAndLine(Mat mat, Rect roi, Line ll) {
        int left = roi.left;
        int top = roi.top;
        int right = roi.right;
        int bottom = roi.bottom;

        Maths.Function function = Maths.Function.solve(
                ll.x1 + left, ll.y1 + top, ll.x2 + left, ll.y2 + top);

        int x1 = left;
        int y1 = function.getY(x1);

        if (y1 < top || y1 > bottom) {
            y1 = y1 < top ? top : bottom;
            x1 = function.getX(y1);
        }

        int x2 = right;
        int y2 = function.getY(x2);

        if (y2 < top || y2 > bottom) {
            y2 = y2 < top ? top: bottom;
            x2 = function.getX(y2);
        }

        line(mat, new Point(x1, y1), new Point(x2, y2),
                LINE_COLOR, LINE_WIDTH, opencv_imgproc.CV_AA, 0);

        rectangle(mat,
                new opencv_core.Rect(roi.left, roi.top, roi.width(), roi.height()),
                RECT_COLOR, RECT_WIDTH, opencv_imgproc.CV_AA, 0);
    }

    private Mat jpegToMat(ByteBufferOutputStream jpegStream) {
        ByteBuffer buffer = jpegStream.getByteBuffer();

        // 将 jpeg 的图像数据转换为 Mat
        return opencv_imgcodecs.imdecode(
                new Mat(new BytePointer(buffer)), opencv_imgcodecs.CV_LOAD_IMAGE_COLOR);
    }

    private ByteBufferOutputStream yuvToJpegROI() {
        int w = imageSize.width();
        int h = imageSize.height();

        YuvImage image = new YuvImage(nv21Data, ImageFormat.NV21, w, h, null);

        ByteBufferOutputStream out = new ByteBufferOutputStream(IMAGE_ROI_SIZE);
        image.compressToJpeg(getRoi(), 100, out);

        return out;
    }

    private ByteBufferOutputStream yuvToJpegFull() {

        int w = imageSize.width();
        int h = imageSize.height();

        YuvImage image = new YuvImage(nv21Data, ImageFormat.NV21, w, h, null);
        ByteBufferOutputStream out = new ByteBufferOutputStream(IMAGE_FILL_SIZE);
        Rect t = new Rect(0, 0, w, h);

        image.compressToJpeg(t, IMAGE_QUALITY, out);

        return out;
    }

    private byte[] getNV21Data() {
        final byte[] unDecodedYUVData = getInput().frameData;

        return decodeNV21(unDecodedYUVData);
    }

    private byte[] decodeNV21(byte[] unDecodedYUVData) {
        int width = imageSize.width();
        int height = imageSize.height();

        if (unDecodedYUVData.length < width * height) {
            //DJILog.d(TAG, "yuvFrame size is too small " + yuvFrame.length);
            return null;
        }

        byte[] y = new byte[width * height];
        byte[] u = new byte[width * height / 4];
        byte[] v = new byte[width * height / 4];
        byte[] nu = new byte[width * height / 4]; //
        byte[] nv = new byte[width * height / 4];

        System.arraycopy(unDecodedYUVData, 0, y, 0, y.length);
        for (int i = 0; i < u.length; i++) {
            v[i] = unDecodedYUVData[y.length + 2 * i];
            u[i] = unDecodedYUVData[y.length + 2 * i + 1];
        }

        int uvWidth = width / 2;
        int uvHeight = height / 2;
        for (int j = 0; j < uvWidth / 2; j++) {
            for (int i = 0; i < uvHeight / 2; i++) {
                byte uSample1 = u[i * uvWidth + j];
                byte uSample2 = u[i * uvWidth + j + uvWidth / 2];
                byte vSample1 = v[(i + uvHeight / 2) * uvWidth + j];
                byte vSample2 = v[(i + uvHeight / 2) * uvWidth + j + uvWidth / 2];
                nu[2 * (i * uvWidth + j)] = uSample1;
                nu[2 * (i * uvWidth + j) + 1] = uSample1;
                nu[2 * (i * uvWidth + j) + uvWidth] = uSample2;
                nu[2 * (i * uvWidth + j) + 1 + uvWidth] = uSample2;
                nv[2 * (i * uvWidth + j)] = vSample1;
                nv[2 * (i * uvWidth + j) + 1] = vSample1;
                nv[2 * (i * uvWidth + j) + uvWidth] = vSample2;
                nv[2 * (i * uvWidth + j) + 1 + uvWidth] = vSample2;
            }
        }

        byte[] bytes = new byte[unDecodedYUVData.length];
        System.arraycopy(y, 0, bytes, 0, y.length);
        for (int i = 0; i < u.length; i++) {
            bytes[y.length + (i * 2)] = nv[i];
            bytes[y.length + (i * 2) + 1] = nu[i];
        }

        return bytes;
    }
}
