package org.mj.leapremote.util;

import android.app.Application;
import android.content.Context;

public class UtilsBridge {
    private static Context context;
    public UtilsBridge(Context context) {
        UtilsBridge.context = context;
    }
    static void init(Application app) {
        UtilsActivityLifecycleImpl.INSTANCE.init(app);
    }

    static void unInit(Application app) {
        UtilsActivityLifecycleImpl.INSTANCE.unInit(app);
    }

    static void preLoad() {
        preLoad(AdaptScreenUtils.getPreLoadRunnable(context));
    }
    private static void preLoad(final Runnable... runs) {
        for (final Runnable r : runs) {
            ThreadUtils.getCachedPool().execute(r);
        }
    }

    static SPUtils getSpUtils4Utils() {
        SPUtils spUtils=new SPUtils(context);
        return spUtils.getInstance(context, "Utils");
    }
    static Application getApplicationByReflect() {
        return UtilsActivityLifecycleImpl.INSTANCE.getApplicationByReflect();
    }

}

