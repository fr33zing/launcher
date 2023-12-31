package dev.fr33zing.launcher.data.persistent

import androidx.annotation.Keep
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.ui.utility.UserEditable

/**
 * Ensure that all node order values are unique and sequential. Mutates this List and returns itself
 * for convenience.
 */
fun List<Node>.fixOrder(): List<Node> {
    sortedBy { it.order }.forEachIndexed { index, node -> node.order = index }
    return this
}

@Keep
@Entity
data class Node(
    @PrimaryKey(autoGenerate = true) val nodeId: Int,
    var parentId: Int?,
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

    @Transaction @Delete suspend fun deleteMany(nodes: List<Node>)

    @Query("SELECT * FROM Node") suspend fun getAll(): List<Node>

    @Query("SELECT * FROM Node WHERE nodeId == :nodeId") suspend fun getNodeById(nodeId: Int): Node?

    @Query("SELECT * FROM Node WHERE label == :label")
    suspend fun getNodeByLabel(label: String): Node?

    @Query("SELECT * FROM Node WHERE parentId == :parentId AND label == :label")
    suspend fun getChildNodeByLabel(parentId: Int?, label: String): Node?

    @Query("SELECT * FROM Node ORDER BY nodeId DESC LIMIT 1") fun getLastNode(): Node

    @Query("SELECT nodeId FROM Node ORDER BY nodeId DESC LIMIT 1") fun getLastNodeId(): Int

    @Query("SELECT * FROM Node WHERE parentId == :nodeId ORDER BY Node.`order` ASC")
    suspend fun getChildNodes(nodeId: Int?): List<Node>

    @Query(
        "SELECT Node.`order` FROM Node where parentId == :parentId ORDER BY Node.`order` DESC LIMIT 1"
    )
    suspend fun getLastNodeOrder(parentId: Int?): Int
}
