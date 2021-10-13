package utils

class Stopwatch {

    private var startTime: Long? = null
    private var endTime: Long? = null

    private var pauseTimes: MutableList<Pair<Long, Long?>> = mutableListOf()

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

    fun getTime(): Long? {
        if (startTime == null || endTime == null) return null
        var time = endTime!! - startTime!!
        for (pauseTime in pauseTimes) {
            if (pauseTime.second == null) continue
            time -= pauseTime.second!! - pauseTime.first
        }
        return time
    }

    companion object {

        fun time(task: () -> Unit): Long {
            val stopwatch = Stopwatch()
            stopwatch.start()
            task()
            stopwatch.stop()
            return stopwatch.getTime() ?: return 0 //shouldn't happen
        }

    }

}