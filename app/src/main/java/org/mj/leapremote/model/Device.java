package org.mj.leapremote.model;

import android.content.res.Resources;
import android.graphics.Color;

import org.mj.leapremote.R;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class Device {
    public static final int MODE_PLAIN = 0;
    public static final int MODE_DIRECT = 1;
    public static final int OS_UNKNOWN = -1;
    public static final int OS_ANDROID = 0;
    public static final int OS_WINDOWS = 1;
    public static final int STATUS_OFFLINE = 0;
    public static final int STATUS_ONLINE = 1;
    public static final int STATUS_DIRECT = 5;
    public static final int STATUS_NOT_ENABLED = 2;
    public static final int STATUS_NOT_SUPPORTED = 3;
    public static final int STATUS_UNKNOWN = 4;

    private int mode;
    private int status;
    private String deviceId;
    private String connectId;
    private String connectPin;
    private String name;
    private String ip;
    private int port;

    public String getStatusString(Resources resources) {
        switch (status) {
            case STATUS_OFFLINE:
                return resources.getString(R.string.offline);
            case STATUS_ONLINE:
                return resources.getString(R.string.online);
            case STATUS_NOT_ENABLED:
                return resources.getString(R.string.not_enabled);
            case STATUS_NOT_SUPPORTED:
                return resources.getString(R.string.not_supported);
            case STATUS_DIRECT:
                return resources.getString(R.string.direct_enabled);
            default:
                return resources.getString(R.string.unknown);
        }
    }

    public int getStatusColor() {
        switch (status) {
            case STATUS_ONLINE:
            case STATUS_DIRECT:
                return Color.GREEN;
            case STATUS_NOT_ENABLED:
                return Color.BLUE;
            default:
                return Color.GRAY;
        }
    }
}
