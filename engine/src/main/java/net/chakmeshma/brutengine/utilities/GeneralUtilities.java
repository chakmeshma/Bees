package net.chakmeshma.brutengine.utilities;

import android.util.DisplayMetrics;

import net.chakmeshma.brutengine.Engine;

/**
 * Created by chakmeshma on 17.05.2017.
 */

public final class GeneralUtilities {

    public static int pxToDp(int px) {
        DisplayMetrics displayMetrics = Engine.context.getResources().getDisplayMetrics();
        return Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }
}
