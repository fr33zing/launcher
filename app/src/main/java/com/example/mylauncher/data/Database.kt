package com.example.mylauncher.data

import android.content.pm.LauncherActivityInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import com.example.mylauncher.ui.util.UserEditable
import kotlinx.coroutines.flow.Flow

//
// Constants
//

const val DEFAULT_NODE_LABEL = "Uncategorized"

//
// Database setup
//

@Database(entities = [Node::class, App::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun nodeDao(): NodeDao
}

@Dao
interface NodeDao {
    @Insert
    suspend fun insertAllNodes(vararg nodes: Node)

    @Update
    suspend fun update(node: Node)

    @Delete
    suspend fun delete(node: Node)

    @Query("SELECT * FROM Node")
    suspend fun getAllNodes(): List<Node>

    @Query("SELECT * FROM Node WHERE nodeId == :nodeId")
    suspend fun getNodeById(nodeId: Int): Node?

    @Query("SELECT * FROM Node WHERE label == :label")
    suspend fun getNodeByLabel(label: String): Node?

    @Transaction
    @Query("SELECT * FROM Node")
    suspend fun getNodesWithChildren(): List<NodeWithChildren>

    @Query("SELECT * FROM Node ORDER BY nodeID DESC LIMIT 1")
    fun getLastNode(): Node

    @Query("SELECT nodeId FROM Node ORDER BY nodeID DESC LIMIT 1")
    fun getLastNodeId(): Int

    @Query("SELECT * FROM Node WHERE parentId IS null")
    suspend fun getTopLevelNodes(): List<Node>

    @Query("SELECT * FROM Node WHERE parentId IS null")
    fun getTopLevelNodesFlow(): Flow<List<Node>>

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
                label = DEFAULT_NODE_LABEL
            )
        )
        return getLastNode()
    }

    @Query("SELECT * FROM App")
    suspend fun getAllApps(): List<App>

    @Query("SELECT * FROM App WHERE nodeId == :nodeId")
    suspend fun getApp(nodeId: Int): App

    @Insert
    suspend fun insertAllApps(vararg app: App)

    @Transaction
    suspend fun insertNewApps(activityInfos: List<LauncherActivityInfo>) {
        val defaultNode = getDefaultNode()
        val apps = getAllApps()

        activityInfos.filter { activityInfo -> apps.find { app -> app.appName == activityInfo.label.toString() } == null }
            .forEach { activityInfo ->
                val node = Node(
                    nodeId = 0,
                    parentId = defaultNode.nodeId,
                    dataId = null,
                    kind = NodeKind.Application,
                    label = activityInfo.label.toString()
                )
                insertAllNodes(node)

                val app = App(
                    appId = 0,
                    nodeId = getLastNodeId(),
                    activityInfo = activityInfo,
                )
                insertAllApps(app)
            }
    }
}

//
// Generic node
//

@Entity
data class Node(
    @PrimaryKey(autoGenerate = true) val nodeId: Int,
    val parentId: Int?,
    val dataId: Int?,
    val kind: NodeKind,
    @UserEditable(label = "Label") var label: String,
)

data class NodeWithChildren(
    @Embedded val parent: Node,
    @Relation(parentColumn = "nodeId", entityColumn = "parentId") val children: List<Node>,
)

//
// Extended node data types
//

// App
@Entity
data class App(
    @PrimaryKey(autoGenerate = true) val appId: Int,
    val nodeId: Int,
    val appName: String,
    val packageName: String,
    val activityClassName: String?,
    val userHandle: String,
) {
    constructor(appId: Int, nodeId: Int, activityInfo: LauncherActivityInfo) : this(
        appId = appId,
        nodeId = nodeId,
        appName = activityInfo.label.toString(),
        packageName = activityInfo.applicationInfo.packageName,
        activityClassName = activityInfo.componentName.className,
        userHandle = activityInfo.user.toString()
    )
}

@Dao
interface AppDao {
    @Insert
    suspend fun insertAll(vararg apps: App)

    @Delete
    suspend fun delete(app: App)

    @Query("SELECT * FROM App")
    suspend fun getAll(): List<App>

    @Query("SELECT * FROM App WHERE nodeId == :nodeId")
    suspend fun getApp(nodeId: Int): App
}