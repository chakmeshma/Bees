package net.chakmeshma.brutengine.utilities;

import android.content.Context;

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
}
