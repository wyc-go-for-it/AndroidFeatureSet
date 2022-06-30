package com.wyc.video.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.wyc.video.R;
import com.wyc.video.ScrollSelectionView;

import java.util.ArrayList;
import java.util.List;


public class VideoRelatedActivity extends VideoBaseActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setMiddleText(getString(R.string.video_related));
        findViewById(R.id.surfaceView).setOnClickListener(this);

        test();
    }

    private void test(){
        ScrollSelectionView view = findViewById(R.id.scrollSelectionView);
        List<ScrollSelectionView.ScrollItem> list = new ArrayList<>();
        list.add(new ScrollSelectionView.ScrollItem(1,"照相",false));
        list.add(new ScrollSelectionView.ScrollItem(2,"视频",false));
        list.add(new ScrollSelectionView.ScrollItem(3,"短视频",true));
        list.add(new ScrollSelectionView.ScrollItem(4,"短视频4",false));
        list.add(new ScrollSelectionView.ScrollItem(5,"短视频5",false));
        list.add(new ScrollSelectionView.ScrollItem(6,"短视频6",false));
        view.addAll(list);
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