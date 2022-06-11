/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus


class InvoiceService(private val dal: AntaeusDal) {
    //Variables for db layer access range
    //The idea is to cut the db into slices and get a piece to work on
    var databasePointerStart = -1
    var databasePointerFinish = -1

    fun fetchAll(): List<Invoice> {
        return dal.fetchInvoices(databasePointerStart, databasePointerFinish)
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    //This function will call the db layer func to change the invoice status
    fun updateInvoice(id: Int, status: InvoiceStatus) {
        return dal.updateInvoice(id, status)
    }
}
