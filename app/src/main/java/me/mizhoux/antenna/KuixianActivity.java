package me.mizhoux.antenna;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.CvType.CV_8UC1;

public class KuixianActivity extends AppCompatActivity {
    Bitmap srcBitmap;
    Bitmap grayBitmap;
    Bitmap toHsvBitmap;
    Bitmap hsvBitmap;

    ImageView imgHuaishi;
    TextView touchedXY, invertedXY, imgSize, colorRGB, colorHSVh, colorHSVs, colorHSVv,kuixian;
    ImageView imgSource1, imgSource2;
    double hsvH ;
    double hsvS ;
    double hsvV ;
    double[] HSVdouble;

    Mat result;

    Button btnzoomProcess;
    Button btnTohsvImage;
    Button btnSelect;

    ProgressBar barKuixian;


    private static boolean flag = true;
    //private static boolean isFirst = true;
    private static final String TAG = "MainActivity";

    int hsvX, hsvY;
    float proX, proY;
    double[] touchHSV;
    boolean hsvProcessing;

    //OpenCV库加载并初始化成功后的回调函数
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            // TODO Auto-generated method stub
            switch (status){
                case BaseLoaderCallback.SUCCESS:
                    Log.i(TAG, "成功加载");
                    break;
                default:
                    super.onManagerConnected(status);
                    Log.i(TAG, "加载失败");
                    break;
            }

        }

    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kuixian);

        initUI();

        btnTohsvImage.setOnClickListener(new ProcessClickListener());
        btnzoomProcess.setOnClickListener(new ProcessClickListener());
        btnSelect.setOnClickListener(new ProcessClickListener());
        barKuixian.setVisibility(View.GONE);

        //监听图片操作
        imgHuaishi.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("SetTextI18n")

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                float eventX = event.getX();
                float eventY = event.getY();
                float[] eventXY = new float[]{eventX, eventY};

                Matrix invertMatrix = new Matrix();
                ((ImageView) v).getImageMatrix().invert(invertMatrix);

                invertMatrix.mapPoints(eventXY);
                int x = (int) eventXY[0];
                int y = (int) eventXY[1];

                //Limit x, y range within bitmap
                if (x < 0) {
                    x = 0;
                } else if (x > srcBitmap.getWidth() - 1) {
                    x = srcBitmap.getWidth() - 1;
                }

                if (y < 0) {
                    y = 0;
                } else if (y > srcBitmap.getHeight() - 1) {
                    y = srcBitmap.getHeight() - 1;
                }

                proX = x / srcBitmap.getWidth();
                proY = y / srcBitmap.getHeight();

                hsvX = x;
                hsvY = y;

                int touchedRGB = srcBitmap.getPixel(hsvX, hsvY);
                Log.i("test",Integer.toHexString(touchedRGB));


                String oxr = Integer.toHexString(Color.red(touchedRGB));
                String oxg = Integer.toHexString(Color.green(touchedRGB));
                String oxb = Integer.toHexString(Color.blue(touchedRGB));
                int intR = Integer.parseInt(oxr,16);
                int intG = Integer.parseInt(oxg,16);
                int intB = Integer.parseInt(oxb,16);
                Log.i("test",Integer.toString(intR));
                Log.i("test",Integer.toString(intG));
                Log.i("test",Integer.toString(intB));


                colorRGB.setText("touched color: " + "#" + Integer.toString(intR) + Integer.toString(intG) + Integer.toString(intB));
                colorRGB.setTextColor(touchedRGB);

                int[] RGBint = {intR,intG,intB};
                HSVdouble = BGR2HSV(RGBint);
                hsvH = HSVdouble[0];
                hsvS = HSVdouble[1];
                hsvV = HSVdouble[2];
                if(hsvH == 0){
                    hsvH += 180.0;
                }
                colorHSVh.setText("colorhsvH: " + hsvH);
                colorHSVs.setText("colorhsvS: " + hsvS);
                colorHSVv.setText("colorhsvV: " + hsvV);


                return true;
            }
        });

        try{
            Intent intent = getIntent();
            Uri uri = intent.getData();
            srcBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            imgHuaishi.setImageBitmap(srcBitmap);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    //opencv 加载相关
    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void initUI(){
//        touchedXY = (TextView)findViewById(R.id.xy);
//        invertedXY = (TextView)findViewById(R.id.invertedxy);
//        imgSize = (TextView)findViewById(R.id.size);
        colorRGB = (TextView)findViewById(R.id.colorrgb);
        colorHSVh = (TextView)findViewById(R.id.colorhsvH);
        colorHSVs = (TextView)findViewById(R.id.colorhsvS);
        colorHSVv = (TextView)findViewById(R.id.colorhsvV);
        kuixian = (TextView)findViewById(R.id.kuixian);
        btnTohsvImage = (Button) findViewById(R.id.btn_toHSV_img);

        barKuixian =(ProgressBar)findViewById(R.id.kuixianbar);

        imgHuaishi = (ImageView) findViewById(R.id.img_huaishi);

        btnzoomProcess = (Button) findViewById(R.id.btn_zoom_process);
        btnSelect =(Button)findViewById(R.id.btn_select);


        Log.i(TAG, "initUI sucess...");

    }

    private class ProcessClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {

            switch (v.getId()) {
                case R.id.btn_toHSV_img:
                    barKuixian.setVisibility(View.VISIBLE);
                    processHSV();

                    break;

                case R.id.btn_zoom_process:
                    ZoomImage();
                    break;
                case R.id.btn_select:
                    selectImage();
                    //dilate();
                    break;
            }
        }
    }

    private void ZoomImage(){
        //传入图片到缩放 activity 中

        Uri uri = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), srcBitmap, null,null));
        //将 Bitmap 转成 uri
        Intent intent=new Intent(getApplicationContext(),ZoomActivity.class);
        intent.setAction(Intent.ACTION_SEND);//设置分享行为
        intent.setType("image/*");//设置分享内容的类型
        intent.setData(uri);

        startActivityForResult(intent,1);// 请求码为1  缩放

        System.out.print("ok");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==1 &&resultCode==3){
            //返回缩放图片
            Uri uri =data.getData();
            try {
                srcBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                imgHuaishi.setImageBitmap(srcBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(requestCode==2){
            //返回选择图片
            Uri uri = data.getData();
            try {
                srcBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                imgHuaishi.setImageBitmap(srcBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static double[] BGR2HSV(int[] rgb){
        double[] hsv=new double[3];
        double r=(double)rgb[0]/255;
        double g=(double)rgb[1]/255;
        double b=(double)rgb[2]/255;
        double max = Math.max(b,Math.max(r,g));
        double min = Math.min(b,Math.min(r,g));
        double h = 0;
        if(r==max)
            h = (g-b)/(max-min);
        if(g==max)
            h = 2+(b-r)/(max-min);
        if(b==max)
            h= 4+(r-g)/(max-min);
        h *=60;
        if(h<0) h +=360;
        hsv[0] = h/2;
        hsv[1] = 255*(max-min)/max;
        hsv[2] = 255*max;
        return hsv;
    }


    int numB = 0;
    int numS = 0;
    ArrayList<MatOfPoint> contours = new ArrayList<>();
    //馈线检测的方法

    private void dilate(){
        if(srcBitmap==null){
            selectImage();
        }else {
            Mat testMat =new Mat();
            Bitmap saveSrc =srcBitmap;
            Utils.bitmapToMat(srcBitmap,testMat);
            Mat toMat=new Mat();
            Imgproc.cvtColor(testMat,toMat,Imgproc.COLOR_RGB2GRAY);
            Mat dilateX_image = new Mat();
            Mat dilateY_image = new Mat();
            Mat erodeX_image = new Mat();

            Mat elementX = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,new Size(5,1));
            Mat elementY = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,new Size(1,2));
            Point point = new Point(-1,-1);
            //我们需要来中值滤波去除单点噪音
            //Mat new_image=new Mat();
            //Imgproc.threshold(candy_image,new_image,0,255,Imgproc.THRESH_BINARY);
            //Imgproc.medianBlur(candy_image,blurr_image,1);
            Imgproc.dilate(toMat,dilateY_image,elementY,point,1);
            Imgproc.dilate(dilateY_image,dilateY_image,elementY,point,1);
            toast("腐蚀成功1");

        }



    }

    private void processHSV() {
        kuixian.setText("馈线安装是否正确: ");
        numB = 0;
        numS = 0;
        contours.clear();

        if (srcBitmap == null) {
            toast("请先选择图片");
            return;
        }

        if (hsvProcessing) {
            toast("已经在处理 HSV 中");
            return;
        }

        hsvProcessing = true;
        barKuixian.setVisibility(View.VISIBLE);

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                toast("开始进行馈线检测请稍候...");
                Mat rgbMat = new Mat();
                Mat hsvMat = new Mat();
                Utils.bitmapToMat(srcBitmap,rgbMat);
                Imgproc.cvtColor(rgbMat,hsvMat,Imgproc.COLOR_RGB2HSV);
                result = new Mat(hsvMat.rows(),hsvMat.cols(),CV_8UC1);
                Double H , S  , V ;
                for(int i = 0 ; i < hsvMat.rows() ; i ++){
                    for(int j = 0 ; j < hsvMat.cols() ; j ++){
                        double[] clone = hsvMat.get(i,j).clone();
                        H = clone[0];
                        S = clone[1];
                        V = clone[2];
                        result.put(i,j,255);
                        if((H >= hsvH - 10) && (H <= hsvH + 10)){
                            if((S >= hsvS - 60) && (S <= hsvS + 60)){
                                if((V >= hsvV - 5) && (V <= hsvV + 5)){
                                    result.put(i,j,0);
                                }
                            }
                        }

                    }
                }
                Bitmap savedBitmap = Bitmap.createBitmap(srcBitmap.getWidth(),srcBitmap.getHeight(),Bitmap.Config.RGB_565);
                //原图是hsvMat
                Mat candy_image = new Mat();
                Mat dilateX_image = new Mat();
                Mat dilateY_image = new Mat();
                Mat erodeX_image = new Mat();
                //Imgproc.cvtColor(result,gray_image,Imgproc.COLOR_BGR5652GRAY);
                Mat blurr_image = new Mat();
                Imgproc.medianBlur(result,blurr_image,5);
                Imgproc.medianBlur(blurr_image,blurr_image,5);
                //Imgproc.Canny(blurr_image,candy_image,100,200,3,true);
                //Imgproc.Canny(blurr_image,candy_image,100,200);
                Imgproc.Canny(blurr_image,candy_image,100,200,3,false);

                Utils.matToBitmap(candy_image,savedBitmap);
                saveBitmap(savedBitmap,"test.jpg");
                Mat elementX = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,new Size(5,1));
                Mat elementY = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,new Size(1,20));
                Point point = new Point(-1,-1);
                //我们需要来中值滤波去除单点噪音
                //Mat new_image=new Mat();
                //Imgproc.threshold(candy_image,new_image,0,255,Imgproc.THRESH_BINARY);
                //Imgproc.medianBlur(candy_image,blurr_image,1);
                Imgproc.dilate(candy_image,dilateY_image,elementY,point,10);
                Imgproc.dilate(dilateY_image,dilateX_image,elementX,point,6);
                Imgproc.erode(dilateX_image,erodeX_image,elementX,point,4);

                //经过膨胀腐蚀，我们得到数根条状线，接下来进行中值滤波
                //我们需要来中值滤波去除单点噪音

                Imgproc.medianBlur(erodeX_image,blurr_image,5);
                Imgproc.medianBlur(blurr_image,blurr_image,5);


                //接下来我们进行轮廓提取
                //取得轮廓后，可以将Y轴方向最顶端和最低端的点连接起来
                //这样判断有多少根线来确定馈线数量，再通过确定线段长度来确定具体馈线
                //轮廓提取
                Mat contour_image = new Mat();
                //深拷贝，查找轮廓会改变原图像信息，因此需要重新拷贝图像
                contour_image = blurr_image.clone();
                List<Point> contour;
                Mat hierarchy = new Mat();
                Imgproc.findContours(contour_image,contours,hierarchy, Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_SIMPLE,new Point(0,0));

                if(contours.size() > 8){
                    toast("请确认是否选中馈线标志点");
                    hsvProcessing = false;
                    return;
                }
                int[] x = new int[10];
                int[] z = new int[10];
                System.out.println(contours.size());
                for(int j = 0 ; j < contours.size() ; j++){
                    int var_top = 0, var_base = 100000;
                    int var_i = 100000;
                    System.out.println(contours.get(j).toList());
                    contour = contours.get(j).toList();
                    for(int i = 0; i < contour.size(); i++){
                        if(var_i >= contour.get(i).x){
                            var_i = (int)contour.get(i).x;
                        }
                        if(var_top <= contour.get(i).y){
                            var_top = (int)contour.get(i).y;
                        }
                        if(var_base >= contour.get(i).y){
                            var_base = (int)contour.get(i).y;
                        }
                    }
                    x[j] = var_i;
                    z[j] = var_top - var_base;
                }

                //排序使x z 的顺序从左到右排序
                for(int i = 0 ; i < contours.size() - 1 ; i++){
                    for(int j = 0 ; j < contours.size() - 1 - i ; j++){
                        if(x[j] > x[j+1]) {
                            int temp_x = x[j];
                            x[j] = x[j + 1];
                            x[j + 1] = temp_x;
                            int temp_z = z[j];
                            z[j] = z[j+1];
                            z[j+1] = temp_z;
                        }
                    }
                }


                //判断z[]是否为递减排序


                for(int i = 0 ; i < contours.size() - 1; i++){
                    if(z[i] > z[i+1]) {
                        numB++;
                    }
                }
                for(int i = 0 ; i < contours.size() - 1; i++){
                    if(z[i] < z[i+1]) {
                        numS++;
                    }
                }

                toHsvBitmap = Bitmap.createBitmap(srcBitmap.getWidth(),srcBitmap.getHeight(),Bitmap.Config.RGB_565);
                Utils.matToBitmap(blurr_image,toHsvBitmap);



                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(numB == contours.size() - 1 || numS == contours.size() - 1){
                            kuixian.setText("馈线安装是否正确: " + "馈线安装正确");
                        }
                        else{
                            kuixian.setText("馈线安装是否正确: " + "馈线安装不正确");
                            toast("请确认是否选中馈线标志点");
                        }
                        imgHuaishi.setImageBitmap(toHsvBitmap);
                    }
                });
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        barKuixian.setVisibility(View.GONE);
                    }
                });
                hsvProcessing = false;
            }
        });

        thread.start();
    }

    private void toast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }



    private String selectedImagePath;

    private void selectImage() {
        Intent intentToPickPic = new Intent(Intent.ACTION_PICK);
        intentToPickPic.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intentToPickPic, 2); //requestCode =2
    }


    private String saveBitmap(Bitmap bmp, String imgName) {
        File sdCard = Environment.getExternalStorageDirectory();
        File destImgFile = new File(sdCard, imgName);
        String destImgPath = destImgFile.getAbsolutePath();

        try (FileOutputStream fos = new FileOutputStream(destImgFile)) {
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return destImgPath;
    }
}
