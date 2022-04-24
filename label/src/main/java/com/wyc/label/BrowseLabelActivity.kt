package com.wyc.label

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wyc.label.room.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class BrowseLabelActivity : AppCompatActivity() {
    private var mCurLabel:LabelTemplate? = null
    private var mAdapter:LabelAdapter? = null

    private val mCoroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.com_wyc_label_activity_browse_label)

        initTitle()
        initAdapter()
    }

    private fun initTitle(){
        findViewById<TextView>(R.id.middle_title_tv).setText(R.string.com_wyc_label_local_label)
        findViewById<TextView>(R.id.left_title_tv).setOnClickListener {
            finish()
        }
    }

    private fun initAdapter() {
        val recyclerView: RecyclerView = findViewById(R.id.label_list)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.addItemDecoration(DividerItemDecoration(this,DividerItemDecoration.VERTICAL))
        mAdapter = LabelAdapter(this)

        mCoroutineScope.launch {
            val a = AppDatabase.getInstance().LabelTemplateDao().getAll()
            withContext(Dispatchers.Main){
                mAdapter!!.setDataForList(a)
            }
        }

        mAdapter!!.setSelectListener(object :LabelAdapter.OnSelectFinishListener{
            override fun onFinish(item: LabelTemplate) {
                val intent = Intent()
                intent.putExtra(LABEL_KEY, item)
                setResult(RESULT_OK, intent)
                finish()
            }

        })
        recyclerView.adapter = mAdapter
    }


    override fun onCreateContextMenu(menu: ContextMenu, v: View?, menuInfo: ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menu.add(1, 1, 1, getString(R.string.com_wyc_label_delete))
        menu.add(1, 2, 1, getString(R.string.com_wyc_label_modify))
        mCurLabel = v?.tag as? LabelTemplate
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> {
                mCurLabel?.let {
                    AppDatabase.getInstance().LabelTemplateDao().deleteTemplateById(it)
                    mAdapter?.deleteLabel(it)
                }
            }
            2 -> {
                mCurLabel?.let {
                    LabelDesignActivity.start(this,it)
                    finish()
                }
            }
        }
        return super.onContextItemSelected(item)
    }

    companion object{
        const val LABEL_KEY = "label"
        @JvmStatic
        fun start(context:Context){
            context.startActivity(Intent(context,BrowseLabelActivity::class.java))
        }
    }

    private class LabelAdapter(private val context: BrowseLabelActivity): RecyclerView.Adapter<LabelAdapter.MyViewHolder>(),View.OnClickListener {
        private val mList = mutableListOf<LabelTemplate>()
        private var mListener: OnSelectFinishListener? = null
        class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            val labelView:LabelView =itemView.findViewById(R.id.labelView)
            val name: TextView = itemView.findViewById(R.id.name)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val view = View.inflate(parent.context,R.layout.com_wyc_label_browse_label_adapter,null)
            view.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)
            view.setOnClickListener(this)
            context.registerForContextMenu(view)
            return MyViewHolder(view)
        }
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val label = mList[position]
            holder.name.text = label.templateName
            holder.labelView.postDelayed({
                holder.labelView.updateLabelTemplate(label)
            },100)
            holder.labelView.previewModel()

            holder.itemView.tag = label
        }

        fun deleteLabel(labelTemplate: LabelTemplate){
            mList.apply {
                val index = indexOf(labelTemplate)
                removeAt(index)
                notifyItemRemoved(index)
            }
        }

        override fun onClick(v: View) {
            val obj = v.tag
            if (obj is LabelTemplate) {
                mListener?.apply {
                    onFinish(obj)
                }
            }
        }

        interface OnSelectFinishListener{
            fun onFinish(item: LabelTemplate)
        }

        fun setSelectListener(l: OnSelectFinishListener) {
            mListener = l
        }

        override fun getItemCount(): Int {
            return mList.size
        }

        @SuppressLint("NotifyDataSetChanged")
        fun setDataForList(all: MutableList<LabelTemplate>?) {
            all?.apply {
                mList.clear()
                mList.addAll(this)
                notifyDataSetChanged()
            }
        }
    }
}