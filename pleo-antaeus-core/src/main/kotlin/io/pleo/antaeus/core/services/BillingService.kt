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
        // 0 success, 1 failed, 2 network error, 3 error customer not found , 4 error currency mismatch
        try {
            //check if invoice is already paid, this is a double check
            if (invoice.status == InvoiceStatus.PAID) {
                logger.info { "Invoice ${invoice.id} status ${invoice.status}" }
                return 1 //failed
            }
            //attempt to pay the invoice
            if (!paymentProvider.charge(invoice)) {
                //logger.info { "Insufficient funds - Invoice ${invoice.id}" }
                return 1 //failed
            }
        } catch (e: CustomerNotFoundException) {
            logger.error(e) { "Customer not found - Invoice ${invoice.id}" }
            return 3 //error customer not found
        } catch (e: CurrencyMismatchException) {
            logger.error(e) { "Currency Mismatched - Invoice ${invoice.id}" }
            return 4 //error currency mismatch
        } catch (e: NetworkException) {
            logger.error(e) { "Network exception - Invoice ${invoice.id}" }
            if (retryCounter < totalRetries) payInvoice(
                invoice,
                retryCounter + 1
            ) else return 2 //try again or net error
        } catch (e: Exception) {
            logger.error(e) { "Undefined Exception - Invoice ${invoice.id} status ${invoice.status}" }
            return 2 //error
        }
        return 0 //all ok success
    }

    //Return message to report the payment status
    fun startPaymentProcess(invoiceService: InvoiceService): String {
        logger.info { "Starting payment process. . ." }
        val invoiceList = invoiceService.fetchAll()
        var completedCounter = 0 //0
        var insFundsCounter = 0 //1
        var networkErrorCounter = 0 //2
        var cnfErrorCounter = 0 //3
        var cmisErrorCounter = 0 //4
        for (invoice in invoiceList) {
            if (invoice.status == InvoiceStatus.PENDING) {
                //add something like change status INPROGRESS in order to avoid overlapping ( but gonna have multiple db updates)
                when (payInvoice(invoice)) {
                    0 -> {
                        invoiceService.updateInvoice(invoice.id, InvoiceStatus.PAID)
                        completedCounter++
                    }
                    1 -> insFundsCounter++
                    2 -> networkErrorCounter++
                    3 -> {
                        cnfErrorCounter++
                        invoiceService.updateInvoice(invoice.id, InvoiceStatus.ERRORCNF) //update status for manual actions
                    }
                    4 -> {
                        cmisErrorCounter++
                        invoiceService.updateInvoice(invoice.id, InvoiceStatus.ERRORCMIS) //update status for manual actions
                    }
                }
            }
        }
        return "Invoice payment process has been executed. Completed: $completedCounter, Insufficient Funds: $insFundsCounter, Network errors: $networkErrorCounter, Customer not found errors:$cnfErrorCounter, Currency mismatch errors: $cmisErrorCounter"
    }
}
