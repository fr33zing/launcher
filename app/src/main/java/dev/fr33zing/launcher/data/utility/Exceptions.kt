package dev.fr33zing.launcher.data.utility

import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.payloads.Payload

class NullNodeException : Exception("Node is null")

fun Node?.notNull(): Node = this ?: throw NullNodeException()

class NullPayloadException(val node: Node? = null) :
    Exception(
        node?.let { "Payload is null (nodeId: ${it.nodeId}, label: \"${it.label}\")" }
            ?: "Payload is null"
    )

fun Payload?.notNull(node: Node? = null): Payload = this ?: throw NullPayloadException(node)

class PayloadClassMismatchException(val node: Node) :
    Exception("Node ${node.nodeId} kind is ${node.kind} but its payload class does not match")

class UnreachableException(unreachableReason: String) :
    Exception(
        "Code that was meant to be unreachable was executed. Unreachable reason: $unreachableReason"
    )

fun unreachable(unreachableReason: () -> String): Nothing =
    throw UnreachableException(unreachableReason())
