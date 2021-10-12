import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.*
import java.util.logging.Formatter

object Conf {

    val logger: Logger = Logger.getLogger("clientLogger")

    init {
        logger.useParentHandlers = false
        val handler = ConsoleHandler()
        handler.formatter = LogFormatter()
        logger.addHandler(handler)
    }

    private class LogFormatter : Formatter() {

        val ansiReset = "\u001B[0m"
        val ansiYellow = "\u001B[33m"
        val ansiRed = "\u001B[31m"
        val ansiBlue = "\u001B[34m"
        val ansiWhite = "\u001B[37m"

        override fun format(record: LogRecord?): String {
            record ?: return ""
            val builder = StringBuilder()
            builder.append( when(record.level) {
                Level.SEVERE -> ansiRed
                Level.WARNING -> ansiYellow
                Level.INFO -> ansiBlue
                else -> ansiWhite
            })
            builder
                .append(" - ")
                .append(record.level.toString())
                .append(" - ")
                .append("[")
                .append(SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(Date(record.millis)))
                .append("] ")
                .append(record.loggerName)
                .append(": ")
                .append(record.message)
                .append(ansiReset)
                .append("\n")
            return builder.toString()
        }

    }

}