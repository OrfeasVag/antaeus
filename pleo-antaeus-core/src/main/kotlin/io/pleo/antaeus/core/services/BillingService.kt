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

    // TODO - Add code e.g. here
    /*
    Throws:
    `CustomerNotFoundException`: when no customer has the given id.
    `CurrencyMismatchException`: when the currency does not match the customer account.
    `NetworkException`: when a network error happens.
    */
    fun payInvoice(invoice: Invoice): Boolean {
        try {
            logger.debug { "[BillingService] Now handling - Invoice ${invoice.id} status ${invoice.status}" }
            //check if invoice is already paid
            if (invoice.status == InvoiceStatus.PAID) {
                logger.info { "[BillingService] Fail - Invoice ${invoice.id} status ${invoice.status}" }
                return false
            }
            //attempt to pay the invoice
            if (paymentProvider.charge(invoice)) {
                logger.info { "[BillingService] Success - Invoice ${invoice.id} status ${invoice.status}" }
                return true
            } else {
                logger.info { "[BillingService] Fail - Invoice ${invoice.id} status ${invoice.status}" }
                return false
            }
        } catch (e: CustomerNotFoundException) {
            logger.error(e) { "[BillingService] Exception - Customer not found Invoice ${invoice.id} status ${invoice.status}" }
        } catch (e: CurrencyMismatchException) {
            logger.error(e) { "[BillingService] Exception - Currency Mismatched Invoice ${invoice.id} status ${invoice.status}" }
        } catch (e: NetworkException) {
            logger.error(e) { "[BillingService] Exception - Network exception Invoice ${invoice.id} status ${invoice.status}" }
            //todo retry?
        }
        return true
    }


}
