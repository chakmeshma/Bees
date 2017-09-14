package net.chakmeshma.brutengine.utilities;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import net.chakmeshma.brutengine.Engine;

import java.io.IOException;
import java.io.InputStream;


public final class AssetsUtilities {
    public static String getAssetFileAsString(String fileName) throws IOException {
        Context context = (Context) Engine.context;

        InputStream inputStream;

        inputStream = context.getAssets().open(fileName);

        int fileLength = inputStream.available();

        byte[] bytes = new byte[fileLength];

        inputStream.read(bytes, 0, fileLength);

        inputStream.close();

        String result = new String(bytes);

        return result;
    }

    public static InputStream openAssetFileInputStream(String fileName) throws IOException {
        Context context = (Context) Engine.context;

        return context.getAssets().open(fileName);
    }

    public static Bitmap getBitmapFromAsset(String filePath, boolean flipVertically) {
        Context context = (Context) Engine.context;

        AssetManager assetManager = context.getAssets();

        InputStream istr;
        Bitmap bitmap = null;
        try {
            istr = assetManager.open(filePath);
            bitmap = BitmapFactory.decodeStream(istr);
        } catch (IOException e) {
            // handle exception
        }

        if (flipVertically) {
            Matrix m = new Matrix();
            m.preScale(1, -1);

            Bitmap flippedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, false);

            return flippedBitmap;
        } else {
            return bitmap;
        }
    }
}
