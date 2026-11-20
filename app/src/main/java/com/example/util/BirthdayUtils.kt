package com.example.util

import java.util.Calendar

object BirthdayUtils {

    private val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    val bengaliMonths = listOf(
        "জানুয়ারি",
        "ফেব্রুয়ারি",
        "মার্চ",
        "এপ্রিল",
        "মে",
        "জুন",
        "জুলাই",
        "আগস্ট",
        "সেপ্টেম্বর",
        "অক্টোবর",
        "নভেম্বর",
        "ডিসেম্বর"
    )

    fun toBengaliDigits(number: Number): String {
        val str = number.toString()
        val sb = StringBuilder(str.length)
        for (ch in str) {
            if (ch in '0'..'9') {
                sb.append(bengaliDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun hasBirthday(day: Long, month: Long): Boolean {
        return day in 1L..31L && month in 1L..12L
    }

    fun formatBirthdayBn(day: Long, month: Long, year: Long): String {
        if (!hasBirthday(day, month)) return "জন্মদিন যোগ করা হয়নি"
        val monthName = bengaliMonths.getOrElse((month - 1).toInt()) { "" }
        val dayStr = toBengaliDigits(day)
        return if (year > 1900L) {
            "$dayStr $monthName, ${toBengaliDigits(year)}"
        } else {
            "$dayStr $monthName"
        }
    }

    fun daysUntilBirthday(day: Long, month: Long, nowMillis: Long = System.currentTimeMillis()): Int? {
        if (!hasBirthday(day, month)) return null
        val today = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val currentYear = today.get(Calendar.YEAR)
        val target = Calendar.getInstance().apply {
            timeInMillis = today.timeInMillis
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, (month - 1).toInt())
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, day.toInt().coerceIn(1, maxDay))
        }

        if (target.timeInMillis < today.timeInMillis) {
            target.set(Calendar.YEAR, currentYear + 1)
            val maxDay = target.getActualMaximum(Calendar.DAY_OF_MONTH)
            target.set(Calendar.DAY_OF_MONTH, day.toInt().coerceIn(1, maxDay))
        }

        val diffMillis = target.timeInMillis - today.timeInMillis
        return ((diffMillis + 43_200_000L) / 86_400_000L).toInt()
    }

    fun formatCountdownBn(daysUntil: Int?): String {
        return when (daysUntil) {
            null -> "তারিখ নেই"
            0 -> "আজ শুভ জন্মদিন!"
            1 -> "আগামীকাল জন্মদিন"
            else -> "আর ${toBengaliDigits(daysUntil)} দিন"
        }
    }

    fun currentAge(day: Long, month: Long, year: Long, nowMillis: Long = System.currentTimeMillis()): Int? {
        if (!hasBirthday(day, month) || year <= 1900L) return null
        val today = Calendar.getInstance().apply { timeInMillis = nowMillis }
        var age = today.get(Calendar.YEAR) - year.toInt()
        val todayMonth = today.get(Calendar.MONTH) + 1
        val todayDay = today.get(Calendar.DAY_OF_MONTH)
        if (todayMonth < month || (todayMonth == month.toInt() && todayDay < day)) {
            age -= 1
        }
        return age.takeIf { it >= 0 }
    }

    fun upcomingAge(day: Long, month: Long, year: Long, nowMillis: Long = System.currentTimeMillis()): Int? {
        val age = currentAge(day, month, year, nowMillis) ?: return null
        val days = daysUntilBirthday(day, month, nowMillis) ?: return null
        return if (days == 0) age else age + 1
    }
}
