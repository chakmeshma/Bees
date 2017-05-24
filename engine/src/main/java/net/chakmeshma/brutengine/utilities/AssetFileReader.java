package net.chakmeshma.brutengine.utilities;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;


public final class AssetFileReader {
    private Context context;
    private static Object _instance;

    private AssetFileReader(Context context) {
        this.context = context;
    }

    public static AssetFileReader initSingleton(Context context) {
        AssetFileReader assetFileReader = new AssetFileReader(context);

        _instance = assetFileReader;

        return assetFileReader;
    }

    public static AssetFileReader getSingleton() {
        return (AssetFileReader) _instance;
    }

    public String getAssetFileAsString(String fileName) throws IOException {
        InputStream inputStream;

        inputStream = this.context.getAssets().open(fileName);

        int fileLength = inputStream.available();

        byte[] bytes = new byte[fileLength];

        inputStream.read(bytes, 0, fileLength);

        inputStream.close();

        String result = new String(bytes);

        return result;
    }
}
