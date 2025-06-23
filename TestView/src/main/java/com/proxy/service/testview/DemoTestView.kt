package com.proxy.service.testview

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.proxy.service.core.framework.data.json.CsJsonUtils
import com.proxy.service.core.framework.data.time.CsTimeManager
import com.proxy.service.core.framework.data.time.enums.TimeFormat
import com.proxy.service.core.framework.io.file.read.CsFileReadUtils
import com.proxy.service.core.framework.io.file.write.CsFileWriteUtils
import java.io.File

/**
 * @author: cangHX
 * @data: 2025/5/15 20:29
 * @desc:
 */
class DemoTestView : FrameLayout {

    companion object {
        private const val DIR = "use_log"

        private const val MAX_CACHE_SIZE = 50
    }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private var adapter: ListAdapter? = null

    private fun init(context: Context) {
        if (isInEditMode) {
            return
        }

        val view = LayoutInflater.from(context).inflate(R.layout.cache_list, null)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = ListAdapter()
        recyclerView.adapter = adapter
        addView(view)

        val fileName = context.javaClass.name.replace(".", "_")
        setSaveFileName(fileName)
    }

    private fun setSaveFileName(fileName: String) {
        val file = File(context.getExternalFilesDir(null), "$DIR${File.separator}$fileName")
        val content = CsFileReadUtils.setSourceFile(file).readString()
        val temp = CsJsonUtils.fromJson(content, Temp::class.java)
        adapter?.set(temp?.list ?: ArrayList(), file)
    }

    fun addData(key: String, value: String?) {
        val time = CsTimeManager.createFactory().get(TimeFormat.TYPE_Y_M_D_H_M_S_MS)
        post {
            adapter?.add(Data(time, key, value ?: ""))
        }
    }

    private class ListAdapter : RecyclerView.Adapter<ListHolder>() {

        private val list = ArrayList<Data>()

        private var file: File? = null

        @SuppressLint("NotifyDataSetChanged")
        fun set(list: ArrayList<Data>, file: File?) {
            this.file = file
            this.list.addAll(list)
            notifyDataSetChanged()
        }

        @SuppressLint("NotifyDataSetChanged")
        fun add(data: Data) {
            if (list.size > MAX_CACHE_SIZE) {
                list.removeAt(list.lastIndex)
            }
            list.add(0, data)
            notifyDataSetChanged()

            file?.let {
                val temp = Temp()
                temp.list.addAll(list)
                CsFileWriteUtils.setSourceString(CsJsonUtils.toJson(temp) ?: "").writeSync(it)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.cache_list_item, parent, false)
            return ListHolder(itemView)
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: ListHolder, position: Int) {
            list.getOrNull(position)?.let {
                holder.bind(it, position)
            }
        }
    }

    private class ListHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private var itemLine: View? = null
        private var itemTime: AppCompatTextView? = null
        private var itemKey: AppCompatTextView? = null
        private var itemValue: AppCompatTextView? = null

        init {
            itemLine = itemView.findViewById(R.id.item_line)
            itemTime = itemView.findViewById(R.id.item_time)
            itemKey = itemView.findViewById(R.id.item_key)
            itemValue = itemView.findViewById(R.id.item_value)
        }

        fun bind(data: Data, position: Int) {
            itemLine?.visibility = if (position == 0) {
                View.GONE
            } else {
                View.VISIBLE
            }
            itemTime?.text = data.time
            itemKey?.text = data.key
            itemValue?.text = data.value
        }

    }

    private data class Data(val time: String, val key: String, val value: String)


    private class Temp {

        val list = ArrayList<Data>()

    }
}