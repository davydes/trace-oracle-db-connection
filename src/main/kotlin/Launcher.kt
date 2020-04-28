import oracle.jdbc.OracleDriver
import java.lang.RuntimeException
import java.sql.DriverManager
import java.sql.DriverManager.getConnection
import java.sql.DriverManager.registerDriver
import java.util.logging.LogManager
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

fun main() {
    val threshold = 1000L
    val connectionString = "jdbc:oracle:thin:iBSPB_Priv/Afpjbydthnjh_2016@10.74.95.100:1521/ibsodrb.bankspb.ru"

    configureLogging()
    configureJDBCDriver()

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

fun configureJDBCDriver() {
    registerDriver(OracleDriver())
    DriverManager.setLoginTimeout(5)
}

fun configureLogging() {
    //System.setProperty("oracle.jdbc.Trace", "true")
    val logManager = LogManager.getLogManager()
    (object {}).javaClass.getResourceAsStream("/logging.properties").use {
        logManager.readConfiguration(it)
    }
}