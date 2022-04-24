package com.wyc.label

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.view.*
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.label
 * @ClassName:      SelectDialog
 * @Description:    搜索、展示、选择对话框
 * @Author:         wyc
 * @CreateDate:     2022/4/24 9:54
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/4/24 9:54
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class SelectDialog(context: Context,private val hasSearch: Boolean = false):Dialog(context,R.style.com_wyc_label_MyDialog) {
    private val mAdapter = Adapter(this)
    private var mMsg = ""
    private var mListener:OnSelect? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.com_wyc_label_select_dialog)
        initItemList()
    }

    override fun show() {
        super.show()
        if (hasSearch)startSearch()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        initWindowSize()
    }
    private fun initWindowSize(){
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val d: Display = wm.defaultDisplay // 获取屏幕宽、高用
        val point = Point()
        d.getSize(point)

        window?.apply {
            setWindowAnimations(R.style.com_wyc_label_bottom_pop_anim)
            val wlp: WindowManager.LayoutParams = attributes
            wlp.gravity = Gravity.BOTTOM
            wlp.y = 68
            wlp.width = (point.x * 0.95).toInt()
            attributes = wlp
        }
    }

    private fun initItemList(){
        val list = findViewById<RecyclerView>(R.id.item_list)
        list.addItemDecoration(DividerItemDecoration(context,DividerItemDecoration.VERTICAL))
        list.layoutManager = LinearLayoutManager(context,LinearLayoutManager.VERTICAL,false)
        list.adapter = mAdapter
    }

    fun addContent(id:String,name:String){
        if (!mAdapter.hasItem(id))
            mAdapter.addItem(Item(id,name))
    }

    fun addContent(vararg arg:Item){
        arg.forEach {
            if (!mAdapter.hasItem(it.id))
                mAdapter.addItem(it)
        }
    }

    fun clearContent(){
        mAdapter.clearItem()
    }
    private fun startSearch(){
        findViewById<ConstraintLayout>(R.id.head)?.apply {
            if (visibility == View.GONE){
                visibility = View.VISIBLE
            }
            findViewById<TextView>(R.id.msg).text = mMsg
        }
    }
    fun stopSearch(){
        findViewById<ConstraintLayout>(R.id.head)?.visibility = View.GONE
    }

    fun showMsg(msg:String){
        mMsg = msg
    }
    interface OnSelect{
        fun select(content:Item)
    }
    fun setSelectListener(listener:OnSelect){
        mListener = listener
    }

    class Item(var id: String = "", var name:String = ""){
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Item

            if (id != other.id) return false
            if (name != other.name) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + name.hashCode()
            return result
        }
    }

    private class Adapter(private val dialog: SelectDialog): RecyclerView.Adapter<Adapter.ViewHolder>(),View.OnClickListener {
        private val mList:MutableList<Item> = mutableListOf()
        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val name:TextView = itemView as TextView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemView = TextView(parent.context)
            itemView.gravity = Gravity.CENTER
            itemView.textSize = 20f
            itemView.setTextColor(parent.context.getColor(R.color.com_wyc_label_text_color))
            itemView.background = parent.context.getDrawable(R.drawable.com_wyc_label_bottom_separator)
            itemView.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                parent.context.resources.getDimensionPixelOffset(R.dimen.com_wyc_label_height_45)
            )
            itemView.setOnClickListener(this)
            return ViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = mList[position];
            holder.name.tag = item
            holder.name.text = item.name
        }

        override fun getItemCount(): Int {
            return mList.size
        }

        fun addItem(i:Item){
            mList.add(i)
            notifyItemChanged(mList.size - 1)
        }

        @SuppressLint("NotifyDataSetChanged")
        fun setItems(items:MutableList<Item>){
            mList.addAll(items)
            notifyDataSetChanged()
        }

        fun clearItem(){
            mList.clear()
        }

        fun  hasItem(id: String):Boolean{
            return mList.find { id == it.id } != null
        }

        override fun onClick(v: View) {
            (v.tag as? Item)?.apply {
                mList.find { this == it }?.let {
                    dialog.mListener?.select(it)
                }
            }
        }

    }
}