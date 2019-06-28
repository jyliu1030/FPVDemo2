package me.mizhoux.antenna.imgproc.bitmap;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Environment;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import me.mizhoux.antenna.imgproc.AbstractFrameTask;
import me.mizhoux.antenna.imgproc.Line;
import me.mizhoux.antenna.util.Maths;

/**
 * BitmapFrameTask
 *
 * @author Mi Zhou <mizhoux@qq.com>
 */
public abstract class BitmapFrameTask<R> extends AbstractFrameTask<Bitmap, R> {

    private static final String RESULT_IMAGE_NAME = "am-task-bmp.jpg";

    private static final int IMAGE_QUALITY = 100;

    private static final int DRAWING_LINE_WIDTH = 8;
    private static final int DRAWING_LINE_COLOR = Color.GREEN;

    BitmapFrameTask(Bitmap input, Rect roi) {
        super(input, roi);
    }

    @Override
    public R call() {
        long begin = System.currentTimeMillis();

        Bitmap roiBmp = crop(getInput(), getRoi());

        saveBitmap(roiBmp, "am-roi-bitmap.jpg");

        Mat roiMat = convert(roiBmp);
        //转换过程中是否存在问题

        List<Line> lines = detectLines(roiMat); //仅仅在矩形区域内检测直线
        Line ll = findLongestLine(lines); //这些方法都在之前定义好

        if (ll != null) {
            R result = handleLongestLine(ll);
            if (result == null) {  // 本张图片不符合要求
                return null;
            }

            drawROIAndLine(ll);
            //drawROIAndLines(lines);

            setResultImagePath(saveBitmap(getInput(), RESULT_IMAGE_NAME));
            setTime((int) (System.currentTimeMillis() - begin));
            setSuccessful();

            return result;
        }

        return null;
    }

    private String saveBitmap(Bitmap bmp, String imgName) {
        File sdCard = Environment.getExternalStorageDirectory();
        File destImgFile = new File(sdCard, imgName);
        String destImgPath = destImgFile.getAbsolutePath();

        try (FileOutputStream fos = new FileOutputStream(destImgFile)) {
            bmp.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, fos);

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return destImgPath;
    }

    private void drawROIAndLine(Line ll) {
        //画矩形
        Canvas canvas = new Canvas(getInput());

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(DRAWING_LINE_COLOR);
        paint.setStrokeWidth(DRAWING_LINE_WIDTH);

        Rect roi = getRoi();

        int left = roi.left;
        int top = roi.top;
        int right = roi.right;
        int bottom = roi.bottom;

        canvas.drawRect(left, top, right, bottom, paint);

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
            y2 = y2 < top ? top : bottom;
            x2 = function.getX(y2);
        }

        canvas.drawLine(x1, y1, x2, y2, paint);
    }

    private void drawROIAndLines(List<Line> lines) {
        //画矩形
        Canvas canvas = new Canvas(getInput());

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(DRAWING_LINE_COLOR);
        paint.setStrokeWidth(DRAWING_LINE_WIDTH);

        Rect roi = getRoi();

        int left = roi.left;
        int top = roi.top;
        int right = roi.right;
        int bottom = roi.bottom;

        canvas.drawRect(left, top, right, bottom, paint);

        int[] colors = {
                Color.BLACK,
                Color.BLUE,
                Color.CYAN,
                Color.GREEN,
                Color.YELLOW,
                Color.DKGRAY
        };

        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            paint.setColor(colors[i % colors.length]);
            canvas.drawLine(line.x1 + left, line.y1 + top, line.x2 + left, line.y2 + top, paint);
        }

        Line line = findLongestLine(lines);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(16);
        canvas.drawLine(line.x1 + left, line.y1 + top, line.x2 + left, line.y2 + top, paint);
    }

    private Bitmap crop(Bitmap src, Rect region) {
        return Bitmap.createBitmap(src, region.left, region.top, region.width(), region.height());
    }

    @Override
    protected Mat convert(Bitmap bmp) {
        AndroidFrameConverter converter = new AndroidFrameConverter();
        OpenCVFrameConverter.ToMat matConvert = new OpenCVFrameConverter.ToMat();

        Frame frame = converter.convert(bmp);

        return matConvert.convert(frame);
    }

}
