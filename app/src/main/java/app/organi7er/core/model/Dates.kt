package app.organi7er.core.model

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Alle deutschen Datums- und Zeittexte an einer Stelle. */
object Dates {
    val locale: Locale = Locale.GERMANY
    private val time = DateTimeFormatter.ofPattern("HH:mm", locale)
    private val longDay = DateTimeFormatter.ofPattern("EEEE, d. MMMM", locale)
    private val dayMonth = DateTimeFormatter.ofPattern("d. MMMM", locale)
    private val numeric = DateTimeFormatter.ofPattern("dd.MM.", locale)
    private val monthYear = DateTimeFormatter.ofPattern("LLLL yyyy", locale)
    private val shortDate = DateTimeFormatter.ofPattern("EEE, d. MMM", locale)

    fun time(t: LocalDateTime): String = t.format(time)
    fun longDay(d: LocalDate): String = d.format(longDay)
    fun dayMonth(d: LocalDate): String = d.format(dayMonth)
    fun numeric(d: LocalDate): String = d.format(numeric)
    fun monthYear(d: LocalDate): String = d.format(monthYear)
    fun shortDate(d: LocalDate): String = d.format(shortDate)

    /** „Mo“, „Di“ … ohne Punkt. */
    fun weekdayShort(d: LocalDate): String = d.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).removeSuffix(".")
    fun weekdayLong(d: LocalDate): String = d.dayOfWeek.getDisplayName(TextStyle.FULL, locale)

    /** „23.–29. November“ bzw. „30. November – 6. Dezember“. */
    fun range(from: LocalDate, to: LocalDate): String = if (from.month == to.month) {
        "${from.dayOfMonth}.–${to.format(dayMonth)}"
    } else {
        "${from.format(dayMonth)} – ${to.format(dayMonth)}"
    }

    /** Heute · Morgen · Gestern · sonst „Mi, 25. Nov.“ */
    fun relativeDay(d: LocalDate, today: LocalDate): String = when (ChronoUnit.DAYS.between(today, d)) {
        0L -> "Heute"
        1L -> "Morgen"
        -1L -> "Gestern"
        in 2L..6L -> weekdayLong(d)
        else -> shortDate(d)
    }

    /** Kurz für Listen: „heute“, „morgen“, „Do“, „12.12.“ */
    fun dueLabel(d: LocalDate, today: LocalDate): String = when (val days = ChronoUnit.DAYS.between(today, d)) {
        0L -> "heute"
        1L -> "morgen"
        -1L -> "gestern"
        in 2L..6L -> weekdayShort(d)
        else -> if (days < 0) "seit ${numeric(d)}" else numeric(d)
    }

    /** „vor 3 Tagen“, „gestern“, „vor 1 Woche“ … */
    fun ago(instant: Instant, now: LocalDateTime): String {
        val then = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val days = ChronoUnit.DAYS.between(then.toLocalDate(), now.toLocalDate())
        return when {
            days <= 0L -> "heute, ${time(then)}"
            days == 1L -> "gestern"
            days < 7L -> "vor $days Tagen"
            days < 14L -> "vor 1 Woche"
            days < 31L -> "vor ${days / 7} Wochen"
            else -> "vor ${days / 30} Monaten"
        }
    }

    /** „45 Min“, „1 Std“, „1,5 Std“, „ganztägig“ */
    fun duration(start: LocalDateTime, end: LocalDateTime?): String {
        val minutes = Duration.between(start, end ?: start.plusHours(1)).toMinutes()
        return when {
            minutes < 60 -> "$minutes Min"
            minutes % 60 == 0L -> "${minutes / 60} Std"
            minutes % 30 == 0L -> "${minutes / 60},5 Std"
            else -> "${minutes / 60} Std ${minutes % 60} Min"
        }
    }

    /** „in 25 Min“, „in 2 Std“, „jetzt“ */
    fun until(start: LocalDateTime, now: LocalDateTime): String {
        val minutes = ChronoUnit.MINUTES.between(now, start)
        return when {
            minutes <= 0 -> "jetzt"
            minutes < 60 -> "in $minutes Min"
            minutes < 120 -> "in 1 Std"
            else -> "in ${minutes / 60} Std"
        }
    }

    fun weekStart(day: LocalDate, firstDay: Int): LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(firstDay)))

    fun LocalDateTime.toInstantLocal(): Instant = atZone(ZoneId.systemDefault()).toInstant()

    fun Instant.toLocal(): LocalDateTime = LocalDateTime.ofInstant(this, ZoneId.systemDefault())
}
