package com.wyc.androidfeatureset.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.wyc.androidfeatureset.R;

import java.security.InvalidParameterException;
import java.util.LinkedList;
import java.util.List;

public class LayoutRecycleViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout_recycle_view);
        init();
    }

    private void init(){
        final RecyclerView list = findViewById(R.id.list);

        final MyLayout myLayout = new MyLayout();
        myLayout.setColumnInfo(4,1);
        list.setLayoutManager(myLayout);

        final Adapter adapter = new Adapter();
        list.setAdapter(adapter);

        final List<String> d = new LinkedList<>();

        for (int i = 0;i < 10;i ++){
            d.add("name" + i);
            d.add("语文");
            d.add("89");
            d.add("21");
            d.add("name1");
            d.add("数学");
            d.add("68");
            d.add("61");
            d.add("name1");
            d.add("体育");
            d.add("98");
            d.add("17");
        }

        adapter.addData(d);

    }

    static class Data{
        private String name;
        private String classes;
        private double score;
        private int ranking;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getClasses() {
            return classes;
        }

        public void setClasses(String classes) {
            this.classes = classes;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public int getRanking() {
            return ranking;
        }

        public void setRanking(int ranking) {
            this.ranking = ranking;
        }
    }

    static class Adapter extends RecyclerView.Adapter<Adapter.MyHolder>{

        private final List<String> mData = new LinkedList<>();
        private final GradientDrawable drawable;

        public Adapter(){
            drawable = new GradientDrawable();
            drawable.setStroke(1,Color.CYAN);
        }


        @NonNull
        @Override
        public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final TextView view = new TextView(parent.getContext());
            view.setLayoutParams(new RecyclerView.LayoutParams(128,108));
            view.setId(R.id._close);
            view.setBackground(drawable);
            view.setGravity(Gravity.CENTER);
            return new MyHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MyHolder holder, int position) {
            holder.view.setText(mData.get(position));
        }


        @Override
        public int getItemCount() {
            return mData.size();
        }

        static class MyHolder extends RecyclerView.ViewHolder{
            private final TextView view;
            public MyHolder(@NonNull View itemView) {
                super(itemView);
                view = itemView.findViewById(R.id._close);
            }
        }

        public void addData(List<String> d){
            mData.addAll(d);
            notifyDataSetChanged();
        }

    }

    static class MyLayout extends RecyclerView.LayoutManager {
        private int columns = 1;
        private int mergeCol = 1;
        /**
         * @param cols 一行的列数
         * @param col 合并列
         * */
        public void setColumnInfo(int cols,int col){
            if (col <0 || col >= cols - 1){
                throw new InvalidParameterException("col invalid");
            }
            columns = cols;
            mergeCol = col;
        }

        @Override
        public void onItemsChanged(@NonNull RecyclerView recyclerView) {
            super.onItemsChanged(recyclerView);
        }

        @Override
        public RecyclerView.LayoutParams generateDefaultLayoutParams() {
            return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        @Override
        public boolean canScrollHorizontally() {
            return true;
        }

        @Override
        public boolean canScrollVertically() {
            return true;
        }

        @Override
        public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
            //offsetChildrenHorizontal(dx * -1);
            return dx;
        }

        @Override
        public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
            Log.e("ddd", String.valueOf(state.hasTargetScrollPosition()));
            offsetChildrenVertical(dy * -1);
            return dy;
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            detachAndScrapAttachedViews(recycler);

            if (state.getItemCount() == 0){
                return;
            }

            int offset = 0;
            int topOffset = 0;
            int jH = 0;
            int kH = -1;

            final int whichMergeCol = mergeCol - 1;
            final int merge = 3 * columns + whichMergeCol ;

            for (int i = 0,count = state.getItemCount();i < count;i ++){

                final View v = recycler.getViewForPosition(i);

                if (i != 0 && i % columns == 0){
                    jH ++;
                    offset = 0;
                }

                boolean hasMerge = (i == whichMergeCol || i % merge == 0);
                if (hasMerge){
                    RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) v.getLayoutParams();
                    layoutParams.height = 3 * layoutParams.height;
                    kH += 1;
                }


                measureChildWithMargins(v,0,0);

                final int w = v.getMeasuredWidth();
                final int h = v.getMeasuredHeight();

                if (hasMerge){
                    topOffset = kH * h;
                }else
                    topOffset = jH * h;


                if (hasMerge || i % columns != 0){
                    addView(v);
                    layoutDecoratedWithMargins(v,offset,topOffset,offset + w,topOffset + h);
                }

                offset += w;
            }
        }
    }

    public static void start(Context c){
        c.startActivity(new Intent(c, LayoutRecycleViewActivity.class));
    }
}