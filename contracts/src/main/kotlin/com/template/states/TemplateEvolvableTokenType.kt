package com.template.states

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.template.contracts.TemplateEvolvableTokenTypeContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@BelongsToContract(TemplateEvolvableTokenTypeContract::class)
class TemplateEvolvableTokenType(
    val description: String,
    val maintainer: Party,
    override val linearId: UniqueIdentifier,
    override val fractionDigits: Int = 0
): EvolvableTokenType() {
    override val maintainers: List<Party>
        get() = listOf(maintainer)
}