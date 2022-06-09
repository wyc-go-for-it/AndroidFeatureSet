package com.wyc.video.recorder

import com.wyc.video.VideoApp
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.video.recorder
 * @ClassName:      AbstractRecorder
 * @Description:    视频录制父类
 * @Author:         wyc
 * @CreateDate:     2022/6/9 13:42
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/6/9 13:42
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

abstract class AbstractRecorder:IRecorder {
    companion object{
        @JvmStatic
        fun getVideoDir():File{
            return File(VideoApp.getVideoDir() + File.separator +"video").apply { if (!exists()){ mkdirs()} }
        }

    }
    protected fun createVideoFile(): File {
        val file = getVideoDir()
        val name = String.format(Locale.CHINA, "%s%s%s.mp4", file.absolutePath, File.separator,
            SimpleDateFormat("yyyyMMddHHmmssSS", Locale.CHINA).format(Date()))
        return File(name)
    }
}