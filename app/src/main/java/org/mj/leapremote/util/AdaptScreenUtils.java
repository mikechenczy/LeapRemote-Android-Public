package org.mj.leapremote.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class AdaptScreenUtils {

    private static List<Field> sMetricsFields;

    private AdaptScreenUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }


    private static void applyDisplayMetrics(Context context, @NonNull final Resources resources, final float newXdpi) {
        resources.getDisplayMetrics().xdpi = newXdpi;
        context.getResources().getDisplayMetrics().xdpi = newXdpi;
        applyOtherDisplayMetrics(resources, newXdpi);
    }

    static Runnable getPreLoadRunnable(Context context) {
        return () -> preLoad(context);
    }

    private static void preLoad(Context context) {
        applyDisplayMetrics(context, Resources.getSystem(), Resources.getSystem().getDisplayMetrics().xdpi);
    }

    private static void applyOtherDisplayMetrics(final Resources resources, final float newXdpi) {
        if (sMetricsFields == null) {
            sMetricsFields = new ArrayList<>();
            Class resCls = resources.getClass();
            Field[] declaredFields = resCls.getDeclaredFields();
            while (declaredFields != null && declaredFields.length > 0) {
                for (Field field : declaredFields) {
                    if (field.getType().isAssignableFrom(DisplayMetrics.class)) {
                        field.setAccessible(true);
                        DisplayMetrics tmpDm = getMetricsFromField(resources, field);
                        if (tmpDm != null) {
                            sMetricsFields.add(field);
                            tmpDm.xdpi = newXdpi;
                        }
                    }
                }
                resCls = resCls.getSuperclass();
                if (resCls != null) {
                    declaredFields = resCls.getDeclaredFields();
                } else {
                    break;
                }
            }
        } else {
            applyMetricsFields(resources, newXdpi);
        }
    }

    private static void applyMetricsFields(final Resources resources, final float newXdpi) {
        for (Field metricsField : sMetricsFields) {
            try {
                DisplayMetrics dm = (DisplayMetrics) metricsField.get(resources);
                if (dm != null) dm.xdpi = newXdpi;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static DisplayMetrics getMetricsFromField(final Resources resources, final Field field) {
        try {
            return (DisplayMetrics) field.get(resources);
        } catch (Exception ignore) {
            return null;
        }
    }
}
