package com.template

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.utilities.heldTokenAmountCriteria
import com.template.flows.Initiator
import com.template.states.TemplateEvolvableTokenType
import com.template.states.TemplateState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.core.singleIdentity
import net.corda.testing.driver.DriverDSL
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.driver
import net.corda.testing.driver.internal.incrementalPortAllocation
import net.corda.testing.node.TestCordapp
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.Future
import kotlin.test.assertEquals

class DriverBasedTest {
    private val bankA = TestIdentity(CordaX500Name("BankA", "", "GB"))
    private val bankB = TestIdentity(CordaX500Name("BankB", "", "US"))

    @Test
    fun `node test`() = withDriver {
        // Start a pair of nodes and wait for them both to be ready.
        val (partyAHandle, partyBHandle) = startNodes(bankA, bankB)

        // From each node, make an RPC call to retrieve another node's name from the network map, to verify that the
        // nodes have started and can communicate.

        // This is a very basic test: in practice tests would be starting flows, and verifying the states in the vault
        // and other important metrics to ensure that your CorDapp is working as intended.
        assertEquals(bankB.name, partyAHandle.resolveName(bankB.name))
        assertEquals(bankA.name, partyBHandle.resolveName(bankA.name))

        // Invoke the flow to issue a TemplateState
        val partyBWellKnownIdentity = partyBHandle.nodeInfo.legalIdentities.first()
        val finalizedTx = partyAHandle.rpc.startFlowDynamic(Initiator::class.java, partyBWellKnownIdentity)
            .returnValue.getOrThrow()

        // Asserting that the
        assertEquals((finalizedTx.tx.outputStates.first() as TemplateState).msg, "Hello-World")
    }

    @Test
    fun `issue 100 organic apples`() = run {
        driver(
            DriverParameters(
            portAllocation = incrementalPortAllocation(),
                startNodesInProcess = false,
                cordappsForAllNodes = listOf(
                    TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
                    TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows"),
                    TestCordapp.findCordapp("com.r3.corda.lib.tokens.selection")
                ),
                networkParameters = testNetworkParameters(minimumPlatformVersion = 4, notaries = emptyList())
        )) {
            val (partyAHandle) = startNodes(bankA)
            val partyAParty = partyAHandle.nodeInfo.singleIdentity()

            val appleDefinition = TokenType("organic-apples", 0)
            val issuedAppleDefinition = IssuedTokenType(partyAParty, appleDefinition)
            val amountToIssue = Amount(100, issuedAppleDefinition)

            val appleTokenToIssueToIssuer = FungibleToken(amountToIssue, partyAParty)

            // Invoking a flow to issue 100 apples into the bankA vault.
            partyAHandle.rpc
                .startFlowDynamic(IssueTokens::class.java, listOf(appleTokenToIssueToIssuer), emptyList<Party>())
                .returnValue
                .getOrThrow()

            // Assert that 100 apples have been issued into the bankA's vault.
            val queryResult = partyAHandle.rpc.vaultQueryByCriteria(heldTokenAmountCriteria(appleDefinition, partyAParty), FungibleToken::class.java)

            Assert.assertThat(queryResult.states.size, `is`(1))
            Assert.assertThat(queryResult.states.first().state.data.holder, `is`(equalTo((partyAParty as AbstractParty))))
            assertEquals(queryResult.states.first().state.data.amount.quantity, 100)
        }
    }

    // Runs a test inside the Driver DSL, which provides useful functions for starting nodes, etc.
    private fun withDriver(test: DriverDSL.() -> Unit) = driver(
        DriverParameters(isDebug = true, startNodesInProcess = true)
    ) { test() }

    // Makes an RPC call to retrieve another node's name from the network map.
    private fun NodeHandle.resolveName(name: CordaX500Name) = rpc.wellKnownPartyFromX500Name(name)!!.name

    // Resolves a list of futures to a list of the promised values.
    private fun <T> List<Future<T>>.waitForAll(): List<T> = map { it.getOrThrow() }

    // Starts multiple nodes simultaneously, then waits for them all to be ready.
    private fun DriverDSL.startNodes(vararg identities: TestIdentity) = identities
        .map { startNode(providedName = it.name) }
        .waitForAll()
}