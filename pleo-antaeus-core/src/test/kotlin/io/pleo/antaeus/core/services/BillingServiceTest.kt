package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BillingServiceTest {

    val inv1 = Invoice(1, 8, Money(66.99.toBigDecimal(), Currency.USD), InvoiceStatus.PENDING)
    val inv2 = Invoice(2, 7, Money(66.10.toBigDecimal(), Currency.USD), InvoiceStatus.PAID)
    val inv3 = Invoice(3, 6, Money(66.11.toBigDecimal(), Currency.USD), InvoiceStatus.PENDING)
    val inv4 = Invoice(4, 5, Money(66.22.toBigDecimal(), Currency.USD), InvoiceStatus.PENDING)
    val inv5 = Invoice(5, 4, Money(66.44.toBigDecimal(), Currency.USD), InvoiceStatus.PENDING)
    val inv6 = Invoice(6, 3, Money(66.33.toBigDecimal(), Currency.USD), InvoiceStatus.PENDING)

    private val paymentProvider = mockk<PaymentProvider> {
        every { charge(inv1) } returns true
        every { charge(inv2) } returns true //will not be called
        every { charge(inv3) } returns false
        every { charge(inv4) } throws CustomerNotFoundException(4)
        every { charge(inv5) } throws CurrencyMismatchException(5,4)
        every { charge(inv6) } throws Exception()
    }

    private val billingService = BillingService(paymentProvider = paymentProvider)

    @Test
    fun `Sunny day should return 0`() {
        assertEquals(0, billingService.payInvoice(inv1))
    }

    @Test
    fun `Already paid should return 1`() {
        assertEquals(1, billingService.payInvoice(inv2))
    }

    @Test
    fun `Insufficient funds should return 1`() {
        assertEquals(1, billingService.payInvoice(inv3))
    }

    @Test
    fun `Customer not found should return 3`() {
        assertEquals(3, billingService.payInvoice(inv4))
    }

    @Test
    fun `Currency mismatched should return 4`() {
        assertEquals(4, billingService.payInvoice(inv5))
    }

    @Test
    fun `Other error should return 2`() {
        assertEquals(2, billingService.payInvoice(inv6))
    }
}