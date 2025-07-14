package com.magicalstory.music.utils.network;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Dns;

public class CustomDns implements Dns {

    private static final String DNS_SERVER_IP = "223.5.5.5";
    private static final int DNS_SERVER_PORT = 53;

    @Override
    public List<InetAddress> lookup(String hostname) {
        List<InetAddress> addresses = new ArrayList<>();

        // 判断传入的主机名是否是一个 IP 地址
        if (isIpAddress(hostname)) {
            try {
                InetAddress address = InetAddress.getByName(hostname);
                addresses.add(address);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        } else {
            try {

                // 设置使用特定的 DNS 服务器进行解析
                System.setProperty("sun.net.spi.nameservice.nameservers", DNS_SERVER_IP);
                System.setProperty("sun.net.spi.nameservice.provider.1", "dns,sun");

                // 进行 DNS 解析
                InetAddress[] inetAddresses = InetAddress.getAllByName(hostname);
                // Convert the array to a list (compatible with Java versions before 9)
                addresses.addAll(Arrays.asList(inetAddresses));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            return addresses;
        }

        return addresses;
    }

    // 判断是否是 IP 地址的辅助方法
    private boolean isIpAddress(String input) {
        try {
            String[] parts = input.split("\\.");
            if (parts.length != 4) {
                return false;
            }
            for (String part : parts) {
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
