package net.chakmeshma.brutengine;

import android.content.Context;

import net.chakmeshma.brutengine.utilities.AssetFileReader;

public class Engine {
    public static void init(Context context) {
        AssetFileReader.initSingleton(context);
    }
}
