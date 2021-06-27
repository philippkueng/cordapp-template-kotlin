package com.template.contracts

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

/**
 * This doesn't do anything over and above the [EvolvableTokenContract].
 */
class TemplateEvolvableTokenTypeContract : EvolvableTokenContract(), Contract {
    override fun additionalCreateChecks(tx: LedgerTransaction) = Unit

    override fun additionalUpdateChecks(tx: LedgerTransaction) = Unit

    companion object {
        val ID: String = this::class.java.enclosingClass.canonicalName
    }
}