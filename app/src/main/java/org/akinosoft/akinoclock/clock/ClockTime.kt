package org.akinosoft.akinoclock.clock

data class ClockTime(val hour: Int, val minute: Int, val second: Int) {
    init {
        require(hour in 0..23) { "hour must be in 0..23, was $hour" }
        require(minute in 0..59) { "minute must be in 0..59, was $minute" }
        require(second in 0..59) { "second must be in 0..59, was $second" }
    }
}
