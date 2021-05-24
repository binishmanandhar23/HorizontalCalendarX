package io.github.binishmanandhar23.horizontalcalendarx.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import io.github.binishmanandhar23.horizontalcalendarx.R
import io.github.binishmanandhar23.horizontalcalendarx.utils.AppUtil
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class HorizontalCalendarAdapter(
    val context: Context,
    private var calendarData: ArrayList<Date>,
    private val listener: HorizontalCalendarAdapterCallback
) : RecyclerView.Adapter<HorizontalCalendarAdapter.ViewHolder>() {
    private var calendarDates = ArrayList<Date>()

    init {
        calendarDates = calendarData
    }
    var selectedDatePosition = AppUtil.goToCurrentDate(calendarDates)
    var cellWidth = ArrayList<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.custom_calendar_item, parent, false)
        )
    }

    override fun getItemCount(): Int = calendarDates.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val date = calendarDates[position]
        holder.textDate.text = SimpleDateFormat("dd", Locale.getDefault()).format(date)
        val params = holder.container.layoutParams
        params.width = if (cellWidth.isNotEmpty()) cellWidth[position % 7] else 0
        Log.i(
            "CustomCalendarWidth",
            "Width: ${if (cellWidth.isNotEmpty()) cellWidth[position % 7] else 0}  Position: $position"
        )
        holder.container.layoutParams = params
        holder.container.setOnClickListener {
            listener.onItemPressed(this, position)
        }

        //Change view style of selected item
        holder.textDate.setTextColor(
            when {
                checkIfCurrentDay(date) -> ContextCompat.getColor(
                    context,
                    R.color.black
                )
                selectedDatePosition == position -> ContextCompat.getColor(context,
                    R.color.colorDarkTeal
                )
                else -> ContextCompat.getColor(
                    context,
                    R.color.white
                )
            }
        )
        holder.container.background =
            if (selectedDatePosition != position) ResourcesCompat.getDrawable(
                context.resources,
                R.color.color_transparent
            ,context.theme) else ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.horizontal_calendar_bottom_selected_background, context.theme
            )
        holder.imageViewDot.imageTintList = ContextCompat.getColorStateList(
            context, if (selectedDatePosition != position && !checkIfCurrentDay(date))
                R.color.white
            else if (selectedDatePosition == position)
                R.color.colorDarkTeal
            else
                R.color.black
        )

        if (cellWidth.isNotEmpty() && position == (calendarDates.size - 1))
            listener.onWidthCalculated(cellWidth)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textDate: TextView = itemView.findViewById(R.id.textViewDate)
        val container: LinearLayout = itemView.findViewById(R.id.customCalendarItemContainer)
        val imageViewDot: ImageView = itemView.findViewById(R.id.imageViewHasMedication)
    }

    private fun checkIfCurrentDay(date: Date): Boolean {
        val currentTime = Calendar.getInstance().time
        val currentDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(currentTime)
        val formattedDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(date)
        if (currentDate.equals(formattedDate, ignoreCase = true))
            return true
        return false
    }

    fun setCellWidths(widthList: ArrayList<Int>) {
        cellWidth = widthList
        notifyDataSetChanged()
    }

    fun changeCalendarData(calendarDates: ArrayList<Date>) {
        this.calendarDates = calendarDates
        notifyDataSetChanged()
    }


    interface HorizontalCalendarAdapterCallback {
        fun onItemPressed(adapter: HorizontalCalendarAdapter, position: Int)
        fun onWidthCalculated(cellWidth: ArrayList<Int>)
    }
}