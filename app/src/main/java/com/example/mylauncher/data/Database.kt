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

    @Delete
    suspend fun delete(node: Node)

    @Query("SELECT * FROM Node")
    suspend fun getAllNodes(): List<Node>

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

        insertAllNodes(Node(0, null, null, NodeKind.Directory, DEFAULT_NODE_LABEL))
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
                val appName = activityInfo.label.toString()
                val node = Node(
                    nodeId = 0,
                    parentId = defaultNode.nodeId,
                    dataId = null,
                    kind = NodeKind.App,
                    label = appName
                )
                val app = App(
                    appId = 0,
                    nodeId = getLastNodeId(),
                    appName = appName,
                    packageName = activityInfo.applicationInfo.packageName,
                    activityClassName = activityInfo.componentName.className,
                    userHandle = activityInfo.user.toString()
                )

                insertAllNodes(node)
                insertAllApps(app)
            }
    }
}

//
// Generic node
//

enum class NodeKind {
    Reference, Directory, App,
}

@Entity
data class Node(
    @PrimaryKey(autoGenerate = true) val nodeId: Int,
    val parentId: Int?,
    val dataId: Int?,
    val kind: NodeKind,
    val label: String,
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
)

//data class NodeApp(
//    @Embedded val node: Node,
//    @Relation(parentColumn = "nodeId", entityColumn = "dataId") val app: App,
//)

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

//
// Helper functions
//

//suspend fun addNewApps(db: AppDatabase, appModels: List<AppModel>) {
//    db.runInTransaction({
//        val defaultNode = db.nodeDao()
//            .getDefaultNode()
//        val apps = db.appDao()
//            .getAll()
//
//        appModels.filter { model -> apps.find { app -> app.appName == model.appName } != null }
//            .forEach { model ->
//                val node = Node(
//                    nodeId = 0,
//                    parentId = defaultNode.nodeId,
//                    dataId = null,
//                    kind = NodeKind.App,
//                    label = model.appName
//                )
//                db.nodeDao()
//                    .insertAll(node)
//
//                val app = App(
//                    appId = 0,
//                    nodeId = node.nodeId,
//                    appName = model.appName,
//                    packageName = model.appPackageName,
//                    activityClassName = model.activityClassName
//                )
//                db.appDao()
//                    .insertAll(app)
//            }
//    })
//}