import oracle.jdbc.OracleDriver
import java.sql.DriverManager
import java.sql.DriverManager.getConnection
import java.sql.DriverManager.registerDriver
import java.util.logging.LogManager
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("first argument must be a valid jdbc connection string\nexample: \"jdbc:oracle:thin:<username>/<password>@<host>:<port>/<db-name>\"")
        exitProcess(1)
    }

    val threshold = run {
        if (args.size < 2)
            1000L
        else
            try {
                args[1].toLong()
            } catch (e: NumberFormatException) {
                println("second argument is threshold time and must be an integral number")
                exitProcess(1)
            }
    }

    val connectionString = args[0]

    configureLogging()
    configureJDBCDriver(threshold)

    try {
        logTime("test finished", threshold) {
            val connection = logTime("opened connection") {
                getConnection(connectionString)
            }

            try {
                logTime("run select") {
                    connection.createStatement().use { statement ->
                        val rs = statement.executeQuery("SELECT 'OK' as RESULT FROM DUAL")
                        while (rs.next()) {
                            val result = rs.getString("RESULT")
                            println("got: $result")
                        }
                    }
                }
            } finally {
                logTime("connection closed") {
                    connection.close()
                }
            }
        }
    } catch (e: TimeoutThresholdException) {
        exitProcess(124)
    }
}

fun <T> logTime(name: String, threshold: Long? = null, block: () -> T): T {
    var result: T? = null
    var throwable: Throwable? = null
    val millis = measureTimeMillis {
        try {
            result = block()
        } catch (e: Throwable) {
            throwable = e
        }
    }
    println("$name in ${millis}ms [${if (throwable != null) "FAILED" else "PASSED"}]")

    if (throwable != null) throw throwable!!
    if (threshold != null && threshold < millis) throw TimeoutThresholdException()
    return result!!
}

class TimeoutThresholdException : RuntimeException()

fun configureJDBCDriver(timeoutMs: Long) {
    registerDriver(OracleDriver())
    val sessionTimeoutSec = (timeoutMs / 1000).toInt().also {
        println("setLoginTimeout: $it seconds")
    }
    DriverManager.setLoginTimeout(sessionTimeoutSec)
}

fun configureLogging() {
    //System.setProperty("oracle.jdbc.Trace", "true")
    val logManager = LogManager.getLogManager()
    (object {}).javaClass.getResourceAsStream("/logging.properties").use {
        logManager.readConfiguration(it)
    }
}