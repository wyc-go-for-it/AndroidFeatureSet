package com.wyc.androidfeatureset.service;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
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
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG,"ReadAccessibilityService Connected...");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d(TAG,"AccessibilityEvent@eventType:"+ event.getEventType() +"--pkgName:"+ event.getPackageName());
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (null != nodeInfo){
            List<AccessibilityNodeInfo> nodeInfos = nodeInfo.findAccessibilityNodeInfosByText("微信红包");
            if (!nodeInfos.isEmpty()){
                for (AccessibilityNodeInfo info : nodeInfos){
                    if ("微信红包".equals(info.getText().toString())){
                        AccessibilityNodeInfo parent = info.getParent();
                        boolean finished = false;
                        for (int i = 0,counts = parent.getChildCount();i < counts;i ++){
                            AccessibilityNodeInfo child = parent.getChild(i);
                            CharSequence charSequence = child.getText();
                            if (null != charSequence){
                                if (charSequence.toString().contains("领完") || charSequence.toString().contains("领取")){
                                    finished = true;
                                }
                            }
                        }
                        if (!finished){
                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                    }
                }
            }else {
                List<AccessibilityNodeInfo> openList = nodeInfo.findAccessibilityNodeInfosByText("红包");
                Log.d(TAG,"openList:" + openList);
                if (!openList.isEmpty()){
                    for (int i = 0,count = nodeInfo.getChildCount();i < count;i ++){
                        AccessibilityNodeInfo c = nodeInfo.getChild(i);
                        if (c.getClassName().toString().contains("Button")){
                            c.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            break;
                        }
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
