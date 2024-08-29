package org.mj.leapremote.util;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import androidx.core.content.FileProvider;

import org.mj.leapremote.Define;
import org.mj.leapremote.ui.activities.LogoActivity;
import org.mj.leapremote.ui.activities.MainActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *@author coolszy
 *@date 2012-4-26
 *@blog http://blog.92coding.com
 */

public class UpdateManager
{
    /* 下载中 */
    private static final int DOWNLOAD = 1;
    /* 下载结束 */
    private static final int DOWNLOAD_FINISH = 2;
    /* 下载保存路径 */
    private String mSavePath;
    /* 记录进度条数量 */
    private int progress;

    private Object mContext;

    public downloadApkThread downloadApkThread;

    private Handler mHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                // 正在下载
                case DOWNLOAD:
                    // 设置进度条位置
                    /*if(mContext instanceof LoginActivity)
                        ((LoginActivity) mContext).progressDialog.setProgress(progress);
                    else if(mContext instanceof SettingsActivity)
                        ((SettingsActivity) mContext).progressDialog.setProgress(progress);
                    else
                        ((RoomsActivity) mContext).progressDialog.setProgress(progress);*/
                    break;
                case DOWNLOAD_FINISH:
                    // 安装文件
                    installApk();
                    break;
                default:
                    break;
            }
        };
    };

    public UpdateManager(Object context)
    {
        this.mContext = context;
    }

    public void update() {
        downloadApk();
    }

    private void downloadApk() {
        // 启动新线程下载软件
        downloadApkThread = new downloadApkThread();
        downloadApkThread.start();
    }

    /**
     * 下载文件线程
     *
     * @author coolszy
     *@date 2012-4-26
     *@blog http://blog.92coding.com
     */
    public class downloadApkThread extends Thread {
        @Override
        public void run() {
            try {
                // 判断SD卡是否存在，并且是否具有读写权限
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    // 获得存储卡的路径
                    String sdpath = Environment.getExternalStorageDirectory() + "/";
                    mSavePath = sdpath + "Android/data/org.mj.leapremote";
                    URL url = new URL(Define.server +"leapremote.apk");
                    // 创建连接
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    if(!Define.ipv6Support && !Utils.stringIsNull(Define.host))
                        conn.setRequestProperty("Host", Define.host);
                    conn.connect();
                    // 获取文件大小
                    int length = conn.getContentLength();
                    // 创建输入流
                    InputStream is = conn.getInputStream();

                    File file = new File(mSavePath);
                    // 判断文件目录是否存在
                    if (!file.exists()) {
                        (mContext instanceof LogoActivity ?(LogoActivity) mContext: MainActivity.INSTANCE).getExternalFilesDir("");
                    }
                    File apkFile = new File(mSavePath, "LeapRemoteUpdate.apk");
                    FileOutputStream fos = new FileOutputStream(apkFile);
                    int count = 0;
                    // 缓存
                    byte[] buf = new byte[1024];
                    // 写入到文件中
                    do {
                        int numread = is.read(buf);
                        count += numread;
                        // 计算进度条位置
                        progress = (int) (((float) count / length) * 100);
                        // 更新进度
                        mHandler.sendEmptyMessage(DOWNLOAD);
                        if (numread <= 0) {
                            // 下载完成
                            mHandler.sendEmptyMessage(DOWNLOAD_FINISH);
                            break;
                        }
                        // 写入文件
                        fos.write(buf, 0, numread);
                    } while (true);// 点击取消就停止下载.
                    fos.close();
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void installApk() {
        File apkfile = new File(mSavePath, "LeapRemoteUpdate.apk");
        if (!apkfile.exists())
            return;
        installApkFile(apkfile.toString());
    }

    /**
     * 安装APK文件
     */

    public void installApkFile(String filePath) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile((mContext instanceof LogoActivity?(LogoActivity) mContext:MainActivity.INSTANCE), "org.mj.leapremote.fileprovider", new File(filePath));
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(new File(filePath)), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        (mContext instanceof LogoActivity?(LogoActivity) mContext:MainActivity.INSTANCE).startActivity(intent);
    }
}