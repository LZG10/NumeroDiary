package com.leezg.app.nmerodiary.others

import android.content.Context
import com.leezg.app.nmerodiary.R
import java.util.*

class DateTimeHelper {

    companion object {

        private val SECOND_MILLIS = 1000
        private val MINUTE_MILLIS = 60 * SECOND_MILLIS
        private val HOUR_MILLIS = 60 * MINUTE_MILLIS
        private val DAY_MILLIS = 24 * HOUR_MILLIS

        fun getTimeAgo(mTime: Long, context: Context): String? {
            var time = mTime
            if (time < 1000000000000L)
                time *= 1000

            val now = System.currentTimeMillis()
            if (time > now || time <= 0)
                return null

            val diff = now - time

            return when {
                diff < MINUTE_MILLIS -> context.getString(R.string.just_now)
                diff < 2 * MINUTE_MILLIS -> context.getString(R.string.a_minute_ago)
                diff < 50 * MINUTE_MILLIS -> (diff / MINUTE_MILLIS).toString() + " " + context.getString(
                    R.string.minutes_ago
                )
                diff < 90 * MINUTE_MILLIS -> context.getString(R.string.an_hour_ago)
                diff < 24 * HOUR_MILLIS -> if ((diff / HOUR_MILLIS).toInt() < 2) context.getString(R.string.an_hour_ago)
                else (diff / HOUR_MILLIS).toString() + " " + context.getString(
                    R.string.hours_ago
                )
                diff < 48 * HOUR_MILLIS -> context.getString(R.string.yesterday) + " " + Constant.timeFormat.format(
                    Date(mTime)
                )
                else -> Constant.dateTimeFormat.format(Date(mTime))
            }
        }
    }
}