repositories { mavenCentral() }

dependencies { implementation(gradleApi()) }

plugins { `kotlin-dsl` }

tasks {
    named("compileKotlin") { dependsOn("generateDatabase") }
    register("generateDatabase") {
        // Update these variables when project details change.
        val thisFile = "buildSrc/build.gradle.kts"
        val module = "app"
        val domain = "dev.fr33zing.launcher"
        val migrationsPackage = "$domain.data.persistent.migrations"
        val nodeKindPackage = "$domain.data"
        val payloadsPackage = "$domain.data.persistent.payloads"
        val convertersPackage = "$domain.data.utility"
        val targetPackage = "$domain.data.persistent"
        val targetFileName = "Database.kt"

        // Update these variables when the database schema changes.
        val migrationClasses = mutableListOf<String>()
        fun migration(className: String): String {
            migrationClasses.add(className)
            return "$className::class"
        }
        val databaseVersion = "6"
        val autoMigrations =
            """
                AutoMigration(from = 1, to = 2),
                AutoMigration(from = 2, to = 3),
                AutoMigration(from = 3, to = 4, ${migration("RenameWebLinkToWebsite")}),
                AutoMigration(from = 4, to = 5),
                AutoMigration(from = 5, to = 6),
            """

        // Update these variables to add support for new NodeKind variants and payload classes.
        val payloadClasses =
            listOf(
                "Application",
                "Checkbox",
                "Directory",
                "File",
                "Location",
                "Note",
                "Reference",
                "Reminder",
                "Website",
                "Setting",
            )
        val entityClasses = listOf("Node") + payloadClasses
        val nodeKindToPayloadClassMap = payloadClasses.associateWith { it }
        val extraDaoFunctions =
            mapOf(
                "Directory" to
                    """
                        @Query("SELECT * FROM Directory WHERE specialMode = :specialMode")
                        suspend fun getBySpecialMode(specialMode: Directory.SpecialMode): Directory?
                    """
            )

        // Generate the file.
        val textParts =
            listOf(
                headerComment(thisFile),
                packageDeclaration(targetPackage),
                imports(
                    migrationsPackage,
                    migrationClasses,
                    nodeKindPackage,
                    payloadsPackage,
                    payloadClasses,
                    convertersPackage
                ),
                database(
                    databaseVersion,
                    autoMigrations,
                    entityClasses,
                    nodeKindToPayloadClassMap,
                ),
                allPayloadDaos(payloadClasses, extraDaoFunctions),
            )
        val text = textParts.joinToString("\n\n") + "\n"
        val dir = "../$module/src/main/java/${targetPackage.replace(".", "/")}"
        val f = file("$dir/$targetFileName")
        f.createNewFile()
        f.writeText(text)
    }
}

fun packageDeclaration(targetPackage: String) = "package $targetPackage"

fun headerComment(thisFile: String) =
    """
    // This file is automatically generated, don't edit it!
    // Generated by: $thisFile
    //
    // The generator file must be updated in the following circumstances:
    // - Project details (module name, domain, etc) change.
    // - NodeKind or payload paths or class names change.
    // - New NodeKind variants are added.
    // - New payload classes are added.
    // - Database schema changes for any reason.
    """
        .trimIndent()

fun imports(
    migrationsPackage: String,
    migrationClasses: List<String>,
    nodeKindPackage: String,
    payloadsPackage: String,
    payloadClasses: List<String>,
    convertersPackage: String
) =
    """
    import androidx.room.AutoMigration
    import androidx.room.Dao
    import androidx.room.Database
    import androidx.room.Delete
    import androidx.room.Insert
    import androidx.room.Query
    import androidx.room.RoomDatabase
    import androidx.room.Transaction
    import androidx.room.TypeConverters
    import androidx.room.Update
    import $nodeKindPackage.NodeKind
    ${(migrationClasses).sorted().joinToString("\n${indent(1)}") { "import $migrationsPackage.$it" }}
    ${(payloadClasses + "Payload").sorted().joinToString("\n${indent(1)}") { "import $payloadsPackage.$it" }}
    import $convertersPackage.Converters
    import kotlin.reflect.KParameter
    import kotlin.reflect.typeOf
    import kotlinx.coroutines.flow.Flow
    """
        .trimIndent()

fun payloadDao(payloadClass: String, extraDaoFunctions: String?) =
    """
    @Dao
    interface ${payloadClass}Dao {
        @Insert suspend fun insert(entity: $payloadClass)

        @Transaction @Insert suspend fun insertMany(entities: List<$payloadClass>)

        @Update suspend fun update(entity: $payloadClass)

        @Transaction @Update suspend fun updateMany(entities: List<$payloadClass>)

        @Delete suspend fun delete(entity: $payloadClass)

        @Transaction @Delete suspend fun deleteMany(entities: List<$payloadClass>)

        @Query("SELECT * FROM $payloadClass") suspend fun getAllPayloads(): List<$payloadClass>

        @Query("SELECT * FROM $payloadClass WHERE nodeId = :nodeId")
        suspend fun getPayloadByNodeId(nodeId: Int): $payloadClass?

        @Query("SELECT * FROM $payloadClass WHERE nodeId = :nodeId")
        fun getPayloadFlowByNodeId(nodeId: Int): Flow<$payloadClass?>
${ extraDaoFunctions?.let { "\n"+ it.trimIndent().split("\n").joinToString("\n") { s ->  indent(2) + s }  + "\n" } ?: "" }    }
    """
        .trimIndent()

fun allPayloadDaos(payloadClasses: List<String>, extraDaoFunctions: Map<String, String>) =
    payloadClasses.joinToString("\n\n") { payloadDao(it, extraDaoFunctions[it]) }

fun database(
    databaseVersion: String,
    autoMigrations: String,
    payloadClasses: List<String>,
    nodeKindToPayloadClassMap: Map<String, String>,
) =
    """
    @Suppress("UNCHECKED_CAST")
    @TypeConverters(Converters::class)
    @Database(
        version = $databaseVersion,
        autoMigrations =
            [
                ${autoMigrations.trim()}
            ],
        entities =
            [
                ${payloadClasses.joinToString(",\n${indent(4)}") { "$it::class" }}
            ]
    )
    abstract class AppDatabase : RoomDatabase() {
        ${payloadClasses.joinToString("\n\n${indent(2)}") { "abstract fun ${daoCall(it)}: ${it}Dao" }}

        inline fun <reified T : Payload> nodeKindForPayloadClass(): NodeKind =
            when (T::class) {
${nodeKindToPayloadClassMap.map { "${indent(4)}${it.value}::class -> NodeKind.${it.key}" }.joinToString("\n")}
                else -> throw Exception("Invalid payload class: ${"$"}{T::class}")
            }

        fun createDefaultPayloadForNode(nodeKind: NodeKind, nodeId: Int): Payload {
            val payloadClass =
                when (nodeKind) {
${nodeKindToPayloadClassMap.map { "${indent(5)}NodeKind.${it.key} -> ${it.value}::class" }.joinToString("\n")}
                }
            val constructor =
                payloadClass.constructors.firstOrNull {
                    with(it.parameters[0]) { name == "payloadId" && type == typeOf<Int>() } &&
                        with(it.parameters[1]) { name == "nodeId" && type == typeOf<Int>() } &&
                        it.parameters.subList(2, it.parameters.size).all(KParameter::isOptional)
                } ?: throw Exception("No minimal constructor for payload ${"$"}{payloadClass.simpleName}")
            return with(constructor) { callBy(mapOf(parameters[0] to 0, parameters[1] to nodeId)) }
        }

        suspend fun getPayloadByNodeId(nodeKind: NodeKind, nodeId: Int): Payload? =
            when (nodeKind) {
${nodeKindToPayloadClassMap.map { "${indent(4)}NodeKind.${it.key} -> ${daoCall(it.value)}.getPayloadByNodeId(nodeId)" }.joinToString("\n")}
            }

        fun getPayloadFlowByNodeId(nodeKind: NodeKind, nodeId: Int): Flow<Payload?> =
            when (nodeKind) {
${nodeKindToPayloadClassMap.map { "${indent(4)}NodeKind.${it.key} -> ${daoCall(it.value)}.getPayloadFlowByNodeId(nodeId)" }.joinToString("\n")}
            }

${listOf("insert", "update", "delete").joinToString("\n\n") { bothWriteFunctions(it, payloadClasses) }}

        private fun preInsert(vararg entities: Any) =
            entities.forEach { if (it is Payload) it.preInsert() }

        private fun preUpdate(vararg entities: Any) =
            entities.forEach { if (it is Payload) it.preUpdate() }

        private fun preDelete(vararg entities: Any) =
            entities.forEach { if (it is Payload) it.preDelete() }
    }
    """
        .trimIndent()

fun writeFunction(types: List<String>, name: String, many: Boolean) =
    """
    suspend fun $name${if (many) "Many" else ""}(${if (many) "entities: List<Any>" else "entity: Any"}) {
        pre${capitalize(name)}(${if (many) "entities" else "entity"})
        when (${if (many) "entities.firstOrNull() ?: return" else "entity"}) {
            ${types.joinToString("\n${indent(3)}") { "is $it -> ${daoCall(it)}.$name${if (many) "Many" else ""}(${if (many) "entities as List<$it>" else "entity"})" }}
            else -> throw Exception("Invalid entity type: ${"$"}{${if (many) "entities[0]" else "entity"}::class.qualifiedName}")
        }
    }
    """
        .replaceIndent(indent(2))

fun bothWriteFunctions(name: String, types: List<String>) =
    writeFunction(types, name, false) + "\n\n" + writeFunction(types, name, true)

fun indent(n: Int) = "    ".repeat(n)

fun camelCase(s: String) = s[0].lowercase() + s.substring(1)

fun capitalize(s: String) = s[0].uppercase() + s.substring(1)

fun daoCall(className: String) = "${camelCase(className)}Dao()"
