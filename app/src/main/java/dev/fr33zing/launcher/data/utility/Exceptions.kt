package dev.fr33zing.launcher.data.utility

import dev.fr33zing.launcher.data.persistent.Node

class NullPayloadException(val node: Node) : Exception("Node ${node.nodeId} payload is null.")

class PayloadClassMismatchException(val node: Node) :
    Exception("Node ${node.nodeId} kind is ${node.kind} but its payload class does not match.")
