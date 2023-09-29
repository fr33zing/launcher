package com.example.mylauncher.data.persistent

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Update

@Entity
abstract class Payload(
    @PrimaryKey(autoGenerate = true) val payloadId: Int,
    val nodeId: Int,
)

@Dao
interface PayloadDao<T> {
    @Insert fun insert(entity: T)

    @Insert fun insertMany(vararg entities: T)

    @Update fun update(entity: T)

    @Update fun updateMany(vararg entities: T)

    @Delete fun delete(entity: T)

    @Delete fun deleteMany(vararg entities: T)

    fun getAll(): List<T>

    fun getByNodeId(nodeId: Int): T
}
