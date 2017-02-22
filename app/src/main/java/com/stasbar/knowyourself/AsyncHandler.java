package com.stasbar.knowyourself;

import android.os.Handler;
import android.os.HandlerThread;

/**
 * Created by admin1 on 26.01.2017.
 */

public final class AsyncHandler {
    private static final HandlerThread sHandlerThread = new HandlerThread("AsyncHandler");
    private static final Handler sHandler;

    static {
        sHandlerThread.start();
        sHandler = new Handler(sHandlerThread.getLooper());
    }

    public static void post(Runnable r) {
        sHandler.post(r);
    }

    private AsyncHandler() {}
}
