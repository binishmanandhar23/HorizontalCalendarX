package io.github.binishmanandhar23.horizontalcalendarx.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

object AppUtil {
    fun goToCurrentDate(calendar: ArrayList<Date>): Int {
        val currentDate = Calendar.getInstance().time
        val currentDateFormatted =
            SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(currentDate)
        for (i in calendar.indices) {
            val dateFormatted =
                SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(calendar[i])
            if (currentDateFormatted == dateFormatted)
                return i
        }
        return 0
    }

    inline fun getDateList(crossinline work:(ArrayList<Date>) -> Unit){
        MainScope().launch {
            withContext(Dispatchers.IO){
                val dateList = ArrayList<Date>()

                val calendar = Calendar.getInstance()
                val month = calendar.get(Calendar.MONTH)
                val year = calendar.get(Calendar.YEAR)

                val start: Calendar = Calendar.getInstance()
                start.set(Calendar.MONTH, month) // month is 0 based on calendar

                start.set(Calendar.YEAR, year)
                start.set(Calendar.DAY_OF_MONTH, 1)
                start.time
                start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                if (start.get(Calendar.MONTH) <= month) start.add(Calendar.DATE, -21)


                val end: Calendar = Calendar.getInstance()
                end.set(Calendar.MONTH, month + 1) // next month

                end.set(Calendar.YEAR, year)
                end.set(Calendar.DAY_OF_MONTH, 1)
                end.time
                end.set(Calendar.DATE, -1)
                end.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                if (end.get(Calendar.MONTH) != month + 1) end.add(Calendar.DATE, +21)


                while (start.before(end)) {
                    dateList.add(start.time)
                    start.add(Calendar.DATE, 1);
                }
                if (dateList.size % 7 != 0)
                    dateList.add(start.time)

                work(dateList)
            }
        }
    }
}