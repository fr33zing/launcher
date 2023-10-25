// This file is automatically generated, don't edit it!
// Generated by: buildSrc/build.gradle.kts
//
// The generator file must be updated in the following circumstances:
// - Project details (module name, domain, etc) change.
// - NodeKind or payload paths or class names change.
// - New NodeKind variants are added.
// - New payload classes are added.

package dev.fr33zing.launcher.data.persistent

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.payloads.Application
import dev.fr33zing.launcher.data.persistent.payloads.Checkbox
import dev.fr33zing.launcher.data.persistent.payloads.Directory
import dev.fr33zing.launcher.data.persistent.payloads.File
import dev.fr33zing.launcher.data.persistent.payloads.Location
import dev.fr33zing.launcher.data.persistent.payloads.Note
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.data.persistent.payloads.Reference
import dev.fr33zing.launcher.data.persistent.payloads.Reminder
import dev.fr33zing.launcher.data.persistent.payloads.Setting
import dev.fr33zing.launcher.data.persistent.payloads.WebLink
import kotlin.reflect.KParameter
import kotlin.reflect.typeOf

@Suppress("UNCHECKED_CAST")
@Database(
    entities =
        [
            Node::class,
            Application::class,
            Checkbox::class,
            Directory::class,
            File::class,
            Location::class,
            Note::class,
            Reference::class,
            Reminder::class,
            WebLink::class,
            Setting::class,
        ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun nodeDao(): NodeDao

    abstract fun applicationDao(): ApplicationDao

    abstract fun checkboxDao(): CheckboxDao

    abstract fun directoryDao(): DirectoryDao

    abstract fun fileDao(): FileDao

    abstract fun locationDao(): LocationDao

    abstract fun noteDao(): NoteDao

    abstract fun referenceDao(): ReferenceDao

    abstract fun reminderDao(): ReminderDao

    abstract fun webLinkDao(): WebLinkDao

    abstract fun settingDao(): SettingDao

    inline fun <reified T : Payload> nodeKindForPayloadClass(): NodeKind =
        when (T::class) {
            Application::class -> NodeKind.Application
            Checkbox::class -> NodeKind.Checkbox
            Directory::class -> NodeKind.Directory
            File::class -> NodeKind.File
            Location::class -> NodeKind.Location
            Note::class -> NodeKind.Note
            Reference::class -> NodeKind.Reference
            Reminder::class -> NodeKind.Reminder
            WebLink::class -> NodeKind.WebLink
            Setting::class -> NodeKind.Setting
            else -> throw Exception("Invalid payload class: ${T::class}")
        }

    fun createDefaultPayloadForNode(nodeKind: NodeKind, nodeId: Int): Payload {
        val payloadClass =
            when (nodeKind) {
                NodeKind.Application -> Application::class
                NodeKind.Checkbox -> Checkbox::class
                NodeKind.Directory -> Directory::class
                NodeKind.File -> File::class
                NodeKind.Location -> Location::class
                NodeKind.Note -> Note::class
                NodeKind.Reference -> Reference::class
                NodeKind.Reminder -> Reminder::class
                NodeKind.WebLink -> WebLink::class
                NodeKind.Setting -> Setting::class
            }
        val constructor =
            payloadClass.constructors.firstOrNull {
                with(it.parameters[0]) { name == "payloadId" && type == typeOf<Int>() } &&
                    with(it.parameters[1]) { name == "nodeId" && type == typeOf<Int>() } &&
                    it.parameters.subList(2, it.parameters.size).all(KParameter::isOptional)
            } ?: throw Exception("No minimal constructor for payload ${payloadClass.simpleName}")
        return with(constructor) { callBy(mapOf(parameters[0] to 0, parameters[1] to nodeId)) }
    }

    suspend fun getPayloadByNodeId(nodeKind: NodeKind, nodeId: Int): Payload? =
        when (nodeKind) {
            NodeKind.Application -> applicationDao().getPayloadByNodeId(nodeId)
            NodeKind.Checkbox -> checkboxDao().getPayloadByNodeId(nodeId)
            NodeKind.Directory -> directoryDao().getPayloadByNodeId(nodeId)
            NodeKind.File -> fileDao().getPayloadByNodeId(nodeId)
            NodeKind.Location -> locationDao().getPayloadByNodeId(nodeId)
            NodeKind.Note -> noteDao().getPayloadByNodeId(nodeId)
            NodeKind.Reference -> referenceDao().getPayloadByNodeId(nodeId)
            NodeKind.Reminder -> reminderDao().getPayloadByNodeId(nodeId)
            NodeKind.WebLink -> webLinkDao().getPayloadByNodeId(nodeId)
            NodeKind.Setting -> settingDao().getPayloadByNodeId(nodeId)
        }

    suspend fun insert(entity: Any) {
        preInsert(entity)
        when (entity) {
            is Node -> nodeDao().insert(entity)
            is Application -> applicationDao().insert(entity)
            is Checkbox -> checkboxDao().insert(entity)
            is Directory -> directoryDao().insert(entity)
            is File -> fileDao().insert(entity)
            is Location -> locationDao().insert(entity)
            is Note -> noteDao().insert(entity)
            is Reference -> referenceDao().insert(entity)
            is Reminder -> reminderDao().insert(entity)
            is WebLink -> webLinkDao().insert(entity)
            is Setting -> settingDao().insert(entity)
            else -> throw Exception("Invalid entity type: ${entity::class.qualifiedName}")
        }
    }

    suspend fun insertMany(entities: List<Any>) {
        preInsert(entities)
        when (entities.firstOrNull() ?: return) {
            is Node -> nodeDao().insertMany(entities as List<Node>)
            is Application -> applicationDao().insertMany(entities as List<Application>)
            is Checkbox -> checkboxDao().insertMany(entities as List<Checkbox>)
            is Directory -> directoryDao().insertMany(entities as List<Directory>)
            is File -> fileDao().insertMany(entities as List<File>)
            is Location -> locationDao().insertMany(entities as List<Location>)
            is Note -> noteDao().insertMany(entities as List<Note>)
            is Reference -> referenceDao().insertMany(entities as List<Reference>)
            is Reminder -> reminderDao().insertMany(entities as List<Reminder>)
            is WebLink -> webLinkDao().insertMany(entities as List<WebLink>)
            is Setting -> settingDao().insertMany(entities as List<Setting>)
            else -> throw Exception("Invalid entity type: ${entities[0]::class.qualifiedName}")
        }
    }

    suspend fun update(entity: Any) {
        preUpdate(entity)
        when (entity) {
            is Node -> nodeDao().update(entity)
            is Application -> applicationDao().update(entity)
            is Checkbox -> checkboxDao().update(entity)
            is Directory -> directoryDao().update(entity)
            is File -> fileDao().update(entity)
            is Location -> locationDao().update(entity)
            is Note -> noteDao().update(entity)
            is Reference -> referenceDao().update(entity)
            is Reminder -> reminderDao().update(entity)
            is WebLink -> webLinkDao().update(entity)
            is Setting -> settingDao().update(entity)
            else -> throw Exception("Invalid entity type: ${entity::class.qualifiedName}")
        }
    }

    suspend fun updateMany(entities: List<Any>) {
        preUpdate(entities)
        when (entities.firstOrNull() ?: return) {
            is Node -> nodeDao().updateMany(entities as List<Node>)
            is Application -> applicationDao().updateMany(entities as List<Application>)
            is Checkbox -> checkboxDao().updateMany(entities as List<Checkbox>)
            is Directory -> directoryDao().updateMany(entities as List<Directory>)
            is File -> fileDao().updateMany(entities as List<File>)
            is Location -> locationDao().updateMany(entities as List<Location>)
            is Note -> noteDao().updateMany(entities as List<Note>)
            is Reference -> referenceDao().updateMany(entities as List<Reference>)
            is Reminder -> reminderDao().updateMany(entities as List<Reminder>)
            is WebLink -> webLinkDao().updateMany(entities as List<WebLink>)
            is Setting -> settingDao().updateMany(entities as List<Setting>)
            else -> throw Exception("Invalid entity type: ${entities[0]::class.qualifiedName}")
        }
    }

    suspend fun delete(entity: Any) {
        preDelete(entity)
        when (entity) {
            is Node -> nodeDao().delete(entity)
            is Application -> applicationDao().delete(entity)
            is Checkbox -> checkboxDao().delete(entity)
            is Directory -> directoryDao().delete(entity)
            is File -> fileDao().delete(entity)
            is Location -> locationDao().delete(entity)
            is Note -> noteDao().delete(entity)
            is Reference -> referenceDao().delete(entity)
            is Reminder -> reminderDao().delete(entity)
            is WebLink -> webLinkDao().delete(entity)
            is Setting -> settingDao().delete(entity)
            else -> throw Exception("Invalid entity type: ${entity::class.qualifiedName}")
        }
    }

    suspend fun deleteMany(entities: List<Any>) {
        preDelete(entities)
        when (entities.firstOrNull() ?: return) {
            is Node -> nodeDao().deleteMany(entities as List<Node>)
            is Application -> applicationDao().deleteMany(entities as List<Application>)
            is Checkbox -> checkboxDao().deleteMany(entities as List<Checkbox>)
            is Directory -> directoryDao().deleteMany(entities as List<Directory>)
            is File -> fileDao().deleteMany(entities as List<File>)
            is Location -> locationDao().deleteMany(entities as List<Location>)
            is Note -> noteDao().deleteMany(entities as List<Note>)
            is Reference -> referenceDao().deleteMany(entities as List<Reference>)
            is Reminder -> reminderDao().deleteMany(entities as List<Reminder>)
            is WebLink -> webLinkDao().deleteMany(entities as List<WebLink>)
            is Setting -> settingDao().deleteMany(entities as List<Setting>)
            else -> throw Exception("Invalid entity type: ${entities[0]::class.qualifiedName}")
        }
    }

    private fun preInsert(vararg entities: Any) =
        entities.forEach { if (it is Payload) it.preInsert() }

    private fun preUpdate(vararg entities: Any) =
        entities.forEach { if (it is Payload) it.preUpdate() }

    private fun preDelete(vararg entities: Any) =
        entities.forEach { if (it is Payload) it.preDelete() }
}

@Dao
interface ApplicationDao {
    @Insert suspend fun insert(entity: Application)

    @Transaction @Insert suspend fun insertMany(entities: List<Application>)

    @Update suspend fun update(entity: Application)

    @Transaction @Update suspend fun updateMany(entities: List<Application>)

    @Delete suspend fun delete(entity: Application)

    @Transaction @Delete suspend fun deleteMany(entities: List<Application>)

    @Query("SELECT * FROM Application") suspend fun getAllPayloads(): List<Application>

    @Query("SELECT * FROM Application WHERE nodeId = :nodeId")
    suspend fun getPayloadByNodeId(nodeId: Int): Application?
}

@Dao
interface CheckboxDao {
    @Insert suspend fun insert(entity: Checkbox)

    @Transaction @Insert suspend fun insertMany(entities: List<Checkbox>)

    @Update suspend fun update(entity: Checkbox)

    @Transaction @Update suspend fun updateMany(entities: List<Checkbox>)

    @Delete suspend fun delete(entity: Checkbox)

    @Transaction @Delete suspend fun deleteMany(entities: List<Checkbox>)

    @Query("SELECT * FROM Checkbox") suspend fun getAllPayloads(): List<Checkbox>

    @Query("SELECT * FROM Checkbox WHERE nodeId = :nodeId")
    suspend fun getPayloadByNodeId(nodeId: Int): Checkbox?
}

@Dao
interface DirectoryDao {
    @Insert suspend fun insert(entity: Directory)

    @Transaction @Insert suspend fun insertMany(entities: List<Directory>)

    @Update suspend fun update(entity: Directory)

    @Transaction @Update suspend fun updateMany(entities: List<Directory>)

    @Delete suspend fun delete(entity: Directory)

    @Transaction @Delete suspend fun deleteMany(entities: List<Directory>)

    @Query("SELECT * FROM Directory") suspend fun getAllPayloads(): List<Directory>

    @Query("SELECT * FROM Directory WHERE nodeId = :nodeId")
    suspend fun getPayloadByNodeId(nodeId: Int): Directory?
}

@Dao
interface FileDao {
    @Insert suspend fun insert(entity: File)

    @Transaction @Insert suspend fun insertMany(entities: List<File>)

    @Update suspend fun update(entity: File)

    @Transaction @Update suspend fun updateMany(entities: List<File>)

    @Delete suspend fun delete(entity: File)

    @Transaction @Delete suspend fun deleteMany(entities: List<File>)

    @Query("SELECT * FROM File") suspend fun getAllPayloads(): List<File>

    @Query("SELECT * FROM File WHERE nodeId = :nodeId")
    suspend fun getPayloadByNodeId(nodeId: Int): File?
}

@Dao
interface LocationDao {
    @Insert suspend fun insert(entity: Location)

    @Transaction @Insert suspend fun insertMany(entities: List<Location>)

    @Update suspend fun update(entity: Location)

    @Transaction @Update suspend fun updateMany(entities: List<Location>)

    @Delete suspend fun delete(entity: Location)

    @Transaction @Delete suspend fun deleteMany(entities: List<Location>)

    @Query("SELECT * FROM Location") suspend fun getAllPayloads(): List<Location>

    @Query("SELECT * FROM Location WHERE nodeId = :nodeId")
    suspend fun getPayloadByNodeId(nodeId: Int): Location?
}

@Dao
interface NoteDao {
    @Insert suspend fun insert(entity: Note)

    @Transaction @Insert suspend fun insertMany(entities: List<Note>)

    @Update suspend fun update(entity: Note)

    @Transaction @Update suspend fun updateMany(entities: List<Note>)

    @Delete suspend fun delete(entity: Note)

    @Transaction @Delete suspend fun deleteMany(entities: List<Note>)

    @Query("SELECT * FROM Note") suspend fun getAllPayloads(): List<Note>

    @Query("SELECT * FROM Note WHERE nodeId = :nodeId")
    suspend fun getPayloadByNodeId(nodeId: Int): Note?
}

@Dao
interface ReferenceDao {
    @Insert suspend fun insert(entity: Reference)

    @Transaction @Insert suspend fun insertMany(entities: List<Reference>)

    @Update suspend fun update(entity: Reference)

    @Transaction @Update suspend fun updateMany(entities: List<Reference>)

    @Delete suspend fun delete(entity: Reference)

    @Transaction @Delete suspend fun deleteMany(entities: List<Reference>)

    @Query("SELECT * FROM Reference") suspend fun getAllPayloads(): List<Reference>

    @Query("SELECT * FROM Reference WHERE nodeId = :nodeId")
    suspend fun getPayloadByNodeId(nodeId: Int): Reference?
}

@Dao
interface ReminderDao {
    @Insert suspend fun insert(entity: Reminder)

    @Transaction @Insert suspend fun insertMany(entities: List<Reminder>)

    @Update suspend fun update(entity: Reminder)

    @Transaction @Update suspend fun updateMany(entities: List<Reminder>)

    @Delete suspend fun delete(entity: Reminder)

    @Transaction @Delete suspend fun deleteMany(entities: List<Reminder>)

    @Query("SELECT * FROM Reminder") suspend fun getAllPayloads(): List<Reminder>

    @Query("SELECT * FROM Reminder WHERE nodeId = :nodeId")
    suspend fun getPayloadByNodeId(nodeId: Int): Reminder?
}

@Dao
interface WebLinkDao {
    @Insert suspend fun insert(entity: WebLink)

    @Transaction @Insert suspend fun insertMany(entities: List<WebLink>)

    @Update suspend fun update(entity: WebLink)

    @Transaction @Update suspend fun updateMany(entities: List<WebLink>)

    @Delete suspend fun delete(entity: WebLink)

    @Transaction @Delete suspend fun deleteMany(entities: List<WebLink>)

    @Query("SELECT * FROM WebLink") suspend fun getAllPayloads(): List<WebLink>

    @Query("SELECT * FROM WebLink WHERE nodeId = :nodeId")
    suspend fun getPayloadByNodeId(nodeId: Int): WebLink?
}

@Dao
interface SettingDao {
    @Insert suspend fun insert(entity: Setting)

    @Transaction @Insert suspend fun insertMany(entities: List<Setting>)

    @Update suspend fun update(entity: Setting)

    @Transaction @Update suspend fun updateMany(entities: List<Setting>)

    @Delete suspend fun delete(entity: Setting)

    @Transaction @Delete suspend fun deleteMany(entities: List<Setting>)

    @Query("SELECT * FROM Setting") suspend fun getAllPayloads(): List<Setting>

    @Query("SELECT * FROM Setting WHERE nodeId = :nodeId")
    suspend fun getPayloadByNodeId(nodeId: Int): Setting?
}
