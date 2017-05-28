package net.chakmeshma.brutengine;

import net.chakmeshma.brutengine.android.GameActivity;

public class Engine {
    public static GameActivity context;

    public static void initContext(GameActivity context) {
        Engine.context = context;
    }
}
