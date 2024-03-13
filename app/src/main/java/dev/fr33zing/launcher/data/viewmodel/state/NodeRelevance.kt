package dev.fr33zing.launcher.data.viewmodel.state

// TODO think of a better name for this
data class NodeRelevanceWithHiddenChildren(val relevance: NodeRelevance, val hiddenChildren: Int)

enum class NodeRelevance {
    /** Node is relevant to the current mode. Client should display mode-specific controls. */
    Relevant,

    /** Node is irrelevant to the current mode. Client should de-emphasize it. */
    Irrelevant,

    /** Node is disruptive to he current mode. Client should hide it. */
    Disruptive,
}
