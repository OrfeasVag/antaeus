package io.pleo.antaeus.models

enum class InvoiceStatus {
    PENDING,
    PAID,
    ERRORCNF,//Customer not found
    ERRORCMIS//Currency mismatch
}
