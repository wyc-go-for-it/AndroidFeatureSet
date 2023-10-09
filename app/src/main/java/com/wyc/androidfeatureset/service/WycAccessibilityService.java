package com.wyc.androidfeatureset.service;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 * @ProjectName: CloudApp
 * @Package: com.wyc.cloudapp.service
 * @ClassName: ReadAccessibilityService
 * @Description: 读界面辅助服务
 * @Author: wyc
 * @CreateDate: 2021-06-04 15:37
 * @UpdateUser: 更新者
 * @UpdateDate: 2021-06-04 15:37
 * @UpdateRemark: 更新说明
 * @Version: 1.0
 */
public class WycAccessibilityService extends AccessibilityService {
    private static final String TAG = "WycAccessibilityService";
    private long time = 0;
    private volatile AccessibilityNodeInfo mTarget ;
    private volatile boolean start = true;
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG,"ReadAccessibilityService Connected...");
        a();
    }

    private void a(){
        new Thread(() -> {
            while (start){
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (mTarget != null){
                    Log.e(TAG,"target:" + mTarget);
                    mTarget.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }

        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        start = false;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d(TAG,"time:" + (System.currentTimeMillis() - time));
        Log.d(TAG,"AccessibilityEvent@eventType:"+ event.getEventType() +"--pkgName:"+ event.getPackageName());
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (null != nodeInfo){
            List<AccessibilityNodeInfo> nodeInfos = nodeInfo.findAccessibilityNodeInfosByText("已预约");
            if (!nodeInfos.isEmpty() && nodeInfos.size() > 1){
                mTarget = nodeInfos.get(1);
                for (AccessibilityNodeInfo info : nodeInfos){
                    //info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    time = System.currentTimeMillis();
                }
            }else {
                List<AccessibilityNodeInfo> openList = nodeInfo.findAccessibilityNodeInfosByText("提交订单");
                Log.d(TAG,"openList:" + openList);
                if (!openList.isEmpty()){
                    for (int i = 0,count = nodeInfo.getChildCount();i < count;i ++){
                        AccessibilityNodeInfo c = nodeInfo.getChild(i);
                        c.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.d(TAG,"getClassName:" + c.getClassName());
                    }
                }

            }
        }
    }

    @Override
    public void onInterrupt() {

    }
}
