package com.template.contracts

import com.r3.corda.lib.tokens.contracts.commands.Create
import com.template.states.TemplateEvolvableTokenType
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import com.template.states.TemplateState
import net.corda.core.contracts.UniqueIdentifier

class ContractTests {
    private val ledgerServices: MockServices = MockServices(listOf("com.template"))
    var alice = TestIdentity(CordaX500Name("Alice", "TestLand", "US"))
    var bob = TestIdentity(CordaX500Name("Alice", "TestLand", "US"))

    @Test
    fun dummytest() {
        val state = TemplateState("Hello-World", alice.party, bob.party)
        ledgerServices.ledger {
            // Should fail bid price is equal to previous highest bid
            transaction {
                //failing transaction
                input(TemplateContract.ID, state)
                output(TemplateContract.ID, state)
                command(alice.publicKey, TemplateContract.Commands.Create())
                fails()
            }
            //pass
            transaction {
                //passing transaction
                output(TemplateContract.ID, state)
                command(alice.publicKey, TemplateContract.Commands.Create())
                verifies()
            }
        }
    }

    @Test
    fun createEvolvableTokenTypeForTomatoes() {
        val tomatoDefinition = TemplateEvolvableTokenType(
            description = "Organic tomatoes",
            maintainer = alice.party,
            linearId = UniqueIdentifier()
        )
        ledgerServices.ledger {
            transaction {
                output(TemplateEvolvableTokenTypeContract.ID, tomatoDefinition)
                command(alice.publicKey, Create())
                verifies()
            }
        }
    }
}