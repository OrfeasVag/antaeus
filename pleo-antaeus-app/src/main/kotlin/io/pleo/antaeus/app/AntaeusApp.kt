/*
    Defines the main() entry point of the app.
    Configures the database and sets up the REST web service.
 */

@file:JvmName("AntaeusApp")

package io.pleo.antaeus.app

import getPaymentProvider
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.data.CustomerTable
import io.pleo.antaeus.data.InvoiceTable
import io.pleo.antaeus.rest.AntaeusRest
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import setupInitialData
import java.io.File
import java.sql.Connection
import java.util.*
import kotlin.concurrent.schedule

private val logger = KotlinLogging.logger {}//added logger
fun main() {
    // The tables to create in the database.
    val tables = arrayOf(InvoiceTable, CustomerTable)

    val dbFile: File = File.createTempFile("antaeus-db", ".sqlite")
    // Connect to the database and create the needed tables. Drop any existing data.
    val db = Database
        .connect(
            url = "jdbc:sqlite:${dbFile.absolutePath}",
            driver = "org.sqlite.JDBC",
            user = "root",
            password = ""
        )
        .also {
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            transaction(it) {
                addLogger(StdOutSqlLogger)
                // Drop all existing tables to ensure a clean slate on each run
                SchemaUtils.drop(*tables)
                // Create all tables
                SchemaUtils.create(*tables)
            }
        }

    // Set up data access layer.
    val dal = AntaeusDal(db = db)

    // Insert example data in the database.
    setupInitialData(dal = dal)

    // Get third parties
    val paymentProvider = getPaymentProvider()

    // Create core services
    val invoiceService = InvoiceService(dal = dal)
    val customerService = CustomerService(dal = dal)

    // This is _your_ billing service to be included where you see fit
    val billingService = BillingService(paymentProvider = paymentProvider)

    // Create REST web service
    AntaeusRest(
        invoiceService = invoiceService,
        customerService = customerService,
        billingService = billingService
    ).run()

    //Start Scheduler
    billingScheduler(invoiceService, billingService)
}

private fun billingScheduler(invoiceService: InvoiceService, billingService: BillingService) {
    //prepare date values
    val calendar = Calendar.getInstance() //get calendar GMT +3 TimeZone.getTimeZone("Europe/Athens")
    var year = calendar.get(Calendar.YEAR)
    var month = calendar.get(Calendar.MONTH)

    //if current month is december, next month is 0 and year is year +1
    if (month == 11) {
        month = 0
        year += 1
    } else {
        //else next month
        month += 1
    }

    calendar.set(year, 5, 9, 9, 40,0) //00:00:00 year/month/1
    //calendar.set(year, month, 1, 0, 0,0) //00:00:00 year/month/1
    logger.info { "PaymentProcess next execution is on ${calendar.time}" }

    //calendar to time
    val nextDate = calendar.time
    val t = Timer()
    t.schedule(time = nextDate) {
        logger.info { "Starting startPaymentProcess" }
        val statusText = billingService.startPaymentProcess(invoiceService)
        logger.info { statusText }

        //Schedule next execution
        billingScheduler(invoiceService, billingService) //TODO fix loop issue, whole minute loop
    }
}

