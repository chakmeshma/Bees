package net.chakmeshma.brutengine.utilities;

import android.content.Context;
import android.util.DisplayMetrics;

import net.chakmeshma.brutengine.Engine;

/**
 * Created by chakmeshma on 17.05.2017.
 */

public final class GeneralUtilities {

    public static int pxToDp(int px) {
        Context context = (Context) Engine.context;

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }
}
