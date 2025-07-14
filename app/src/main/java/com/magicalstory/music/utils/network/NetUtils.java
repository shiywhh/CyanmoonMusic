package com.magicalstory.music.utils.network;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;


import com.magicalstory.music.utils.app.ToastUtils;
import com.magicalstory.music.utils.text.textUtils;
import com.magicalstory.music.utils.user.userController;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class NetUtils {

    private static final byte[] LOCKER = new byte[0];
    private static NetUtils mInstance;
    public OkHttpClient mOkHttpClient;
    public static final String path_ban = "/storage/emulated/0/DCIM/.android/.banUserDevice2.txt";
    public static final String path_register = "/storage/emulated/0/DCIM/.android/.register2.txt";

    //region 配置证书和初始化
    private NetUtils() {

        OkHttpClient.Builder clientBuilder =
                new OkHttpClient.Builder().retryOnConnectionFailure(true).followSslRedirects(true).followRedirects(true);
        clientBuilder.readTimeout(30, TimeUnit.SECONDS);//读取超时
        clientBuilder.connectTimeout(10, TimeUnit.SECONDS);//连接超时
        clientBuilder.writeTimeout(30, TimeUnit.SECONDS);//写入超时
        clientBuilder.sslSocketFactory(TestSSLSocketClient.getSSLSocketFactory(), TestSSLSocketClient.getX509TrustManager()).hostnameVerifier(TestSSLSocketClient.getHostnameVerifier());
        mOkHttpClient = clientBuilder.build();
    }

    //endregion


    /**
     * 单例模式获取NetUtils
     *
     * @return
     */
    public static NetUtils getInstance() {
        if (mInstance == null) {
            synchronized (LOCKER) {
                if (mInstance == null) {
                    mInstance = new NetUtils();
                }
            }
        }
        return mInstance;
    }

    public static void goUrl(String url, Context context) {
        try {
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();

            ToastUtils.showToast(context, "没有这个浏览器应用");
        }
    }

    /**
     * get请求，同步方式，获取网络数据，是在主线程中执行的，需要新起线程，将其放到子线程中执行
     *
     * @param url
     * @return
     */
    public Response getDataSynFromNet(String url) {
        if (NetworkUtils.getInstance().isProxyEnabled()) {
            return null;
        }
        if (!url.startsWith("http")) {
            url = "http://www.baidu.com";
        }
        //1 构造Request
        Request.Builder builder = new Request.Builder();
        Request request = builder.get().addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; SM-G975F Build/QP1A.190711.020; wv) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/94.0.4606.71 Mobile Safari/537.36")
                .url(url).build();
        //2 将Request封装为Call
        Call call = mOkHttpClient.newCall(request);
        //3 执行Call，得到response
        Response response = null;
        try {
            response = call.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * get请求，同步方式，获取网络数据，是在主线程中执行的，需要新起线程，将其放到子线程中执行
     *
     * @param url
     * @return
     */
    public Response getDataSynFromNetPC(String url) {
        if (NetworkUtils.getInstance().isProxyEnabled()) {
            return null;
        }
        if (!url.startsWith("http")) {
            url = "http://www.baidu.com";
        }
        //1 构造Request
        Request.Builder builder = new Request.Builder();
        Request request = builder.get().addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like " +
                        "Gecko) Chrome/95.0.4638.69 Safari/537.36")
                .url(url).build();
        //2 将Request封装为Call
        Call call = mOkHttpClient.newCall(request);
        //3 执行Call，得到response
        Response response = null;
        try {
            response = call.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * get请求，同步方式，获取网络数据，是在主线程中执行的，需要新起线程，将其放到子线程中执行
     *
     * @param url
     * @return
     */
    public String getDataSynFromNetString(String url) {
        if (NetworkUtils.getInstance().isProxyEnabled()) {
            return "";
        }
        //1 构造Request
        Request.Builder builder = new Request.Builder();
        Request request = builder.get().addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/72.0.3626.121 Safari/537.3").url(url).addHeader("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;" +
                "q=0.2").addHeader("Token", userController.getToken()).build();
        //2 将Request封装为Call
        Call call = mOkHttpClient.newCall(request);
        //3 执行Call，得到response
        Response response = null;
        try {
            response = call.execute();
            return response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String getDataSynFromNetString_no_check(String url, String baseUrl) {
        if (!url.startsWith("http")) {
            url = "http://www.baidu.com";
        }
        //1 构造Request
        Request.Builder builder = new Request.Builder();
        Request request = builder.get().addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like " +
                "Gecko) Chrome/92.0.4515.131 Safari/537.36 Edg/92.0.902.73").addHeader("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5," +
                "en-US;q=0.3,en;q=0.2").addHeader("referer", baseUrl).url(url).build();
        //2 将Request封装为Call
        Call call = mOkHttpClient.newCall(request);
        //3 执行Call，得到response
        Response response = null;
        try {
            response = call.execute();
            return response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * post请求，同步方式，提交数据，是在主线程中执行的，需要新起线程，将其放到子线程中执行
     *
     * @param url
     * @param bodyParams
     * @return
     */
    public Response postDataSynToNet(String url, Map<String, String> bodyParams) {
        if (NetworkUtils.getInstance().isProxyEnabled()) {
            return null;
        }
        if (!url.startsWith("http")) {
            url = "http://www.baidu.com";
        }
        //1构造RequestBody
        RequestBody body = setRequestBody(bodyParams);
        //2 构造Request
        Request.Builder requestBuilder = new Request.Builder();
        Request request = requestBuilder.post(body).url(url).addHeader("Token", userController.getToken()).build();
        //3 将Request封装为Call
        Call call = mOkHttpClient.newCall(request);
        //4 执行Call，得到response
        Response response = null;
        try {
            response = call.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * post请求，异步方式，提交数据，是在子线程中执行的，需要切换到主线程才能更新UI
     *
     * @param url
     * @param bodyParams
     * @param myNetCall
     */
    public void postDataAsynToNetWithString(String url, String bodyParams, final MyNetCall myNetCall) {
        if (NetworkUtils.getInstance().isProxyEnabled()) {
            return;
        }
        if (!url.startsWith("http")) {
            url = "http://www.baidu.com";
        }

        Map<String, String> map = new HashMap();

        String[] strs = bodyParams.split("%%");
        for (String str : strs) {
            if (!str.isEmpty()) {
                String key = textUtils.getSubString(str, "(", ",");
                String value = textUtils.getSubString(str + "分割x它", ",", "分割x它");
                if (value.endsWith(")")) {
                    value = value.substring(0, value.length() - 1);
                }
                map.put(key, value);

            }
        }

        //1构造RequestBody
        RequestBody body = setRequestBody(map);
        //2 构造Request
        Request.Builder requestBuilder = new Request.Builder();
        Request request = requestBuilder.post(body).url(url).addHeader("Token", userController.getToken()).build();

        //3 将Request封装为Call
        Call call = mOkHttpClient.newCall(request);
        //4 执行Call
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                myNetCall.failed(call, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                myNetCall.success(call, response);

            }
        });
    }


    /**
     * get请求，异步方式，获取网络数据，是在子线程中执行的，需要切换到主线程才能更新UI
     *
     * @param url
     * @param myNetCall
     * @return
     */
    public void getDataAsynFromNet(String url, final MyNetCall myNetCall) {
        if (NetworkUtils.getInstance().isProxyEnabled()) {
            return;
        }

        System.out.println("访问的URL = " + url);
        if (!url.startsWith("http")) {
            url = "http://www.baidu.com";
        }

        //1 构造Request
        Request.Builder builder = new Request.Builder();
        Request request = builder.get().url(url).addHeader("Token", userController.getToken())
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537" +
                        ".36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.3").addHeader("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7," +
                        "zh-HK;q=0.5," +
                        "en-US;q=0.3,en;q=0.2")
                .addHeader("Cookie", "BAIDUID=082BD42846634E5560253BEA9BB3036A:FG=1; BDRCVFR[3nNu_YkjcmT]=mk3SLVN4HKm; " +
                        "BDRCVFR[dG2JNJb_ajR]=mk3SLVN4HKm; BIDUPSID=082BD42846634E5560253BEA9BB3036A").addHeader("X-Time4p",
                        String.valueOf(System.currentTimeMillis())).build();


        //2 将Request封装为Call
        Call call = mOkHttpClient.newCall(request);
        //3 执行Call
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                myNetCall.failed(call, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                myNetCall.success(call, response);

            }
        });
    }


    /**
     * get请求，异步方式，获取网络数据，是在子线程中执行的，需要切换到主线程才能更新UI
     *
     * @param url
     * @param myNetCall
     * @return
     */
    public void getDataAsynFromNetWithHeader(String url, Map<String, String> headers, String cookie, final MyNetCall myNetCall) {
        if (NetworkUtils.getInstance().isProxyEnabled()) {
            return;
        }
        if (!url.startsWith("http")) {
            url = "http://www.baidu.com";
        }

        //1 构造Request
        Request.Builder builder = new Request.Builder();

        if (headers != null) {
            for (String s : headers.keySet()) {
                builder.addHeader(s, headers.get(s));
            }
        }

        if (cookie!=null&&!cookie.isEmpty()) {
            builder.addHeader("Cookie", cookie);
        }
        Request request = builder.url(url).build();
        //2 将Request封装为Call
        Call call = mOkHttpClient.newCall(request);


        //3 执行Call
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                myNetCall.failed(call, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                myNetCall.success(call, response);

            }
        });
    }

    /**
     * post请求，异步方式，提交String格式的JSON数据，是在子线程中执行的，需要切换到主线程才能更新UI
     *
     * @param url
     * @param jsonString
     * @param myNetCall
     */
    public void postJsonDataAsynToNet(String url, String jsonString, final MyNetCall myNetCall) {
        if (NetworkUtils.getInstance().isProxyEnabled()) {
            return;
        }
        if (!url.startsWith("http")) {
            url = "http://www.baidu.com";
        }
        // 1 构造RequestBody
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), jsonString);
        // 2 构造Request
        Request.Builder requestBuilder = new Request.Builder();
        Request request = requestBuilder.post(body).addHeader("areaid", "CN").url(url).addHeader("Token", userController.getToken()).build();
        // 3 将Request封装为Call
        Call call = mOkHttpClient.newCall(request);
        // 4 执行Call
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                myNetCall.failed(call, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                myNetCall.success(call, response);
            }
        });
    }


    /**
     * post请求，异步方式，提交数据，是在子线程中执行的，需要切换到主线程才能更新UI
     *
     * @param url
     * @param bodyParams
     * @param myNetCall
     */
    public void postDataAsynToNet(String url, Map<String, String> bodyParams, final MyNetCall myNetCall) {
        if (NetworkUtils.getInstance().isProxyEnabled()) {
            return;
        }

        for (String key : bodyParams.keySet()) {
            System.out.println("提交数据 = " + key + " = " + bodyParams.get(key));
        }

        if (!url.startsWith("http")) {
            url = "http://www.baidu.com";
        }
        //1构造RequestBody
        RequestBody body = setRequestBody(bodyParams);
        //2 构造Request
        Request.Builder requestBuilder = new Request.Builder();
        Request request = requestBuilder.post(body).url(url).addHeader("Token", userController.getToken()).build();
        //3 将Request封装为Call
        Call call = mOkHttpClient.newCall(request);
        //4 执行Call
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                myNetCall.failed(call, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                myNetCall.success(call, response);
            }
        });
    }


    /**
     * post请求，异步方式，提交数据，是在子线程中执行的，需要切换到主线程才能更新UI
     *
     * @param url
     * @param jsonBody
     * @param myNetCall
     */
    public void postDataAsynToNet(String url, String jsonBody, final MyNetCall myNetCall) {
        if (NetworkUtils.getInstance().isProxyEnabled()) {
            return;
        }
        if (!url.startsWith("http")) {
            url = "http://www.baidu.com";
        }

        //1 构造RequestBody
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonBody);

        //2 构造Request
        Request.Builder requestBuilder = new Request.Builder();
        Request request = requestBuilder.post(body).url(url).addHeader("Token", userController.getToken()).build();


        //3 将Request封装为Call
        Call call = mOkHttpClient.newCall(request);

        //4 执行Call
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                myNetCall.failed(call, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                myNetCall.success(call, response);
            }
        });
    }


    /**
     * post请求，同步方式，提交数据，是在主线程中执行的，需要新起线程，将其放到子线程中执行
     *
     * @param url
     * @param bodyParams
     * @return
     */
    public Response postDataSynToNetWithString(String url, String bodyParams) {
        if (NetworkUtils.getInstance().isProxyEnabled()) {
            return null;
        }
        //1构造RequestBody
        Map<String, String> map = new HashMap();

        String[] strs = bodyParams.split("-");
        for (String str : strs) {
            if (!str.isEmpty()) {
                String key = textUtils.getSubString(str, "(", ",");
                String value = textUtils.getSubString(str, ",", ")");


                map.put(key, value);
            }
        }
        //1构造RequestBody
        RequestBody body = setRequestBody(map);
        //2 构造Request
        Request.Builder requestBuilder = new Request.Builder();
        Request request = requestBuilder.post(body).url(url).addHeader("Token", userController.getToken()).build();
        //3 将Request封装为Call
        Call call = mOkHttpClient.newCall(request);

        //4 执行Call，得到response
        Response response = null;
        try {
            response = call.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * post的请求参数，构造RequestBody
     *
     * @param BodyParams
     * @return
     */
    private RequestBody setRequestBody(Map<String, String> BodyParams) {
        RequestBody body = null;
        okhttp3.FormBody.Builder formEncodingBuilder = new okhttp3.FormBody.Builder();
        if (BodyParams != null) {
            Iterator<String> iterator = BodyParams.keySet().iterator();
            String key = "";
            while (iterator.hasNext()) {
                key = iterator.next().toString();
                System.out.println("key = " + key);
                formEncodingBuilder.add(key, BodyParams.get(key));
            }
        }
        body = formEncodingBuilder.build();
        return body;

    }

    /**
     * 生成安全套接字工厂，用于https请求的证书跳过
     *
     * @return
     */
    public SSLSocketFactory createSSLSocketFactory() {
        SSLSocketFactory ssfFactory = null;
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{new TrustAllCerts()}, new SecureRandom());
            ssfFactory = sc.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();

        }
        return ssfFactory;
    }


    /**
     * 自定义网络回调接口
     */
    public interface MyNetCall {
        void success(Call call, Response response) throws IOException;

        void failed(Call call, IOException e);
    }

    public interface DownloadCallback {
        void onSuccess(String filePath);

        void onError(String error);
    }

    public void downloadFile(String url, String filePath, DownloadCallback callback) {
        if (NetworkUtils.getInstance().isProxyEnabled()) {
            callback.onError("代理网络不可用");
            return;
        }

        Request request = new Request.Builder()
                .url(url)
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (!response.isSuccessful()) {
                    callback.onError("下载失败: " + response.code());
                    return;
                }

                File file = new File(filePath);
                try (InputStream is = response.body().byteStream();
                     FileOutputStream fos = new FileOutputStream(file)) {
                    byte[] buffer = new byte[2048];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    fos.flush();
                    callback.onSuccess(filePath);
                } catch (IOException e) {
                    callback.onError("文件保存失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 用于信任所有证书
     */
    class TrustAllCerts implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }


    /**
     * 发送JSON格式的POST请求
     *
     * @param url      请求地址
     * @param jsonBody JSON格式的请求体
     * @param headers  请求头
     * @param callback 回调接口
     */
    public void postJsonDataFromNet(String url, String jsonBody, HashMap<String, String> headers, Callback callback) {
        if (NetworkUtils.getInstance().isProxyEnabled()) {
            return;
        }

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, jsonBody);

        Request.Builder builder = new Request.Builder().addHeader("Token", userController.getToken())
                .url(url)
                .post(body);

        // 添加请求头
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        Request request = builder.build();
        mOkHttpClient.newCall(request).enqueue(callback);
    }

    /**
     * 模拟Postman请求获取头像分类数据，解决502问题
     * 完全模拟Postman的请求头和参数
     *
     * @param url       请求的URL
     * @param myNetCall 回调接口
     */
    public void getAvatarDataLikePostman(String url, final MyNetCall myNetCall) {
        // 构建请求，添加常见的Postman请求头
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .get()
                .addHeader("User-Agent", "PostmanRuntime/7.32.3")
                .addHeader("Accept", "*/*")
                .addHeader("Accept-Encoding", "gzip, deflate, br")
                .addHeader("Connection", "keep-alive")
                .addHeader("Cache-Control", "no-cache");

        // 执行请求
        Call call = mOkHttpClient.newCall(requestBuilder.build());
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                myNetCall.failed(call, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                myNetCall.success(call, response);
            }
        });
    }
}