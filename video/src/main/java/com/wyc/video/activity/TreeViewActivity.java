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


        for (int i = 0;i < 5;i ++){
            final TreeView.Item item = mTreeView.newItem(new TreeView.ItemData(i,"000","列表" + i,null));
            for (int k = 5;k < 10;k++){
                final TreeView.Item item_k = mTreeView.addChildItem(item,new TreeView.ItemData(k * 10,"000","列表" + k,null));
                for (int j = 1;j < k;j ++){
                    mTreeView.addChildItem(item_k,new TreeView.ItemData(j * 100,"000","列表" + j,null));
                }
            }
        }
    }

    @Override
    public int getContentLayoutId() {
        return R.layout.activity_tree_view;
    }

    public static void start(Context context){
        context.startActivity(new Intent(context, TreeViewActivity.class));
    }
}