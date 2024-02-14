package dev.fr33zing.launcher.ui.components.tree.utility

import androidx.compose.runtime.compositionLocalOf
import java.util.EnumSet

typealias NodeRowFeatureSet = EnumSet<NodeRowFeatures>

val LocalNodeRowFeatures = compositionLocalOf { NodeRowFeatures.All }

enum class NodeRowFeatures {
    APPEAR_ANIMATION,

    /** If true, tapping the NodeRow will call its state's activate function. */
    ACTIVATE,

    /** If true, the NodeRow will consider the state when rendering. */
    RENDER_STATE,

    // TODO document this
    FOLLOW_REFERENCES,

    /** If true, long pressing the NodeRow will show buttons to create new nodes adjacent to it. */
    CREATE_ADJACENT,

    /** If true, long pressing the NodeRow will show action buttons over itself. */
    ACTION_BUTTONS,
    ;

    companion object FeatureSets {
        val All: NodeRowFeatureSet = EnumSet.allOf(NodeRowFeatures::class.java)
        val None: NodeRowFeatureSet = EnumSet.noneOf(NodeRowFeatures::class.java)
        val Minimal: NodeRowFeatureSet = EnumSet.of(ACTIVATE, RENDER_STATE, FOLLOW_REFERENCES)
    }
}

fun NodeRowFeatureSet.interactive() =
    contains(NodeRowFeatures.CREATE_ADJACENT) || contains(NodeRowFeatures.ACTION_BUTTONS)