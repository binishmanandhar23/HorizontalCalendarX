package io.github.binishmanandhar23.horizontalcalendarx.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import io.github.binishmanandhar23.horizontalcalendarx.R
import io.github.binishmanandhar23.horizontalcalendarx.adapter.HorizontalCalendarAdapter
import io.github.binishmanandhar23.horizontalcalendarx.databinding.CustomCalendarBinding
import io.github.binishmanandhar23.horizontalcalendarx.utils.AppUtil
import io.github.binishmanandhar23.horizontalcalendarx.utils.SnapToBlock
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.lang.IndexOutOfBoundsException
import java.util.*
import kotlin.collections.ArrayList

class HorizontalCalendarView : FrameLayout {
    val calendarDates: MutableLiveData<ArrayList<Date>> = MutableLiveData(ArrayList())
    var selectedDate: Date? = null
    var goToTodayButtonEnabled = true

    constructor(context: Context) : super(context) {
        init()
    }


    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        init(attributeSet)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs)
    }

    @SuppressLint("InflateParams")
    private fun init(attrs: AttributeSet? = null) {
        val binding = CustomCalendarBinding.bind(
            LayoutInflater.from(context).inflate(R.layout.custom_calendar, null)
        )
        addView(binding.root)
        extractAttributes(attrs) { textSizeHeading, backgroundReference, goToTodayButtonEnabled ->
            arrayOfTextViewTextSize(
                arrayOf(
                    binding.textSunday,
                    binding.textMonday,
                    binding.textTuesday,
                    binding.textWednesday,
                    binding.textThursday,
                    binding.textFriday,
                    binding.textSaturday
                ), textSizeHeading
            )

            //Background reference//
            setBackgroundResource(backgroundReference)
            binding.customCalendarMainContainer.setBackgroundResource(backgroundReference)
            /**********************/
        }
        setUpDates(binding)
    }

    private inline fun extractAttributes(attrs: AttributeSet?, work: (textSize: Int, backgroundReference: Int, goToTodayButtonEnabled: Boolean) -> Unit) {
        var textSizeHeading = resources.getDimensionPixelSize(R.dimen.defaultTextSizeHeading)
        var background = R.color.colorDustyTeal
        if (attrs != null) {
            val attribute =
                context.obtainStyledAttributes(attrs, R.styleable.HorizontalCalendarView)
            textSizeHeading = attribute.getDimensionPixelSize(
                R.styleable.HorizontalCalendarView_textSizeHeading,
                textSizeHeading
            )
            background = attribute.getResourceId(R.styleable.HorizontalCalendarView_wholeBackground, R.color.colorDustyTeal)
            goToTodayButtonEnabled = attribute.getBoolean(R.styleable.HorizontalCalendarView_enableGoToTodayButton, true)
            attribute.recycle()
        }
        work(textSizeHeading, background, goToTodayButtonEnabled)
    }

    private fun arrayOfTextViewTextSize(textViews: Array<TextView>, textSize: Int) {
        textViews.forEach { textView ->
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
        }
    }

    private fun setUpDates(binding: CustomCalendarBinding, maxFling: Int = 1) {
        var calculated = false
        AppUtil.getDateList {
            calendarDates.postValue(it)
        }
        val snapToBlock = SnapToBlock(maxFling)
        val listener = object: HorizontalCalendarAdapter.HorizontalCalendarAdapterCallback{
            override fun onItemPressed(adapter: HorizontalCalendarAdapter, position: Int) {
                val oldPosition = adapter.selectedDatePosition
                adapter.selectedDatePosition = position
                adapter.notifyItemChanged(oldPosition)
                adapter.notifyItemChanged(adapter.selectedDatePosition)
                try {
                    selectedDate = calendarDates.value!![position] //For keeping track of the selected date
                } catch (e: IndexOutOfBoundsException){
                    e.printStackTrace()
                }
                selectCalendarDay(binding,position, AppUtil.goToCurrentDate(calendarDates.value?: ArrayList()))
                goToTodayButtonFunctionality(
                    binding,
                    adapter.selectedDatePosition,
                    AppUtil.goToCurrentDate(calendarDates.value?: ArrayList())
                )
            }

            override fun onWidthCalculated(cellWidth: ArrayList<Int>) {
                if (!calculated) {
                    //Calculating average for [Go To Today] scrolling consistency
                    var result = 0
                    for (width in cellWidth) {
                        result += width
                    }
                    snapToBlock.itemDimensionAverage(result / cellWidth.size)
                    //--------------------------------------------------------//

                    selectCalendarDay(
                        binding,
                        AppUtil.goToCurrentDate(calendarDates.value?: ArrayList()),
                        AppUtil.goToCurrentDate(calendarDates.value?: ArrayList())
                    )
                    calculated = true
                }
            }

        }
        val calendarAdapter = HorizontalCalendarAdapter(context, calendarDates.value?: ArrayList(), listener)
        getCellWidthFromTextView(binding, calendarAdapter)
        binding.calendarRecyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        binding.calendarRecyclerView.setHasFixedSize(true)
        binding.calendarRecyclerView.setItemViewCacheSize(21)
        (binding.calendarRecyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations =
            false
        binding.calendarRecyclerView.adapter = calendarAdapter
        snapToBlock.attachToRecyclerView(binding.calendarRecyclerView)
        snapToBlock.setSnapBlockCallback(object : SnapToBlock.SnapBlockCallback {
            override fun onBlockSnap(snapPosition: Int) {
                val compensateDate = calendarAdapter.selectedDatePosition % 7
                listener.onItemPressed(
                    calendarAdapter,
                    snapPosition + compensateDate
                )
            }

            override fun onBlockSnapped(snapPosition: Int) {
                val compensateDate = calendarAdapter.selectedDatePosition % 7
                listener.onItemPressed(
                    calendarAdapter,
                    snapPosition + compensateDate
                )
            }
        })

        binding.goToTodayButton.setOnClickListener {
            listener.onItemPressed(calendarAdapter, AppUtil.goToCurrentDate(calendarDates.value?: ArrayList()))
        }

        calendarDates.observeForever {
            calendarAdapter.changeCalendarData(it)
            listener.onItemPressed(calendarAdapter, AppUtil.goToCurrentDate(calendarDates.value?: ArrayList()))
        }
    }

    private fun selectCalendarDay(binding: CustomCalendarBinding,
        position: Int, currentDayPosition: Int
    ) {
        if (position >= 0)
            binding.calendarRecyclerView.smoothScrollToPosition(
                position
            )
        for (i in 0..6) {
            when (i) {
                0 -> changeWeekDayView(binding.textMonday, i, position, currentDayPosition)
                1 -> changeWeekDayView(binding.textTuesday, i, position, currentDayPosition)
                2 -> changeWeekDayView(binding.textWednesday, i, position, currentDayPosition)
                3 -> changeWeekDayView(binding.textThursday, i, position, currentDayPosition)
                4 -> changeWeekDayView(binding.textFriday, i, position, currentDayPosition)
                5 -> changeWeekDayView(binding.textSaturday, i, position, currentDayPosition)
                6 -> changeWeekDayView(binding.textSunday, i, position, currentDayPosition)
            }
        }
    }
    private fun changeWeekDayView(
        view: TextView,
        i: Int,
        position: Int, currentDayPosition: Int
    ) {
        view.background =
            if (i == position % 7)
                ResourcesCompat.getDrawable(
                    context.resources,
                    R.drawable.horizontal_calendar_top_selected_background,
                    context.theme
                ) else ResourcesCompat.getDrawable(
                context.resources,
                R.color.color_transparent, context.theme
            )
        view.setTextColor(
            when (i) {
                isCurrentDateToday(position, currentDayPosition) -> ContextCompat.getColor(context,
                    R.color.black
                )
                position % 7 -> ContextCompat.getColor(context,
                    R.color.colorDarkTeal
                )
                else -> ContextCompat.getColor(context,
                    R.color.white
                )
            }
        )
    }

    private fun isCurrentDateToday(
        position: Int, currentDayPosition: Int
    ): Int? {
        val diff = currentDayPosition - position
        if (diff < 7 || diff > -7) {
            return (position % 7) + diff
        }
        return null
    }

    private fun goToTodayButtonFunctionality(binding: CustomCalendarBinding,position: Int, currentDatePosition: Int) {
        binding.goToTodayButton.visibility =
            if (position != currentDatePosition && goToTodayButtonEnabled)
                View.VISIBLE
            else if(!goToTodayButtonEnabled)
                View.GONE
            else
                View.INVISIBLE
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.root)
        constraintSet.setHorizontalBias(
            R.id.goToTodayButton,
            if (position < currentDatePosition) 0.08f else 0.92f
        )
        constraintSet.applyTo(binding.root)
    }

    private fun getCellWidthFromTextView(binding: CustomCalendarBinding, calendarAdapter: HorizontalCalendarAdapter) {
        getViewWidth(arrayOf(
            binding.textSunday,
            binding.textMonday,
            binding.textTuesday,
            binding.textWednesday,
            binding.textThursday,
            binding.textFriday,
            binding.textSaturday
        ), calendarAdapter)
    }

    private var widthList = ArrayList<Int>()
    private fun getViewWidth(views: Array<View>, calendarAdapter: HorizontalCalendarAdapter) {
        views.forEach { view ->
            view.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    widthList.add(view.width)
                    if (widthList.size == 7)
                        calendarAdapter.setCellWidths(widthList)
                }
            })
        }
    }
}