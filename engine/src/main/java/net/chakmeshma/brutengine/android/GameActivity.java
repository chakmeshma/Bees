package net.chakmeshma.brutengine.android;

/**
 * Created by chakmeshma on 28.05.2017.
 */

public interface GameActivity {
    //region sendMessageToUIThreadHandler
    void sendMessageToUIThreadHandler(UIThreadMessageType messageType, int arg1, int arg2);

    void sendMessageToUIThreadHandler(UIThreadMessageType messageType, int arg1);

    void sendMessageToUIThreadHandler(UIThreadMessageType messageType);

    void incrementCountGLFlushes();

    enum UIThreadMessageType {
        MESSAGE_PART_LOADED,
        MESSAGE_EXTEND_LOAD_PARTS_COUNT
    }
}
