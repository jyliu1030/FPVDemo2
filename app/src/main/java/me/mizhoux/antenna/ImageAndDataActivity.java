package me.mizhoux.antenna;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.mizhoux.antenna.constant.ExtraKey;
import me.mizhoux.antenna.imgproc.FrameTaskResult;
import me.mizhoux.antenna.imgproc.MeasureType;
import me.mizhoux.antenna.util.Images;
import me.mizhoux.antenna.util.Maths;
import me.mizhoux.antenna.util.Util;

/**
 * ImageAndDataActivity
 *
 * @author Mi Zhou <mizhoux@qq.com>
 */
public class ImageAndDataActivity extends Activity {

    private static final String TAG = ImageAndDataActivity.class.getSimpleName();

    @BindView(R.id.iv_img_data) ImageView ivImg;
    @BindView(R.id.tv_img_data) TextView tvData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_img_data);

        ButterKnife.bind(this);

        Intent intent = getIntent();

        FrameTaskResult result = (FrameTaskResult) intent.getSerializableExtra(ExtraKey.KEY_TASK_RESULT);
        String imgPath = result.resultImagePath;
        double angleData = result.resultAngle;
        int measureType = intent.getIntExtra(ExtraKey.KEY_MEASURE_TYPE, 0);

        Pair<Integer, Integer> imgSize = Images.getImageSize(imgPath);
        int imgWidth = imgSize.first;
        int imgHeight = imgSize.second;

//        Window dialogWindow = getWindow();
//        WindowManager.LayoutParams lp = dialogWindow.getAttributes();

        DisplayMetrics d = getResources().getDisplayMetrics();

        int IW = d.widthPixels;  // 缩放后图片的宽度
        int IH = Maths.toInt(1.0 * imgHeight * IW / imgWidth);  // 缩放后图片的高度

        int _sh = Maths.toInt(d.heightPixels * 0.75); // 图片高度大于当前屏幕可用高度 * 0.75
        if (IH > _sh) {
            IH = Maths.toInt(_sh);
            IW = Maths.toInt(1.0 * imgWidth * IH / imgHeight);
        }

        ViewGroup.LayoutParams params = ivImg.getLayoutParams();
        params.width = IW;
        params.height = IH;
        ivImg.setLayoutParams(params);

        ImageLoader.getInstance().displayImage(Util.FILE_PROTOCOL + imgPath, ivImg);

        String angleType = measureType == MeasureType.TYPE_PITCH ? "俯仰角：" : "方位角：";
        String text = angleType + String.format(Locale.getDefault(), "%.2f°", angleData);

        if(angleData<46&&angleData>40){
            tvData.setText("截取的图片存在问题，请重新截取");
        } else {
            tvData.setText(text);
        }

    }

}
