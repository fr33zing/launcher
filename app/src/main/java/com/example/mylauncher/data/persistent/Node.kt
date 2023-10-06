package com.example.mylauncher.data.persistent

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.mylauncher.data.NodeKind
import com.example.mylauncher.ui.util.UserEditable

const val DEFAULT_NODE_LABEL = "Uncategorized"

@Entity
data class Node(
    @PrimaryKey(autoGenerate = true) val nodeId: Int,
    val parentId: Int?,
    val kind: NodeKind,
    var order: Int,
    @UserEditable(label = "Label") var label: String,
)

@Dao
interface NodeDao {
    @Insert suspend fun insert(node: Node)

    @Transaction @Insert suspend fun insertMany(nodes: List<Node>)

    @Update suspend fun update(node: Node)

    @Transaction @Update suspend fun updateMany(nodes: List<Node>)

    @Delete suspend fun delete(node: Node)

    @Transaction @Insert suspend fun deleteMany(nodes: List<Node>)

    @Query("SELECT * FROM Node WHERE nodeId == :nodeId") suspend fun getNodeById(nodeId: Int): Node?

    @Query("SELECT * FROM Node WHERE label == :label")
    suspend fun getNodeByLabel(label: String): Node?

    @Query("SELECT * FROM Node ORDER BY nodeID DESC LIMIT 1") fun getLastNode(): Node

    @Query("SELECT nodeId FROM Node ORDER BY nodeID DESC LIMIT 1") fun getLastNodeId(): Int

    @Query("SELECT * FROM Node WHERE parentId IS null") suspend fun getTopLevelNodes(): List<Node>

    @Query("SELECT * FROM Node WHERE parentId == :nodeId")
    suspend fun getChildNodes(nodeId: Int?): List<Node>

    suspend fun getDefaultNode(): Node {
        val defaultNode = getNodeByLabel(DEFAULT_NODE_LABEL)
        if (defaultNode != null) return defaultNode

        insert(
            Node(
                nodeId = 0,
                parentId = null,
                kind = NodeKind.Directory,
                order = 0,
                label = DEFAULT_NODE_LABEL
            )
        )
        return getLastNode()
    }
}
