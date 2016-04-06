package com.ghkjgod.lightnovel.util;

import android.content.ContentValues;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import com.ghkjgod.lightnovel.MyApp;
import com.google.gson.internal.$Gson$Types;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

/**
 * Light Network
 * *
 * This class achieve the basic network protocol:
 * HttpPost ...
 **/

public class LightNetwork {

    public static OkHttpClient client = new OkHttpClient();

    private static String getFileName(String path) {
        int separatorIndex = path.lastIndexOf("/");
        return (separatorIndex < 0) ? path : path.substring(separatorIndex + 1, path.length());
    }
    public static boolean okHttpDownload(String urlString, ContentValues values,String destFileDir,String filename,boolean forceUpdate) {
        File file = new File(destFileDir);
        file.mkdirs();
        file = new File(destFileDir + (destFileDir.charAt(destFileDir.length() - 1) != File.separatorChar ? File.separator : "") + filename);
        Log.v("okHttpDownload","Path: " + destFileDir + (destFileDir.charAt(destFileDir.length() - 1) != File.separatorChar ? File.separator : "") + filename);
        if (!file.exists() || forceUpdate) {
            if (file.exists() && !file.isFile()) {
                Log.v("okHttpDownload", "Write failed0");
                return false; // is not a file
            }
            try {
                file.createNewFile(); // create file
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("okHttpDownload","cant_create");
                return false;
            }

            Request request = null;
            if(values!=null) {
                FormEncodingBuilder builder = new FormEncodingBuilder();
                for (String key : values.keySet()) {
                    if (!(values.get(key) instanceof String)) continue;
                    builder.add(key, (String) values.get(key));
                }
                RequestBody formBody = builder.build();
                request = new Request.Builder()
                        .url(urlString)
                        .post(formBody)
                        .addHeader("Accept-Encoding", "gzip")
                        .build();
            }else{

               request = new Request.Builder()
                        .url(urlString)
                        .addHeader("Accept-Encoding", "gzip")
                        .build();
            }
            client.setConnectTimeout(3, TimeUnit.SECONDS);
            client.setReadTimeout(5, TimeUnit.SECONDS);
            //cookie auto manager
            client.setCookieHandler(new CookieManager(
                    new PersistentCookieStore(MyApp.getContext()),
                    CookiePolicy.ACCEPT_ALL));
            Response response = null;
            InputStream is = null;
            FileOutputStream fos = null;
            int len = 0;
            byte[] buf = new byte[2048];
            try {
                response = client
                        .newCall(request)
                        .execute();
                if (response.isSuccessful()) {
                    is = response.body().byteStream();
                    if (response.header("Content-Encoding")!=null&&response.header("Content-Encoding").toLowerCase().contains("gzip")) {
                        // using 'gzip'
                        Log.e("useGzip", "start");
                        is = new GZIPInputStream(new BufferedInputStream(is));
                    }
                    file = new File(destFileDir, filename);
                    fos = new FileOutputStream(file);
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                    }
                    fos.flush();
                }else{

                    return false;
                }
            } catch (IOException e) {
                return false;
            } finally {
                try {
                    if (is != null) is.close();
                } catch (IOException e) {
                }
                try {
                    if (fos != null) fos.close();
                } catch (IOException e) {
                }
            }
            return true;
        }
        return true;
    }


    public static byte[] okHttpPostConnection(String urlString, ContentValues values) {

        FormEncodingBuilder builder = new FormEncodingBuilder();
        for (String key : values.keySet()) {
            if (!(values.get(key) instanceof String)) continue;
            builder.add(key, (String) values.get(key));
        }
        RequestBody formBody = builder.build();
        Request request = new Request.Builder()
                .url(urlString)
                .post(formBody)
                .addHeader("Accept-Encoding", "gzip")
                .build();
        client.setConnectTimeout(3, TimeUnit.SECONDS);
        client.setReadTimeout(5, TimeUnit.SECONDS);
        //cookie auto manager
        client.setCookieHandler(new CookieManager(
                new PersistentCookieStore(MyApp.getContext()),
                CookiePolicy.ACCEPT_ALL));
        Response response = null;
        try {
            response = client
                    .newCall(request)
                    .execute();
            if (response.isSuccessful()) {
                InputStream inStream = response.body().byteStream();
                ByteArrayOutputStream outStream = new ByteArrayOutputStream(); // output stream
                if (response.header("Content-Encoding")!=null&&response.header("Content-Encoding").toLowerCase().contains("gzip")) {
                    // using 'gzip'
                    Log.e("useGzip", "start");
                    inStream = new GZIPInputStream(new BufferedInputStream(inStream));
                }
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inStream.read(buffer)) != -1) {
                    outStream.write(buffer, 0, len); // read to outStream
                }
                byte[] data = outStream.toByteArray(); // copy to ByteArray
                outStream.flush();
                outStream.close();
                inStream.close();
                return data;
            }
        } catch (IOException e) {

            return null;
        }
        return null;

    }

    public static String encodeToHttp(String str) {
        String enc;
        try {
            enc = URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Log.v("MewX-Net", e.getMessage());
            enc = ""; // prevent crash
        }
        return enc;
    }

}
