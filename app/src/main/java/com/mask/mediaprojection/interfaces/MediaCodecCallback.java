package com.mask.mediaprojection.interfaces;

import android.media.MediaCodec;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * 媒体录制回调
 * Created by lishilin on 2020/03/20
 */
public abstract class MediaCodecCallback {

    public void onSuccess(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {

    }

    /**
     * 失败
     */
    public void onFail() {

    }
}
