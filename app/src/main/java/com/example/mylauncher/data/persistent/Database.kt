package com.example.mylauncher.data.persistent

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.mylauncher.data.NodeKind
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

const val DEFAULT_NODE_LABEL = "Uncategorized"

/**
 * The main application database for persistent storage of nodes and their payloads.
 *
 * To add a new payload type:
 * - Create an entity that inherits [Payload].
 * - Add `Example::class` to `entities` in the [Database] annotation below.
 * - Update [com.example.mylauncher.data.NodeKind.payloadClass]
 * - Create a [PayloadDao] for your entity and implement the `get*` functions.
 * - Add `abstract fun exampleDao(): ExampleDao` below.
 * - Add `Example::class to ::exampleDao` to [payloadDaos] below.
 * - Update [com.example.mylauncher.ui.pages.EditForm] if needed.
 */
@Database(entities = [Node::class, Application::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun nodeDao(): NodeDao

    abstract fun applicationDao(): ApplicationDao

    private val payloadDaos =
        mapOf<KClass<*>, KFunction<PayloadDao<*>>>(Application::class to ::applicationDao)

    fun payloadDao(entityClass: KClass<*>): PayloadDao<*> = payloadDaos[entityClass]!!.call()

    fun payloadDao(nodeKind: NodeKind): PayloadDao<*>? =
        nodeKind.payloadClass()?.let { payloadDao(it) }
}
