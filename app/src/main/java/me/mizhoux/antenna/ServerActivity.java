package me.mizhoux.antenna;

/**
 * Created by ljy on 2019/6/5.
 */

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import me.mizhoux.antenna.constant.ExtraKey;
import me.mizhoux.antenna.imgproc.FrameTaskResult;
import me.mizhoux.antenna.imgproc.Line;
import me.mizhoux.antenna.imgproc.MeasureType;
import me.mizhoux.antenna.imgproc.bitmap.BitmapTaskHandler;
import me.mizhoux.antenna.util.CompletedCallback;
import me.mizhoux.antenna.util.Maths;
import me.mizhoux.antenna.util.Util;

import static org.bytedeco.javacpp.opencv_imgproc.Canny;
import static org.bytedeco.javacpp.opencv_imgproc.createLineSegmentDetector;

public class ServerActivity extends Activity {
    private static final int POST=1030;
    private static final String HOST="192.168.1.105";
    private Button send,detect,cancel;
    private ProgressBar progressBar;
    private ImageView img;
    private Bitmap sendBitmap;
    private Bitmap result=null;
    private TextView text1,text2;
    private String zuobiao=null;
    private float Yaw=0f;
    private Rect toDrawRect=null;
    private boolean finish=false;

    private final static int REQUEST_WRITE=1;
    private final static int REQUEST_PICK=2;

    private final static int UPDATE_PICTURE=3;
    private final static int UPDATE_BAR=4;
    private final static int UPDATE_TEXT=5;
    private final static int NO_OBJECT=6;
    private static boolean CONNECTED=false;
    private static boolean textOK=false;//标识符 图片接收成功，开始接收文字

    private BitmapTaskHandler mTaskHandler = new BitmapTaskHandler(1);

    private Handler handler =new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case UPDATE_TEXT:
                    text2.setText(zuobiao);

                case UPDATE_BAR:
                    //更新进度条
                    progressBar.setVisibility(View.GONE);
                    text1.setVisibility(View.GONE);
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        img=(ImageView)findViewById(R.id.image);
        send=(Button) findViewById(R.id.send);
        detect=(Button)findViewById(R.id.detectMingpai);
        progressBar =(ProgressBar)findViewById(R.id.bar);
        text1=(TextView)findViewById(R.id.text);
        text2=(TextView)findViewById(R.id.zuobiao);
        text1.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);


        try {
            Intent intent = getIntent();
            Uri uri = intent.getData();
            Yaw=0.0f;
            Toast.makeText(getApplicationContext(),"请选择需要检测的类型",Toast.LENGTH_SHORT).show();
            sendBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            Log.i("sendBitmap:",sendBitmap.getConfig().toString());
            //获取上一个 Activity 传的 bitmap
            img.setImageBitmap(sendBitmap);
            //send();
            send.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //发送天线姿态图片
                    send(1030);
                    //receive();

                }
            });

            detect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //发送铭牌图片
                    send(1061);
                }
            });
        } catch (IOException e){
            e.printStackTrace();
        }
//        receive.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                receive();
//            }
//        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void send(final int POST){
        Log.i("ljytest","clickSend");
        //Socket 发送
        if(sendBitmap==null){
            Toast.makeText(getApplicationContext(),"未选择图片",Toast.LENGTH_LONG).show();
        }else {
            progressBar.setVisibility(View.VISIBLE);
            text1.setVisibility(View.VISIBLE);
            new Thread() {
                @Override
                public void run() {
                    try {
                        Log.i("ljytest","即将发送图片");
                        Socket socket = new Socket(HOST, POST);
                        //指定服务器地址
                        DataOutputStream os = new DataOutputStream(socket.getOutputStream());
                        //输出流

                        sendImg(os);

                        Log.i("ljytest", "发送完成");
                        //发送图片完等待服务器发送完成标识符

                        Log.i("ljytest","等待处理图片");
                        BufferedReader br =new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        //br.readLine()阻塞

                        //接收图片
                        while(br.readLine()!=null&&!CONNECTED){
                            Log.i("ljytest","服务器处理完毕");
                            Socket recsocket=new Socket(HOST,1031);

                            long startTime =System.currentTimeMillis();
                            //开始时间

                            DataInputStream in =new DataInputStream(recsocket.getInputStream());
                            int size=in.readInt();//图片长度
                            byte[] data =new byte[size];
                            int length=0;
                            Log.i("ljytest","receiveLength is"+size);
                            //in.readFully(data);
                            ByteArrayOutputStream bos=new ByteArrayOutputStream();
                            Log.i("ljytest","开始接受数据....");
                            while(length<size){
                                length += in.read(data,length,size-length);
                            }
                            bos.flush();
                            result=BitmapFactory.decodeByteArray(data,0,data.length).copy(Bitmap.Config.ARGB_8888,true);
                            result.compress(Bitmap.CompressFormat.JPEG,100,bos);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    img.setImageBitmap(result);
                                }
                            });
                            textOK=true;
                            Message message=new Message();
                            message.what=UPDATE_BAR;
                            handler.sendMessage(message);
                            Log.i("ljytest","完成....");

                        }
                        //接收文字
                        while(textOK){
                            Log.i("ljytest","开始接收坐标");
                            Socket textsocket=new Socket(HOST,1035);
                            DataInputStream in =new DataInputStream(textsocket.getInputStream());
                            int length =in.readInt();
                            byte[] bytes=new byte[length];
                            in.read(bytes);
                            zuobiao =new String(bytes);
                            Message message=new Message();
                            message.what=UPDATE_TEXT;
                            handler.sendMessage(message);
                            textOK=false;
                            Log.i("ljytest","坐标接收完成");
                        }

                        os.close();
                        socket.close();

                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

    private void sendImg(DataOutputStream os){
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
