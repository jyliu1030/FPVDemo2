package me.mizhoux.antenna;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;

import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.secneo.sdk.Helper;


public class MApplication extends Application {

    private FPVApplication fpvApplication;
    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);

        Helper.install(MApplication.this);
        if (fpvApplication == null) {
            fpvApplication = new FPVApplication();
            fpvApplication.setContext(this);
        }


    }

    @Override
    public void onCreate() {
        super.onCreate();
        fpvApplication.onCreate();

        DisplayImageOptions defaultOptions = new DisplayImageOptions
                .Builder()
                .showImageForEmptyUri(R.mipmap.ic_img_none)
                .showImageOnFail(R.mipmap.ic_img_none)
                .cacheInMemory(true) //设置图片缓存于内存中
                .bitmapConfig(Bitmap.Config.RGB_565)    //设置图片的质量
                .imageScaleType(ImageScaleType.IN_SAMPLE_INT)    //设置图片的缩放类型，该方法可以有效减少内存的占用
                .build();

        ImageLoaderConfiguration config = new ImageLoaderConfiguration
                .Builder(getApplicationContext())
                .defaultDisplayImageOptions(defaultOptions)
                .threadPoolSize(2)
                .memoryCache(new WeakMemoryCache())
                //		.memoryCacheSize(50 * 1024 * 1024)
                .writeDebugLogs()
                .build();

        ImageLoader.getInstance().init(config);
    }

}
