package org.mj.leapremote.util;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

public class HttpUtils {
    public static String setParamToUrl(String url, Map<String,String> map){
        if(url==null || map==null)
            return null;
        if(url.endsWith("/"))
            url.substring(0,url.length()-1);
        url = url+(url.contains("?")?(map.size()==0?"":"&"):"?");
        for(String str : map.keySet()){
            try {
                url = url + str + "=" + URLEncoder.encode(map.get(str), "utf-8") +"&";
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                url = url + str + "=" + map.get(str) +"&";
            }
        }
        return url.substring(0,url.length()-1);
    }
    public static void saveCookieStore(CookieStore cookieStore, String savePath) throws IOException {
        FileOutputStream fs = new FileOutputStream(System.getProperty("user.dir")+"/"+savePath);
        ObjectOutputStream os = new ObjectOutputStream(fs);
        os.writeObject(cookieStore);
        os.close();
    }
    //读取Cookie的序列化文件，读取后可以直接使用
    public static CookieStore readCookieStore(String savePath) throws IOException, ClassNotFoundException {
        File file = new File(savePath);
        if((file.exists() && !file.isFile()) || !file.exists())
            return new BasicCookieStore();
        FileInputStream fs = new FileInputStream(System.getProperty("user.dir")+"/"+savePath);//("foo.ser");
        ObjectInputStream ois = new ObjectInputStream(fs);
        CookieStore cookieStore = (CookieStore) ois.readObject();
        ois.close();
        return cookieStore;
    }

    public static long getFileLength(String downloadUrl) {
        if (downloadUrl == null || "".equals(downloadUrl)) {
            return 0L;
        }
        URL url;
        HttpURLConnection conn = null;
        try {
            url = new URL(downloadUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows 7; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.73 Safari/537.36 YNoteCef/5.8.0.1 (Windows)");
            return conn.getContentLength();
        } catch (IOException e) {
            return 0L;
        } finally {
            conn.disconnect();
        }
    }

    /*public static void update(UpdateDialog updateDialog) {
        new Thread(() -> {
            updateDialog.progressBar.setVisible(true);
            FileOutputStream fos = null;
            InputStream inputStream = null;
            try {
                URL url = new URL(Define.domain80 + "BestVpn.exe");
                long length = getFileLength(Define.domain80 + "BestVpn.exe");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                //设置超时间为3秒
                conn.setConnectTimeout(3 * 1000);
                //防止屏蔽程序抓取而返回403错误
                conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");

                //得到输入流
                inputStream = conn.getInputStream();
                //获取自己数组
                File file = new File(System.getProperty("user.dir") + "/" + "BestVpnUpdate.exe");
                fos = new FileOutputStream(file);

                byte[] buffer = new byte[1024];
                int len;
                int offset = 0;
                NumberFormat numberFormat = NumberFormat.getNumberInstance();
                numberFormat.setMaximumFractionDigits(2);
                while ((len = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                    updateDialog.progressBar.setValue((int) Double.parseDouble(numberFormat.format((float) (offset = offset + 1024) / length * 100)));
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fos != null) {
                        fos.close();
                    }
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Utils.update();
        }).start();
    }*/
}
