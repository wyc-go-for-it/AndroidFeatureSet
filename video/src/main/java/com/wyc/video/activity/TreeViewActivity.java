package com.wyc.video.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

import com.wyc.logger.Logger;
import com.wyc.video.R;
import com.wyc.video.TreeView;

import java.util.Arrays;

public class TreeViewActivity extends BaseActivity {
    private TreeView mTreeView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setMiddleText(getString(R.string.tree_menu));
        initTree();

        findViewById(R.id.button).setOnClickListener(v -> Logger.d(Arrays.toString(mTreeView.getSelectedItem().toArray())));
    }

    private void initTree(){
        mTreeView = findViewById(R.id.treeView);
        mTreeView.setOnItemClickListener(Logger::d);
        final TreeView.Item item = mTreeView.newItem(new TreeView.ItemData(1,"000","wyc",null));
        mTreeView.addChildItem(item,new TreeView.ItemData(2,"000","wyc1",null));
        mTreeView.newItem(new TreeView.ItemData(20,"000","wyc20",null));
        //mTreeView.initDefaultData();
    }

    @Override
    public int getContentLayoutId() {
        return R.layout.activity_tree_view;
    }

    public static void start(Context context){
        context.startActivity(new Intent(context, TreeViewActivity.class));
    }
}