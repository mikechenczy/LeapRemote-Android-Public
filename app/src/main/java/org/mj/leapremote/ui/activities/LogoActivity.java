package org.mj.leapremote.ui.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSONObject;
import org.mj.leapremote.Define;
import org.mj.leapremote.R;
import org.mj.leapremote.service.HttpService;
import org.mj.leapremote.util.DataUtil;
import org.mj.leapremote.util.UpdateManager;
import org.mj.leapremote.util.Utils;

public class LogoActivity extends AppCompatActivity {
    public static Activity INSTANCE;
    public static boolean alive = true;
    public ProgressDialog progressDialog;
    public boolean readMessage;
    public String messageContent;
    public AlertDialog messageDialog;
    public TextView errorMessage;


    public void onClickNeverShow(View view) {
        Define.neverShowMessageDialog = messageContent;
        new Thread(DataUtil::save).start();
        onClickAdmit(view);
    }

    public void onClickAdmit(View view) {
        messageDialog.dismiss();
        login();
    }

    public class MessageThread extends Thread {
        @Override
        public void run() {
            messageContent = HttpService.getMessageContent();
            if(messageContent==null) {
                runOnUiThread(() -> Toast.makeText(LogoActivity.this, R.string.cannot_connect_to_server, Toast.LENGTH_SHORT).show());
                readMessage = true;
                login();
            } else {
                readMessage = true;
                if (!(messageContent.equals(Define.neverShowMessageDialog) || messageContent.equals("No message")))
                    runOnUiThread(() -> showMessage(LogoActivity.this));
                else
                    runOnUiThread(LogoActivity.this::login);
            }
        }
    }

    public void showMessage(Context context) {
        final View inflate = LayoutInflater.from(context).inflate(R.layout.dialog_message, null);
        TextView tv_title = inflate.findViewById(R.id.tv_title);
        tv_title.setText(R.string.notice);
        TextView tv_content = inflate.findViewById(R.id.tv_content);
        tv_content.setText(messageContent);
        messageDialog = new AlertDialog
                .Builder(this)
                .setView(inflate)
                .show();
        // 通过WindowManager获取
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        final WindowManager.LayoutParams params = messageDialog.getWindow().getAttributes();
        params.width = dm.widthPixels*4/5;
        params.height = dm.heightPixels*3/5;
        messageDialog.getWindow().setAttributes(params);
        messageDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }

    public class UpdateThread extends Thread {
        @Override
        public void run() {
               /*if(!TcpService.getRealIp()) {
                runOnUiThread(() -> updateText.setText(R.string.can_not_connect_to_server));
                return;
            }*/
            JSONObject s = HttpService.needUpdate();
            if(s==null) {
                runOnUiThread(() -> showErrorMessage(getResources().getText(R.string.cannot_connect_to_server).toString()));
                login();
                //new StartThread().start();
                return;
            }
            runOnUiThread(() -> {
                if (s.get("force")==null || (!s.getBoolean("force") && s.getString("version").equals(Define.neverShowVersionDialog))) {
                    login();
                } else {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(LogoActivity.this).setTitle(R.string.newVersion).setMessage(getString(R.string.version) + ":" + s.getString("version") + "\n" + getString(R.string.content) + ":" + s.getString("description") + "\n" + (s.getBoolean("force") ? getString(R.string.forceUpdate) : "") + getString(R.string.toUpdate))
                            .setPositiveButton(R.string.yes, (dialog, which) -> {
                                progressDialog = new ProgressDialog(LogoActivity.this);
                                progressDialog.setTitle(R.string.downloadingUpdate);
                                //设置水平进度条
                                progressDialog.setProgressStyle(progressDialog.STYLE_HORIZONTAL);
                                //设置进度条最大值为100
                                progressDialog.setMax(100);
                                //设置进度条当前值为0
                                progressDialog.setProgress(0);
                                progressDialog.setCancelable(!s.getBoolean("force"));// 设置是否可以通过点击Back键取消
                                progressDialog.setCanceledOnTouchOutside(false);
                                progressDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.cancel), (dialog1, which1) -> {
                                    if (s.getBoolean("force")) {
                                        System.exit(0);
                                    }
                                });
                                UpdateManager updateManager = new UpdateManager(LogoActivity.this);
                                updateManager.update();
                                progressDialog.setOnDismissListener((dialog1 -> {
                                    if (s.getBoolean("force")) {
                                        System.exit(0);
                                    }
                                    updateManager.downloadApkThread.interrupt();
                                    login();
                                }));
                                progressDialog.show();
                            })
                            .setOnCancelListener((v) -> {
                                if (s.getBoolean("force")) {
                                    System.exit(0);
                                }
                                login();
                            })
                            .setNegativeButton(R.string.no, ((dialog, which) -> {
                                if (s.getBoolean("force")) {
                                    System.exit(0);
                                }
                                login();
                            }));
                    if (!s.getBoolean("force")) {
                        alertDialogBuilder.setNeutralButton(R.string.neverShowVersionDialog, ((dialog, which) -> {
                            new Thread(() -> {
                                Define.neverShowVersionDialog = s.getString("version");
                                DataUtil.save();
                            }).start();
                            login();
                        }));
                    } else {
                        alertDialogBuilder.setCancelable(false);
                    }
                    alertDialogBuilder.show();
                }
            });
        }
    }

    private void login() {
        if(!readMessage) {
            new MessageThread().start();
            return;
        }
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void showErrorMessage(String message){
        errorMessage.setText(message);
        errorMessage.setVisibility(View.VISIBLE);
    }


    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        INSTANCE = this;
        Define.version = Utils.getVersion(this);
        Define.displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(Define.displayMetrics);
        setContentView(R.layout.activity_logo);
        DataUtil.sharedPreferences = getSharedPreferences("yan", MODE_PRIVATE);
        ((TextView) findViewById(R.id.versionText)).setText(getString(R.string.version) + Define.version);
        errorMessage = findViewById(R.id.error_message);
        TextView initializing = findViewById(R.id.initializing);
        new Thread(() -> {
            while (alive) {
                runOnUiThread(() -> initializing.setText(getString(R.string.initializing)+"."));
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    return;
                }
                runOnUiThread(() -> initializing.setText(getString(R.string.initializing)+".."));
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    return;
                }
                runOnUiThread(() -> initializing.setText(getString(R.string.initializing)+"..."));
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    return;
                }
                runOnUiThread(() -> initializing.setText(getString(R.string.initializing)+"...."));
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }).start();
        Thread thread = new Thread(() -> {
            while (!Define.ipGot) {
                try {
                    Thread.sleep(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            }
            new UpdateThread().start();
        });
        thread.start();
        new Thread(() -> {
            try {
                Thread.sleep(Define.waitTimeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(thread.isAlive()) {
                thread.interrupt();
                new UpdateThread().start();
            }
        }).start();
    }
}
