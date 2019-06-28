package me.mizhoux.antenna.util;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public final class Util {
	
	/**
	 * 文件协议头
	 */
	public static final String FILE_PROTOCOL = "file://";
	
	/**
	 * CPU 数目
	 */
	public static final int CPU_NUM = Runtime.getRuntime().availableProcessors();
	
	/**
	 * 换行符
	 */
	public static final String LINE_SEP = System.getProperty("line.separator");


	private Util() {}


	public static String saveBitmap(Bitmap mBitmap) {
		File file = new File("/storage/ljytest/pic/");   //FILE_DIR自定义
		if (!file.exists()) {
			file.mkdir();
		}
		File tmpf = new File(file, "test" + ".jpeg");



		try {

			FileOutputStream fOut = new FileOutputStream(tmpf);
			mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
			fOut.flush();
			fOut.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String image_file_url=tmpf.getAbsolutePath();
		Log.i("image_file_url", image_file_url);
		return image_file_url;
	}





}
