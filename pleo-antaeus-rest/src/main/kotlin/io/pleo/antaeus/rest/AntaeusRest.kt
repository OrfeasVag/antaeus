/*
    Configures the rest app along with basic exception handling and URL endpoints.
 */

package io.pleo.antaeus.rest

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.pleo.antaeus.core.exceptions.EntityNotFoundException
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
private val thisFile: () -> Unit = {}

class AntaeusRest(
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService,
    private val billingService: BillingService
) : Runnable {

    override fun run() {
        app.start(7000)
    }

    // Set up Javalin rest app
    private val app = Javalin
        .create()
        .apply {
            // InvoiceNotFoundException: return 404 HTTP status code
            exception(EntityNotFoundException::class.java) { _, ctx ->
                ctx.status(404)
            }
            // Unexpected exception: return HTTP 500
            exception(Exception::class.java) { e, _ ->
                logger.error(e) { "Internal server error" }
            }
            // On 404: return message
            error(404) { ctx -> ctx.json("not found") }
        }

    init {
        // Set up URL endpoints for the rest app
        app.routes {
            get("/") {
                it.result("Welcome to Antaeus! see AntaeusRest class for routes")
            }
            path("rest") {
                // Route to check whether the app is running
                // URL: /rest/health
                get("health") {
                    it.json("ok")
                }

                // URL: /rest/info
                get("info") {
                    if ((invoiceService.databasePointerStart >= invoiceService.databasePointerFinish) || (invoiceService.databasePointerStart < 1 || invoiceService.databasePointerFinish < 1)) {
                        it.json("This worker works on the whole db.")
                    } else {
                        it.json("This worker works on the range:(${invoiceService.databasePointerStart} - ${invoiceService.databasePointerFinish})")
                    }
                }

                // /rest/config?dbstart=?&dbend=?
                put("config") {
                    try {
                        val dbstart: Int? = it.queryParam("dbstart")?.toIntOrNull()
                        val dbend: Int? = it.queryParam("dbend")?.toIntOrNull()
                        if (dbstart == null || dbend == null) {
                            it.json("Error - check again: dbstart, dbend")
                        } else {
                            if ((dbstart >= dbend) || (dbstart < 1 || dbend < 1)) { //check the given values
                                it.json("Error - check again: dbstart, dbend. values > 0, dbstart < dbend")
                            } else {
                                invoiceService.databasePointerStart = dbstart
                                invoiceService.databasePointerFinish = dbend
                                it.json("Pointers have been updated dbstart: ${invoiceService.databasePointerStart}, dbend: ${invoiceService.databasePointerFinish}")
                            }
                        }
                    } catch (e: Exception) {
                        it.json("Please check again the given params. Need dbstart, dbend")
                    }
                }


                // V1
                path("v1") {
                    path("invoices") {
                        // URL: /rest/v1/invoices
                        get {
                            it.json(invoiceService.fetchAll())
                        }

                        // URL: /rest/v1/invoices/{:id}
                        get(":id") {
                            try {
                                it.json(invoiceService.fetch(it.pathParam("id").toInt()))
                            } catch (e: NumberFormatException) {
                                it.json("Please check again the given id.")
                                it.status(500)
                            }
                        }

                        path("execute") {
                            // URL: /rest/v1/invoices/execute
                            //ad hoc execution of the payment process
                            post {
                                it.json(billingService.startPaymentProcess(invoiceService))
                            }
                        }

                    }

                    path("customers") {
                        // URL: /rest/v1/customers
                        get {
                            it.json(customerService.fetchAll())
                        }

                        // URL: /rest/v1/customers/{:id}
                        get(":id") {
                            it.json(customerService.fetch(it.pathParam("id").toInt()))
                        }
                    }
                }
            }
        }
    }
}
