package me.mizhoux.antenna;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import me.mizhoux.antenna.view.ZoomImageView;

public class ZoomActivity extends AppCompatActivity {

    ZoomImageView ziv;
    Button btngetimage;
    Bitmap recBitmap;


    @Override

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zoom);

//        for(int i=0;i<imgIds.length;i++)
//        {
        Intent intent = getIntent();
        Uri uri = intent.getData();
        try {
            recBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ZoomImageView zoomImageView = (ZoomImageView) findViewById(R.id.ziv);
        zoomImageView.setImageBitmap(recBitmap);

        //final Bitmap bitmap1 = ((BitmapDrawable)zoomImageView.getDrawable()).getBitmap();

        btngetimage = (Button) findViewById(R.id.btn_get_img);
        View cut = findViewById(R.id.cut);
        final View dView = cut;
        //final Bitmap bitmap1 = bitmap;

        btngetimage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                //Bitmap bitmap = ((BitmapDrawable)ziv.).getBitmap();
                    dView.setDrawingCacheEnabled(true);
                    dView.buildDrawingCache();
                    Bitmap newBitmap = Bitmap.createBitmap(dView.getDrawingCache());
                    //文件输出流
                    Uri uri = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), newBitmap, null,null));
                    //将 Bitmap 转成 uri
                    Intent intent=new Intent();
                    intent.setAction(Intent.ACTION_SEND);//设置分享行为
                    intent.setType("image/*");//设置分享内容的类型
                    intent.setData(uri);
                    setResult(3,intent);
                    finish();

            }
        });
    }
}
