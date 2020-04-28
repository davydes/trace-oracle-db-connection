import oracle.jdbc.OracleDriver
import java.sql.DriverManager
import java.sql.DriverManager.getConnection
import java.sql.DriverManager.registerDriver
import java.util.logging.LogManager
import kotlin.system.measureTimeMillis

fun main() {
    val connectionString = "jdbc:oracle:thin:iBSPB_Priv/Afpjbydthnjh_2016@10.74.95.100:1521/ibsodrb.bankspb.ru"
    configureLogging()
    configureJDBCDriver()

    logTime("test finished") {
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
}

fun <T> logTime(name: String, block: () -> T): T {
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
    return result!!
}

fun configureJDBCDriver() {
    registerDriver(OracleDriver())
    DriverManager.setLoginTimeout(5)
}

fun configureLogging() {
    System.setProperty("oracle.jdbc.Trace", "true")
    val logManager = LogManager.getLogManager()
    (object {}).javaClass.getResourceAsStream("/logging.properties").use {
        logManager.readConfiguration(it)
    }
}