package utils

/**
 * stops time (really)
 */
class Stopwatch {

    private var startTime: Long? = null
    private var endTime: Long? = null

    private var pauseTimes: MutableList<Pair<Long, Long?>> = mutableListOf()

    /**
     * the time in ms the stopwatch has been running for. null if the Stopwatch is not in a clean state. (for example
     * the stopwatch has not been started and stopped yet or the stopwatch is paused and has not been unpaused)
     */
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

    /**
     * starts the stopwatch
     */
    fun start() {
        this.startTime = System.currentTimeMillis()
    }

    /**
     * stops the stopwatch
     */
    fun stop() {
        this.endTime = System.currentTimeMillis()
    }

    /**
     * pauses the stopwatch. While it is paused the time does not increase
     */
    fun pause() {
        if (pauseTimes.isNotEmpty() && pauseTimes[pauseTimes.size - 1].second == null) return
        pauseTimes.add(Pair(System.currentTimeMillis(), null))
    }

    /**
     * unpauses the stopwatch
     */
    fun unpause() {
        if (pauseTimes.isEmpty() || pauseTimes[pauseTimes.size - 1].second != null) return
        pauseTimes[pauseTimes.size - 1] = Pair(pauseTimes[pauseTimes.size - 1].first, System.currentTimeMillis())
    }

    companion object {

        /**
         * times how long a task took to execute
         * @param task the task
         * @return the time in ms it took to execute
         */
        fun time(task: () -> Unit): Long {
            val stopwatch = Stopwatch()
            stopwatch.start()
            task()
            stopwatch.stop()
            return stopwatch.time ?: return 0 //shouldn't happen
        }

    }

}