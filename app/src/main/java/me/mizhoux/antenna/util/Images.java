package me.mizhoux.antenna.util;

import android.graphics.BitmapFactory;
import android.util.Pair;

public final class Images {

    public static Pair<Integer, Integer> getImageSize(String imgPath) {
		BitmapFactory.Options options = new BitmapFactory.Options();

		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(imgPath, options);

		return new Pair<>(options.outWidth, options.outHeight);
	}

    private Images() {}

}
