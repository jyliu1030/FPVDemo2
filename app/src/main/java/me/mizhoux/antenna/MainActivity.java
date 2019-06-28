package me.mizhoux.antenna;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import org.bytedeco.javacpp.opencv_core;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dji.common.battery.BatteryState;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.gimbal.GimbalState;
import dji.common.product.Model;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.battery.Battery;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.useraccount.UserAccountManager;
import me.mizhoux.antenna.constant.ExtraKey;
import me.mizhoux.antenna.imgproc.FrameTaskResult;
import me.mizhoux.antenna.imgproc.MeasureType;
import me.mizhoux.antenna.imgproc.bitmap.BitmapTaskHandler;
import me.mizhoux.antenna.imgproc.yuv.YUVData;
import me.mizhoux.antenna.util.CompletedCallback;
import me.mizhoux.antenna.util.Maths;
import me.mizhoux.antenna.util.Util;
import me.mizhoux.antenna.view.RegionSelectorView;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class MainActivity
        extends Activity
        implements SurfaceTextureListener, DJICodecManager.YuvDataCallback {

    private static final String TAG = MainActivity.class.getName();


    //视频编码   VideoDataCallback 处理收到回收的视频
    protected VideoFeeder.VideoDataCallback mReceivedVideoDataCallBack = null;
    // codec for video live view
    protected DJICodecManager mCodecManager = null;

    protected TextureView mVideoSurface = null;

    @BindView(R.id.btn_capture) Button btnCapture;
    @BindView(R.id.btn_detect)  Button btnDetect;
    @BindView(R.id.btn_exit)    Button btnExit;
    @BindView(R.id.btn_send)    Button btnSend;
    @BindView(R.id.btn_pick) Button btnPick;

    @BindView(R.id.tv_battery)      TextView tvBattery;
    @BindView(R.id.tv_camera_pitch) TextView tvCameraPitch;

    @BindView(R.id.region_selector) RegionSelectorView regionSelector;

    @BindView(R.id.rb_measure_pitch) RadioButton rbMeasurePitch;

    private Handler mUIHandler;

    private Rect toDrawRect =null;
    private ScheduledExecutorService mTimer;
    private BitmapTaskHandler mTaskHandler = new BitmapTaskHandler(2);
    //开启 Bitmaptask

    //private YUVTaskHandler mTaskHandler = new YUVTaskHandler(2);

    private float gimbalYaw, gimbalPitch, gimbalRoll;


    private static final int POST=1030;
    private static final String HOST="192.168.1.105";
    private Bitmap result;
    private String zuobiao=null;
    private Bitmap sendBitmap=null;

    private static boolean CONNECTED=false;
    private static boolean textOK=false;//标识符 图片接收成功，开始接收文字

    private static boolean autoFlag=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);  // 保持屏幕常亮

        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);


        mUIHandler = new Handler();

        initUI();

        //回调之后完成的内容
        // the callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }

        };

        //获取角度相关属性 通过传感器获得飞行器的姿态角
        final Gimbal gimbal = FPVApplication.getGimbalInstance();
        if (gimbal != null) {
            gimbal.setStateCallback(new GimbalState.Callback() {

                @Override
                public void onUpdate(@NonNull GimbalState gimbalState) {
                    dji.common.gimbal.Attitude attitude = gimbalState.getAttitudeInDegrees();

                    gimbalYaw = attitude.getYaw();
                    gimbalPitch = attitude.getPitch();
                    gimbalRoll = attitude.getRoll();

                    mUIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            tvCameraPitch.setText(String.format(Locale.getDefault(),
                                    "相机的俯仰角：%.2f", gimbalPitch));
                        }
                    });
                }
            });
        }

        //获取电池相关属性
        final Battery battery = FPVApplication.getBatteryInstance();
        if (battery != null) {
            battery.setStateCallback(new BatteryState.Callback() {
                @Override
                public void onUpdate(final BatteryState state) {
                    mUIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            int remainingInPercent = state.getChargeRemainingInPercent();

                            if (remainingInPercent > 30) {
                                tvBattery.setTextColor(Color.WHITE);

                            } else {
                                tvBattery.setTextColor(Color.RED);
                            }

                            tvBattery.setText("电池电量：" + state.getChargeRemainingInPercent() + "%");
                        }
                    });

                }
            });
        }

    }

    protected void onProductChange() {
        //初始化大疆相关
        loadPreviewer();
        loginAccount();
    }

    private void loginAccount(){
        UserAccountManager.getInstance().logIntoDJIUserAccount(this,

                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {

                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Log.e(TAG, "登录成功");
                    }

                    @Override
                    public void onFailure(DJIError error) {
                        showToast("登录失败:"
                                + error.getDescription());
                    }
                });
    }

    public void test()
    {   Log.i("ljytest","clickagian");
        if(sendBitmap==null||toDrawRect==null){
            Toast.makeText(getApplicationContext(),"未截图图像",Toast.LENGTH_SHORT).show();
        }else{
            final int measureType = rbMeasurePitch.isChecked() ? MeasureType.TYPE_PITCH : MeasureType.TYPE_AZIMUTH;

            //通过选择类型进行线程
            mTaskHandler.startDetecting(measureType, new CompletedCallback<FrameTaskResult>() {

                @Override
                public void onCompleted(final FrameTaskResult result) {
                    //处理帧的结果
                    Log.i("ljytest","这是个测试");

                    if(result==null){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(),"无法显示",Toast.LENGTH_LONG).show();
                            }
                        });
                        return;
                    }
                    Log.i("ljytest","开始 Intent");
                    Intent intent = new Intent(MainActivity.this, ImageAndDataActivity.class);
                    intent.putExtra(ExtraKey.KEY_MEASURE_TYPE, measureType);
                    intent.putExtra(ExtraKey.KEY_TASK_RESULT, result);
                    //result 是包含着相关数据的
                    startActivity(intent);
                    Log.i("ljy","againFinish");


                }

            });

            Log.i("ljytest","start 已经执行");
            mTaskHandler.handleFrame(sendBitmap,toDrawRect,gimbalYaw);
        }

    }

    @OnClick(R.id.btn_pick)

    public void pick(){
        //进行馈线检测
        regionSelector.setVisibility(View.GONE);
        sendBitmap=mVideoSurface.getBitmap();
        regionSelector.setVisibility(View.VISIBLE);
        //创建窗口
        android.app.AlertDialog.Builder builder =new android.app.AlertDialog.Builder(this,R.style.AlertDialog);
        View view =View.inflate(getApplicationContext(),R.layout.activity_dialog,null);
        builder.setView(view);
        builder.setCancelable(false);
        final android.app.AlertDialog dialog=builder.create();
        ImageView toshow = (ImageView) view.findViewById(R.id.sendimg);
        toshow.setImageBitmap(sendBitmap);
        Button cancel =(Button) view.findViewById(R.id.cancelDialog);
        Button ok= (Button)view.findViewById(R.id.okDialog);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //通过 Intent 进入馈线 Activity
                Uri uri = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), sendBitmap, null,null));
                //将 Bitmap 转成 uri
                Intent intent=new Intent(getApplicationContext(),KuixianActivity.class);
                intent.setAction(Intent.ACTION_SEND);//设置分享行为
                intent.setType("image/*");//设置分享内容的类型
                intent.setData(uri);
                intent.putExtra("yaw",gimbalYaw);
                //Bundle b=new Bundle();
                //b.putParcelable("bitmap",bitmap);
                Log.i("ljytest",uri.toString());
                //intent.putExtras(b);
                startActivity(intent);
                dialog.dismiss();
            }
        });
        dialog.show();
    }
    @OnClick(R.id.btn_send)
    public void autoDetect(){
        //自动检测的方法
        final Bitmap bitmap = mVideoSurface.getBitmap();
        //弹出对话框
        android.app.AlertDialog.Builder builder =new android.app.AlertDialog.Builder(this,R.style.AlertDialog);
        View view =View.inflate(getApplicationContext(),R.layout.activity_dialog,null);
        builder.setView(view);
        builder.setCancelable(false);
        final android.app.AlertDialog dialog=builder.create();
        ImageView toshow = (ImageView) view.findViewById(R.id.sendimg);
        toshow.setImageBitmap(bitmap);
        Button cancel =(Button) view.findViewById(R.id.cancelDialog);
        Button ok= (Button)view.findViewById(R.id.okDialog);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, null,null));
                //将 Bitmap 转成 uri
                Intent intent=new Intent(getApplicationContext(),ServerActivity.class);
                intent.setAction(Intent.ACTION_SEND);//设置分享行为
                intent.setType("image/*");//设置分享内容的类型
                intent.setData(uri);
                intent.putExtra("yaw",gimbalYaw);
                //Bundle b=new Bundle();
                //b.putParcelable("bitmap",bitmap);
                Log.i("ljytest",uri.toString());
                //intent.putExtras(b);
                startActivity(intent);
                dialog.dismiss();
            }
        });
        dialog.show();


    }


    @OnClick(R.id.btn_detect)
    public void detect() {
        autoFlag=false;
        detectUsingBitmap();
    }

    private void detectUsingBitmap() {
        if (mTaskHandler.isDetecting()) {
            showToast("已经处于检测中");
            return;
        }

        btnDetect.setText("检测中...");

        final int measureType = rbMeasurePitch.isChecked() ? MeasureType.TYPE_PITCH : MeasureType.TYPE_AZIMUTH;

        //通过选择类型进行线程
        mTaskHandler.startDetecting(measureType, new CompletedCallback<FrameTaskResult>() {


            @Override
            public void onCompleted(final FrameTaskResult result) {
                //处理帧的结果
                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        btnDetect.setText("检测");

                        // showLongToast("检测完毕: " + result.time);
                    }
                });

                Intent intent = new Intent(MainActivity.this, ImageAndDataActivity.class);

                intent.putExtra(ExtraKey.KEY_MEASURE_TYPE, measureType);
                intent.putExtra(ExtraKey.KEY_TASK_RESULT, result);
                //result 是包含着相关数据的

                startActivity(intent);
            }


        });
    }

    private void detectUsingYUV() {
//        mCodecManager.enabledYuvData(true);
//        mCodecManager.setYuvDataCallback(this);
    }

    @Override
    public void onYuvDataReceived(ByteBuffer yuvBuffer, int dataSize, final int width, final int height) {
        Log.e(TAG, "S: " + dataSize + ", W: " + width + ", H: " + height);

        final byte[] yuvFrame = new byte[dataSize];
        yuvBuffer.get(yuvFrame);

        YUVData yuvData = new YUVData(yuvFrame, new opencv_core.Size(width, height));
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");

        super.onResume();
        loadPreviewer();
        onProductChange();

        if(mVideoSurface == null) {
            Log.e(TAG, "mVideoSurface is null");
        }

        mTimer = Executors.newSingleThreadScheduledExecutor();

        // 每隔 500ms 从 TextureView 中截取一帧进行检测
        // bitmap 即为所需要的每一帧
        mTimer.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (mVideoSurface != null && mTaskHandler.isDetecting() && autoFlag==false) {
                    Bitmap bitmap = mVideoSurface.getBitmap();
                    if (bitmap != null) {
                        Rect selectedRegion = regionSelector.getSelectedRegion();  // 框选的区域
                        mTaskHandler.handleFrame(bitmap, selectedRegion, gimbalYaw);
                        //核心！  传入设备的俯仰角
                    }
                }
            }

        }, 0, 500, TimeUnit.MILLISECONDS);

    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");

        mTimer.shutdownNow();
        mTimer = null;

        unloadPreviewer();
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");

        unloadPreviewer();

        mTaskHandler.release();
        mTaskHandler = null;

        super.onDestroy();
    }

    private void initUI() {
        // init mVideoSurface
        mVideoSurface = (TextureView)findViewById(R.id.video_previewer);
        mVideoSurface.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }
    }

    private void loadPreviewer() {
        //载入无人机实例、设置 textureview、设置视频回调
        BaseProduct product = FPVApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            showToast(getString(R.string.disconnected));

        } else {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }

            //设置好回调接口
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(mReceivedVideoDataCallBack);
            }
        }
    }

    private void unloadPreviewer() {
        Camera camera = FPVApplication.getCameraInstance();

        if (camera != null){
            // reset the callback
            VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(null);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");


        //当 texutureview 可用的时候， 初始化 mCodecManager 编码
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG,"onSurfaceTextureDestroyed");

        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }

        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void showLongToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @OnClick(R.id.btn_capture)
    public void capture() {
        final Camera camera = FPVApplication.getCameraInstance();

        if (camera != null) {
            // Set the camera capture mode as Single mode
            camera.setShootPhotoMode(SettingsDefinitions.ShootPhotoMode.SINGLE, new CommonCallbacks.CompletionCallback(){
                @Override
                public void onResult(DJIError djiError) {
                    if (null == djiError) {
                        capturePhoto(camera);

                    } else {
                        showLongToast("setShootPhoneMode error:\n" + djiError.getDescription());
                    }
                }
            });

        }
    }

    private void capturePhoto(Camera camera) {
        camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {

            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {
                    showToast("拍摄照片成功！");

                } else {
                    showToast("拍照失败：" + djiError.getDescription());
                }
            }
        });
    }

    @OnClick(R.id.btn_exit)
    public void exit() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setIcon(R.mipmap.ic_launcher) // 设置标题的图片
                .setTitle("提示")               // 设置对话框的标题
                .setMessage("确定退出测量？")     // 设置对话框的内容
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();

                        finish();
                    }
                })
                //设置对话框的按钮
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();

        dialog.show();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private Rect scale(Rect region, Rect screen, opencv_core.Size imageSize) {

        int SW = screen.width();
        int SH = screen.height();

        int IW = imageSize.width();
        int IH = imageSize.height();

        Rect s = new Rect(region);

        s.left = Maths.toInt(1.0 * s.left * IW / SW);
        s.top = Maths.toInt(1.0 * s.top * IH / SH);

        s.right = Maths.toInt(1.0 * s.right * IW / SW);
        s.bottom = Maths.toInt(1.0 * s.bottom * IH / SH);

        return s;
    }





    private Rect parseRect(String zuobiao){
        if(zuobiao==null){
            Toast.makeText(getApplicationContext(),"未接收坐标",Toast.LENGTH_LONG).show();
            return null;
        }
        String[] recZuobiao=zuobiao.split(" ");
        int[] newZuobiao=new int[4];
        if(recZuobiao.length!=6){
            Toast.makeText(getApplicationContext(),"接收坐标格式错误",Toast.LENGTH_LONG).show();
            return null;
        }
        for(int i=0;i<4;i++){
            newZuobiao[i]=Integer.parseInt(recZuobiao[i]);
        }

        /*接收数据
        * 0为 left
        * 1为 top
        * 2为 right
        * 3为 bottom
        * 4为 score
        * 5为 类型*/

        int left=newZuobiao[0];
        int top=newZuobiao[1];
        int right=newZuobiao[2];
        int bottom=newZuobiao[3];
        double score=Double.parseDouble(recZuobiao[4]);
        Log.i("ljytest",Double.toString(score));
        if(score<0.1){
            return null;
        }
        Rect roiRect=new Rect(left,top,right,bottom);
        Log.i("ljytest",Integer.toString(left)+","+Integer.toString(top)+","+Integer.toString(right)+","+Integer.toString(bottom));
        return roiRect;
    }


    private void sendImg(DataOutputStream os,Bitmap sendBitmap){
        if(sendBitmap==null){
            Toast.makeText(getApplicationContext(),"未选择图片",Toast.LENGTH_LONG).show();
        }else {
            //将 bitmap 转为字节数组
            try{
                ByteArrayOutputStream bout=new ByteArrayOutputStream();
                sendBitmap.compress(Bitmap.CompressFormat.JPEG,100,bout);
                //可设置压缩质量 quality
                byte[] data=bout.toByteArray();
                int length=bout.size();
                Log.i("ljytest","SendLength "+length);
                os.writeInt(length);
                os.write(data);
                os.flush();
            }catch (IOException e){
                e.printStackTrace();
            }

        }
    }
}
