package utils

class Stopwatch {

    private var startTime: Long? = null
    private var endTime: Long? = null

    private var pauseTimes: MutableList<Pair<Long, Long?>> = mutableListOf()

    val time: Long?
        get() {
            if (startTime == null || endTime == null) return null
            var time = endTime!! - startTime!!
            for (pauseTime in pauseTimes) {
                if (pauseTime.second == null) continue
                time -= pauseTime.second!! - pauseTime.first
            }
            return time
        }

    fun start() {
        this.startTime = System.currentTimeMillis()
    }

    fun stop() {
        this.endTime = System.currentTimeMillis()
    }

    fun pause() {
        if (pauseTimes.isNotEmpty() && pauseTimes[pauseTimes.size - 1].second == null) return
        pauseTimes.add(Pair(System.currentTimeMillis(), null))
    }

    fun unpause() {
        if (pauseTimes.isEmpty() || pauseTimes[pauseTimes.size - 1].second != null) return
        pauseTimes[pauseTimes.size - 1] = Pair(pauseTimes[pauseTimes.size - 1].first, System.currentTimeMillis())
    }

    companion object {

        fun time(task: () -> Unit): Long {
            val stopwatch = Stopwatch()
            stopwatch.start()
            task()
            stopwatch.stop()
            return stopwatch.time ?: return 0 //shouldn't happen
        }

        fun <T> time(task: () -> T): Pair<T, Long> {
            val stopwatch = Stopwatch()
            stopwatch.start()
            val result = task()
            stopwatch.stop()
            return Pair(result, stopwatch.time ?: 0)
        }

    }

}