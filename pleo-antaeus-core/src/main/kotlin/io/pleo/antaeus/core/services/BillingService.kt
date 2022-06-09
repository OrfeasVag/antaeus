package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

class BillingService(
    private val paymentProvider: PaymentProvider
) {
    private val logger = KotlinLogging.logger {}//added logger
    private var totalRetries = 2

    private fun payInvoice(invoice: Invoice, retryCounter: Int = 0): Int {
        // 0 success, 1 failed, 2 error
        try {
            //check if invoice is already paid
            if (invoice.status == InvoiceStatus.PAID) {
                logger.info { "Invoice ${invoice.id} status ${invoice.status}" }
                return 1 //failed
            }
            //attempt to pay the invoice
            if (!paymentProvider.charge(invoice)) {
                logger.info { "Insufficient funds - Invoice ${invoice.id}" }
                return 1 //failed
            }
        } catch (e: CustomerNotFoundException) {
            logger.error(e) { "Customer not found - Invoice ${invoice.id}" }
            return 2 //error
        } catch (e: CurrencyMismatchException) {
            logger.error(e) { "Currency Mismatched - Invoice ${invoice.id}" }
            return 2 //error
        } catch (e: NetworkException) {
            logger.error(e) { "Network exception - Invoice ${invoice.id}" }
            if (retryCounter < totalRetries) payInvoice(invoice, retryCounter + 1) else return 2 //try again or error
        } catch (e: Exception) {
            logger.error(e) { "Undefined Exception - Invoice ${invoice.id} status ${invoice.status}" }
            return 2 //error
        }
        return 0 //all ok success
    }

    //Return message to report the payment status
    fun startPaymentProcess(invoiceService: InvoiceService): String {
        val invoiceList = invoiceService.fetchAll()
        var completedCounter = 0
        var insFundsCounter = 0
        var errorCounter = 0
        for (invoice in invoiceList) {
            if (invoice.status == InvoiceStatus.PENDING) {
                when (payInvoice(invoice)) {
                    0 -> {
                        invoiceService.updateInvoice(invoice.id, InvoiceStatus.PAID)
                        completedCounter++
                    }
                    1 -> insFundsCounter++
                    2 -> errorCounter++
                }
            }
        }
        return "Invoice payment process has been executed. Completed: $completedCounter, Insufficient Funds: $insFundsCounter, Errors: $errorCounter"
    }
}
