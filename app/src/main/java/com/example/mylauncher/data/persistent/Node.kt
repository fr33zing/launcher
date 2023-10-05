package com.example.mylauncher.data.persistent

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.example.mylauncher.data.NodeKind
import com.example.mylauncher.ui.util.UserEditable
import kotlinx.coroutines.flow.Flow

@Entity
data class Node(
    @PrimaryKey(autoGenerate = true) val nodeId: Int,
    val parentId: Int?,
    val dataId: Int?,
    val kind: NodeKind,
    var order: Int,
    @UserEditable(label = "Label") var label: String,
)

data class NodeWithChildren(
    @Embedded val parent: Node,
    @Relation(parentColumn = "nodeId", entityColumn = "parentId") val children: List<Node>,
)

@Dao
interface NodeDao {
    @Insert suspend fun insert(node: Node)

    @Insert suspend fun insertAllNodes(vararg nodes: Node)

    @Update suspend fun update(node: Node)

    @Delete suspend fun delete(node: Node)

    @Query("SELECT * FROM Node") suspend fun getAllNodes(): List<Node>

    @Query("SELECT * FROM Node WHERE nodeId == :nodeId") suspend fun getNodeById(nodeId: Int): Node?

    @Query("SELECT * FROM Node WHERE label == :label")
    suspend fun getNodeByLabel(label: String): Node?

    @Transaction
    @Query("SELECT * FROM Node")
    suspend fun getNodesWithChildren(): List<NodeWithChildren>

    @Query("SELECT * FROM Node ORDER BY nodeID DESC LIMIT 1") fun getLastNode(): Node

    @Query("SELECT nodeId FROM Node ORDER BY nodeID DESC LIMIT 1") fun getLastNodeId(): Int

    @Query("SELECT * FROM Node WHERE parentId IS null") suspend fun getTopLevelNodes(): List<Node>

    @Query("SELECT * FROM Node WHERE parentId IS null") fun getTopLevelNodesFlow(): Flow<List<Node>>

    @Query("SELECT * FROM Node WHERE parentId == :nodeId")
    suspend fun getChildNodes(nodeId: Int?): List<Node>

    @Query("SELECT * FROM Node WHERE parentId == :nodeId")
    fun getChildNodesFlow(nodeId: Int?): Flow<List<Node>>

    suspend fun getDefaultNode(): Node {
        val defaultNode = getNodeByLabel(DEFAULT_NODE_LABEL)
        if (defaultNode != null) return defaultNode

        insertAllNodes(
            Node(
                nodeId = 0,
                parentId = null,
                dataId = null,
                kind = NodeKind.Directory,
                order = 0,
                label = DEFAULT_NODE_LABEL
            )
        )
        return getLastNode()
    }
}
