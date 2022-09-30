package com.wyc.androidfeatureset.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;

import androidx.annotation.CallSuper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.wyc.androidfeatureset.R;
import com.wyc.androidfeatureset.SM2.SM2;
import com.wyc.androidfeatureset.SM2.SM2Demo;
import com.wyc.androidfeatureset.camera.CaptureActivity;
import com.wyc.androidfeatureset.provider.ProviderActivity;
import com.wyc.label.LabelPrintSettingActivity;
import com.wyc.label.printer.LabelPrintUtils;
import com.wyc.logger.Logger;
import com.wyc.video.activity.TreeViewActivity;
import com.wyc.video.activity.VideoRelatedActivity;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final int BROADCAST_PORT = 1234;
    private volatile boolean start = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        LabelPrintUtils.openPrinter(null);

        serveTest();
        test();

    }

    private void serveTest(){
        new Thread(() -> {

            try(DatagramSocket datagramSocket = new DatagramSocket(BROADCAST_PORT, InetAddress.getByName("0.0.0.0"))){
                datagramSocket.setBroadcast(true);

                byte[] buf = new byte[1024];
                // 接收数据
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                datagramSocket.receive(packet);
                String content = new String(packet.getData()).trim();

                Logger.d("server IP :%s,receive：%s,client IP:%s",getLocalIP(),content,packet.getAddress().getHostAddress());

            }catch (IOException e){
                e.printStackTrace();
            }
        }).start();
    }

    private String getLocalIP(){
        try {
            // 获取本地所有网络接口
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                // getInterfaceAddresses()方法返回绑定到该网络接口的所有 IP 的集合
                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress address = interfaceAddress.getAddress();
                    if (address instanceof Inet4Address)
                    return interfaceAddress.getAddress().getHostAddress();
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return "";
    }

    private void test(){

        try(DatagramSocket datagramSocket = new DatagramSocket()) {
            datagramSocket.setBroadcast(true);
            // 获取本地所有网络接口
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                // getInterfaceAddresses()方法返回绑定到该网络接口的所有 IP 的集合
                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    // 不广播回环网络接口
                    if (broadcast  == null) {
                        continue;
                    }
                    // 发送广播报文
                    try {
                        byte[] data = "你好".getBytes();
                        DatagramPacket sendPacket = new DatagramPacket(data,
                                data.length, broadcast, BROADCAST_PORT);
                        datagramSocket.send(sendPacket);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Logger.d("发送请求:%s", getClass().getName() + ">>> Request packet sent to: " +
                            broadcast.getHostAddress() + "; Interface: " + networkInterface.getDisplayName());
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        checkSelfPermission();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        start = false;
    }

    private void checkSelfPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            if (!Environment.isExternalStorageManager()){
                final Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        }else
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if ((ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                }
            }
    }

    @OnClick(R.id.camera_feature_btn)
    void camera(){
        CaptureActivity.start(this);
    }
    @OnClick(R.id.provider_btn)
    void provider(){
        ProviderActivity.start(this);
    }
    @OnClick(R.id.label_design)
    void label(){
        LabelPrintSettingActivity.start(this);
    }
    @OnClick(R.id.sm2)
    void sing(){
        SM2 clz = SM2.getInstance();
        byte [] sourceData = "8003 300370100000013 2022051010180188620220510 20220510101801 0.01 100370100000697".getBytes();

        /* 密管密钥测试 */
        String x = "95510BADCE29F70BE07DF6E2B0CE75BE124A56C08E82435E72B4AA6C17679F45";
        String y = "5A6892AADDE2A6B7A58CA7B0E10CA78D3811FF27E9F728CD80D53C1B9A6461DB";
        String d = "D3F24D61BB2816882B8474B778DD7C3166D665F9455DC9D551C989C161E76AB0";

        {   // 密管密钥自签自验
            byte[] sign = clz.SM2Sign(SM2Demo.hexStringToBytes(d), sourceData);
            Logger.d("SignData：" + new String(sign));
            boolean verify = clz.SM2Verify(SM2Demo.hexStringToBytes(x), SM2Demo.hexStringToBytes(y), sourceData, sign);
            Logger.d("VerifyResult：" + verify);
        }
    }
    @OnClick(R.id.video)
    void video(){
        VideoRelatedActivity.start(this);
    }

    @OnClick(R.id.tree_menu)
    void tree(){
        TreeViewActivity.start(this);
    }
}