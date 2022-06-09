package com.wyc.video.recorder

import android.view.Surface


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.video.recorder
 * @ClassName:      IRecorder
 * @Description:    视频录制接口
 * @Author:         wyc
 * @CreateDate:     2022/6/9 13:35
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/6/9 13:35
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

interface IRecorder {
    fun configure()
    fun getSurface():Surface
    fun start()
    fun stop()
    fun release()
}