package com.wyc.video.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.wyc.video.FFmpegPlay.ffmpegApi.FFMediaPlayer;
import com.wyc.video.R;
import com.wyc.video.ScrollSelectionView;
import com.wyc.video.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class VideoRelatedActivity extends VideoBaseActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setMiddleText(getString(R.string.video_related));
        findViewById(R.id.surfaceView).setOnClickListener(this);

        test();
        showFFmpegInfo();
        showFFmpegCodec();
        showFFmpegMuxer();
    }

    private void showFFmpegInfo(){
        final TextView tv = findViewById(R.id.ffmpeg_version);
        tv.setText(FFMediaPlayer.getFFmpegVersion());
    }
    private void showFFmpegCodec(){
        final TextView tv = findViewById(R.id.ffmpeg_codec);
        tv.setText(FFMediaPlayer.getFFmpegAllCodecName());
    }
    private void showFFmpegMuxer(){
        final TextView tv = findViewById(R.id.ffmpeg_muxer);
        final String muxers = String.format(Locale.CHINA,"%s\n%s",FFMediaPlayer.getFFmpegMuxerName(),FFMediaPlayer.getFFmpegDemuxerName());
        tv.setText(muxers);
    }

    private void test(){
        ScrollSelectionView view = findViewById(R.id.scrollSelectionView);
        List<ScrollSelectionView.ScrollItem> list = new ArrayList<>();
        list.add(new ScrollSelectionView.ScrollItem(1,"照相",false));
        list.add(new ScrollSelectionView.ScrollItem(2,"视频",false));
        list.add(new ScrollSelectionView.ScrollItem(3,"短视频",false));
        list.add(new ScrollSelectionView.ScrollItem(4,"短视频4",false));
        list.add(new ScrollSelectionView.ScrollItem(5,"短视频5",false));
        list.add(new ScrollSelectionView.ScrollItem(6,"短视频6",false));
        list.add(new ScrollSelectionView.ScrollItem(7,"短视频7",false));
        list.add(new ScrollSelectionView.ScrollItem(8,"短视频8",false));
        list.add(new ScrollSelectionView.ScrollItem(9,"短视频9",false));
        list.add(new ScrollSelectionView.ScrollItem(10,"短视频10",false));
        list.add(new ScrollSelectionView.ScrollItem(11,"短视频11",false));
        list.add(new ScrollSelectionView.ScrollItem(12,"短视频12",false));

        list.add(new ScrollSelectionView.ScrollItem(13,"短视频13",false));
        list.add(new ScrollSelectionView.ScrollItem(14,"短视频14",false));

        list.add(new ScrollSelectionView.ScrollItem(15,"短视频15",false));
        list.add(new ScrollSelectionView.ScrollItem(16,"短视频16",false));

        list.add(new ScrollSelectionView.ScrollItem(17,"短视频17",false));
        list.add(new ScrollSelectionView.ScrollItem(18,"短视频18",false));

        list.add(new ScrollSelectionView.ScrollItem(19,"短视频19",false));
        list.add(new ScrollSelectionView.ScrollItem(20,"短视频20",false));

        view.addAll(list);
        view.setListener(item -> Utils.logInfo(item.toString()));
    }

    @Override
    public int getContentLayoutId() {
        return R.layout.video_activity_main;
    }

    public static void start(Context context){
        context.startActivity(new Intent(context, VideoRelatedActivity.class));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.surfaceView) {
            CameraSurfaceViewActivity.start(this);
        }
    }
}