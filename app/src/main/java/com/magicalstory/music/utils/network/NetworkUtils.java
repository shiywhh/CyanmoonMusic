package com.magicalstory.music.utils.network;

/**
 * @Classname: NetworkUtils
 * @Auther: Created by 奇谈君 on 2023/5/21.
 * @Description:抓包检测
 */

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Proxy;

public class NetworkUtils {
    private static NetworkUtils instance;
    private Context context;

    private NetworkUtils(Context context) {
        this.context = context.getApplicationContext();
    }

    public static void initialize(Context context) {
        if (instance == null) {
            instance = new NetworkUtils(context);
        }
    }

    public static NetworkUtils getInstance() {
        if (instance == null) {
            throw new IllegalStateException("NetworkUtils is not initialized. Call initialize() first.");
        }
        return instance;
    }

    // 检测当前设备是否使用了代理
    public boolean isProxyEnabled() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_VPN);
        boolean isVpnConn = networkInfo != null && networkInfo.isConnected();
        return false;
    }

    // 获取当前设备设置的代理主机名
    public String getProxyHost() {
        return Proxy.getHost(context);
    }

    // 获取当前设备设置的代理端口
    public int getProxyPort() {
        return Proxy.getPort(context);
    }

}
