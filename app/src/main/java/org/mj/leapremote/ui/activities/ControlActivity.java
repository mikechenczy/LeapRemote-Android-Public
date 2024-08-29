package org.mj.leapremote.ui.activities;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.mj.leapremote.Define;
import org.mj.leapremote.R;
import org.mj.leapremote.coder.ScreenDecoder;
import org.mj.leapremote.cs.ClientHandler;
import org.mj.leapremote.cs.direct.NettyClientDirect;
import org.mj.leapremote.service.AutoService;
import org.mj.leapremote.util.ClientHelper;
import org.mj.leapremote.util.DataUtil;
import org.mj.leapremote.util.ImageUtils;
import org.mj.leapremote.util.SendCommandHelper;
import org.mj.leapremote.util.Utils;

import java.util.ArrayList;
import java.util.List;

import io.netty.util.internal.StringUtil;

public class ControlActivity extends AppCompatActivity {
    public static ControlActivity INSTANCE;
    public ImageView capturedImage;
    public TextureView record;
    //public Button unlock;
    public Button lock;
    public Button back;
    public Button home;
    public Button recent;
    public Button volumeUp;
    public Button volumeDown;
    public Button volumeMute;
    public Button gestures;
    public Button showButtons;
    public Button hideButtons;
    public Button fullscreenButton;
    public Button rotate;
    public LinearLayout buttonsLayout;
    //public SeekBar scale;
    //public SeekBar quality;
    public SeekBar qualitySeekBar;
    public SendCommandHelper sendCommandHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        INSTANCE = this;
        setContentView(R.layout.activity_control);
        capturedImage = findViewById(R.id.captured_image);
        record = findViewById(R.id.record);
        /*record.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                ScreenDecoder.startDecode(holder.getSurface());
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }
        });*/
        record.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                ScreenDecoder.startDecode(new Surface(surface));
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
            }
        });
        //Matrix matrix = new Matrix();
        //matrix.postRotate(90);
        //matrix.postTranslate(100, 0);
        //record.setTransform(matrix);

        //unlock = findViewById(R.id.unlock);
        lock = findViewById(R.id.lock);
        back = findViewById(R.id.back);
        home = findViewById(R.id.home);
        recent = findViewById(R.id.recent);
        volumeUp = findViewById(R.id.volume_up);
        volumeDown = findViewById(R.id.volume_down);
        volumeMute = findViewById(R.id.volume_mute);
        gestures = findViewById(R.id.gestures);
        showButtons = findViewById(R.id.show_buttons_button);
        hideButtons = findViewById(R.id.hide_buttons_button);
        fullscreenButton = findViewById(R.id.fullscreen);
        rotate = findViewById(R.id.rotate_button);
        buttonsLayout = findViewById(R.id.buttons_layout);
        //scale = findViewById(R.id.scale);
        //quality = findViewById(R.id.quality);
        qualitySeekBar = findViewById(R.id.qualitySeekBar);
        sendCommandHelper = new SendCommandHelper(getApplicationContext());
        sendCommandHelper.doSendResolution(50);
        setListeners();
        if(!Utils.stringIsEmpty(Define.controlSavedGestures)) {
            setGestures(Define.controlSavedGestures);
        }
        //startService(new Intent(MainActivity.this, FloatingView.class));
    }

    long startTime;
    boolean pressing = false;
    JSONArray points = new JSONArray();
    boolean fullscreen;
    int originSystemUiVisibility;
    //Thread current;
    private void setListeners() {
        showButtons.setOnClickListener(v -> {
            //findViewById(R.id.available_layout).setVisibility(View.GONE);
            buttonsLayout.setVisibility(View.VISIBLE);
            showButtons.setVisibility(View.GONE);
        });
        hideButtons.setOnClickListener(v -> {
            buttonsLayout.setVisibility(View.GONE);
            showButtons.setVisibility(View.VISIBLE);
        });
        fullscreenButton.setOnClickListener(v -> {
            if(!fullscreen){
                originSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                fullscreen = true;
                fullscreenButton.setBackgroundResource(R.drawable.fullscreen_disable);
                return;
            }
            getWindow().getDecorView().setSystemUiVisibility(originSystemUiVisibility);
            fullscreen = false;
            fullscreenButton.setBackgroundResource(R.drawable.fullscreen);
        });
        rotate.setOnClickListener(v -> {
            if (rotateState < 3)
                rotateState += 1;
            else
                rotateState = 0;
            ScreenDecoder.stopDecode();
            ScreenDecoder.ROTATION = rotateState*90;
            System.out.println("ROTATION"+ScreenDecoder.ROTATION);
            sendCommandHelper.doSendRestartMedia();
            ScreenDecoder.startDecode(new Surface(record.getSurfaceTexture()));
        });
        //unlock.setOnClickListener(v -> sendCommandHelper.doSendKey("unlock"));
        lock.setOnClickListener(v -> sendCommandHelper.doSendKey("lockOrUnlock"));
        back.setOnClickListener(v -> sendCommandHelper.doSendButton(AccessibilityService.GLOBAL_ACTION_BACK));
        home.setOnClickListener(v -> sendCommandHelper.doSendButton(AccessibilityService.GLOBAL_ACTION_HOME));
        recent.setOnClickListener(v -> sendCommandHelper.doSendButton(AccessibilityService.GLOBAL_ACTION_RECENTS));
        volumeUp.setOnClickListener(v -> sendCommandHelper.doSendKey("volumeUp"));
        volumeDown.setOnClickListener(v -> sendCommandHelper.doSendKey("volumeDown"));
        volumeMute.setOnClickListener(v -> sendCommandHelper.doSendKey("volumeMute"));
        qualitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sendCommandHelper.doSendQuality(100-progress, 100-progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //System.out.println("STTTOOOPP");
                ScreenDecoder.updateResolution(seekBar.getProgress());//+30);
                sendCommandHelper.doSendResolution(seekBar.getProgress());//+30);
                //ScreenDecoder.updateResolution(seekBar.getProgress()+30);
                //sendCommandHelper.doSendResolution(seekBar.getProgress()+30);
            }
        });
        sendCommandHelper.doSendQuality(100-qualitySeekBar.getProgress(), 100-qualitySeekBar.getProgress());
        /*scale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sendCommandHelper.doSendQuality(progress, quality.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        quality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sendCommandHelper.doSendQuality(scale.getProgress(), progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });*/
        capturedImage.setOnTouchListener((v, e) -> {
            if (e.getAction() == 0) {
                pressing = true;
                points = new JSONArray();
                startTime = System.currentTimeMillis();
                JSONObject jsonObject = new JSONObject();
                float[] realPos = realPos(e.getX(), e.getY());
                jsonObject.put("x", realPos[0]>0?realPos[0]:0);
                jsonObject.put("y", realPos[1]>0?realPos[1]:0);
                points.add(jsonObject);
                return true;
            }
            long now = System.currentTimeMillis();
            JSONObject jsonObject = new JSONObject();
            float[] realPos = realPos(e.getX(), e.getY());
            jsonObject.put("x", realPos[0]>0?realPos[0]:0);
            jsonObject.put("y", realPos[1]>0?realPos[1]:0);
            jsonObject.put("duration", now - startTime);
            points.add(jsonObject);
            startTime = now;

            if (e.getAction() == 1) {
                pressing = false;
                sendCommandHelper.doSendAction(points);
            }
            return true;
        });
        record.setOnTouchListener((v, e) -> {
            if (e.getAction() == 0) {
                pressing = true;
                points = new JSONArray();
                startTime = System.currentTimeMillis();
                JSONObject jsonObject = new JSONObject();
                float[] realPos = realPos(e.getX(), e.getY());
                jsonObject.put("x", realPos[0]>0?realPos[0]:0);
                jsonObject.put("y", realPos[1]>0?realPos[1]:0);
                points.add(jsonObject);
                return true;
            }
            long now = System.currentTimeMillis();
            JSONObject jsonObject = new JSONObject();
            float[] realPos = realPos(e.getX(), e.getY());
            jsonObject.put("x", realPos[0]>0?realPos[0]:0);
            jsonObject.put("y", realPos[1]>0?realPos[1]:0);
            jsonObject.put("duration", now - startTime);
            points.add(jsonObject);
            startTime = now;

            if (e.getAction() == 1) {
                pressing = false;
                sendCommandHelper.doSendAction(points);
            }
            return true;
        });
    }

    public int rotateState;

    private boolean remoteRotateState;

    private float[] realPos(float x, float y) {
        if(capturedImage==null)
            return new float[2];
        //int width = capturedImage.getWidth();
        //int height = capturedImage.getHeight();
        int width = record.getWidth();
        int height = record.getHeight();
        switch (rotateState) {
            case 0:
                return dealWithRemoteRotate(width, height, x, y);
            case 1:
                return dealWithRemoteRotate(height, width, y, width-x);
            case 2:
                return dealWithRemoteRotate(width, height, width-x, height-y);
            case 3:
                return dealWithRemoteRotate(height, width, height-y, x);
            default:
                return new float[2];
        }

    }

    private float[] dealWithRemoteRotate(int width, int height, float x, float y) {
        if(remoteRotateState) {
            return new float[]{y/height, (width - x)/width};
        }
        return new float[]{x/width, y/height};
    }

    public void updateImage(Bitmap bitmap) {
        int availableWidth = findViewById(R.id.available_layout).getMeasuredWidth();
        int availableHeight = findViewById(R.id.available_layout).getMeasuredHeight();
        //System.out.println(bitmap.getWidth()+"  "+bitmap.getHeight());
        try {
            bitmap = ImageUtils.compressBitmap(ImageUtils.rotateBitmap(bitmap, rotateState), availableWidth, availableHeight);
            capturedImage.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateRotate(boolean remoteRotate) {
        this.remoteRotateState = remoteRotate;
    }

    public boolean destroying;

    @Override
    protected void onDestroy() {
        destroying = true;
        rotateState = 0;
        super.onDestroy();
        new Thread(ScreenDecoder::stopDecode).start();
        if(Define.direct && NettyClientDirect.INSTANCE!=null) {
            NettyClientDirect.INSTANCE.interrupt();
        } else {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "exitControl");
            ClientHelper.sendMessage(this, jsonObject.toJSONString());
            Define.controlId = 0;
        }
    }


    public void calculateSize(int width, int height) {
        new Thread(() -> {
            try {
                int availableWidth = findViewById(R.id.available_layout).getMeasuredWidth();
                int availableHeight = findViewById(R.id.available_layout).getMeasuredHeight();
                int videoWidth = new Integer(width);
                int videoHeight = new Integer(height);
                if (rotateState == 1 || rotateState == 3) {
                    videoWidth = new Integer(height);
                    videoHeight = new Integer(width);
                }
                int realWidth = availableWidth;
                int realHeight = Math.round((float) ((float) availableWidth / videoWidth) * videoHeight);
                if (realHeight > availableHeight) {
                    realWidth = Math.round((float) ((float) availableHeight / videoHeight) * videoWidth);
                    realHeight = availableHeight;
                }
        /*if(rotateState==1 || rotateState==3) {
            int temp = new Integer(realWidth);
            realWidth = new Integer(realHeight);
            realHeight = temp;
        }*/
                int finalRealWidth = realWidth;
                int finalRealHeight = realHeight;
                if (record.getWidth() == finalRealWidth && record.getHeight() == finalRealHeight)
                    return;
                runOnUiThread(() -> record.setLayoutParams(new LinearLayout.LayoutParams(finalRealWidth, finalRealHeight)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

    }

    public void setGestures(String savedGestures) {
        JSONArray savedGesture = JSONArray.parseArray(savedGestures);
        runOnUiThread(() -> {
            gestures.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(this, v);
                for(int i=0;i<savedGesture.size();i++) {
                    popupMenu.getMenu().add(0, i, i, savedGesture.getJSONObject(i).getString("name"));
                    // groupId, itemId, order, title
                }
                popupMenu.setOnMenuItemClickListener(item -> {
                    sendCommandHelper.doSendGestures(savedGesture.getJSONObject(item.getItemId()).getJSONArray("gesture"));
                    return true;
                });
                popupMenu.show();
            });
        });
    }
}
