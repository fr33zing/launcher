repositories { mavenCentral() }

dependencies { implementation(gradleApi()) }

plugins { `kotlin-dsl` }

tasks {
    named("compileKotlin") { dependsOn("generateDatabase") }
    register("generateDatabase") {
        // Update these variables when project details change.
        val thisFile = "buildSrc/build.gradle.kts"
        val module = "app"
        val domain = "com.example.mylauncher"
        val nodeKindPackage = "$domain.data"
        val payloadsPackage = "$domain.data.persistent.payloads"
        val targetPackage = "$domain.data.persistent"
        val targetFileName = "Database.kt"

        // Update these variables to add support for new NodeKind variants and payload classes.
        val databaseVersion = "1"
        val payloadClasses = listOf("Application")
        val entityClasses = listOf("Node") + payloadClasses
        val nodeKindToPayloadClassMap = mapOf("Application" to "Application")

        // Generate the file.
        val textParts =
            listOf(
                headerComment(thisFile),
                packageDeclaration(targetPackage),
                imports(nodeKindPackage, payloadsPackage, payloadClasses),
                database(databaseVersion, entityClasses, nodeKindToPayloadClassMap),
                allPayloadDaos(payloadClasses),
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
    """
        .trimIndent()

fun imports(nodeKindPackage: String, payloadsPackage: String, payloadClasses: List<String>) =
    """
    import androidx.room.Dao
    import androidx.room.Database
    import androidx.room.Delete
    import androidx.room.Insert
    import androidx.room.Query
    import androidx.room.RoomDatabase
    import androidx.room.Transaction
    import androidx.room.Update
    import $nodeKindPackage.NodeKind
    ${(payloadClasses + "Payload").sorted().joinToString("\n${indent(1)}") { "import $payloadsPackage.$it" }}
    import kotlin.reflect.KParameter
    import kotlin.reflect.typeOf
    """
        .trimIndent()

fun payloadDao(payloadClass: String) =
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
    }
    """
        .trimIndent()

fun allPayloadDaos(payloadClasses: List<String>) =
    payloadClasses.joinToString("\n\n") { payloadDao(it) }

fun database(
    databaseVersion: String,
    payloadClasses: List<String>,
    nodeKindToPayloadClassMap: Map<String, String>,
) =
    """
    @Suppress("UNCHECKED_CAST")
    @Database(entities = [${payloadClasses.joinToString(", ") { "$it::class" }}], version = $databaseVersion)
    abstract class AppDatabase : RoomDatabase() {
        ${payloadClasses.joinToString("\n\n${indent(2)}") { "abstract fun ${daoCall(it)}: ${it}Dao" }}

${listOf("insert", "update", "delete").joinToString("\n\n") { bothWriteFunctions(it, payloadClasses) }}

        suspend fun getPayloadByNodeId(nodeKind: NodeKind, nodeId: Int): Payload? =
            when (nodeKind) {
${nodeKindToPayloadClassMap.map { "${indent(4)}NodeKind.${it.key} -> ${daoCall(it.value)}.getPayloadByNodeId(nodeId)" }.joinToString("\n")}
                else -> throw Exception("Invalid NodeKind")
            }

        fun createDefaultPayloadForNode(nodeKind: NodeKind, nodeId: Int): Payload? {
            val payloadClass =
                when (nodeKind) {
${nodeKindToPayloadClassMap.map { "${indent(5)}NodeKind.${it.key} -> ${it.value}::class" }.joinToString("\n")}
                    else -> return null
                }
            val constructor =
                payloadClass.constructors.firstOrNull {
                    with(it.parameters[0]) { name == "payloadId" && type == typeOf<Int>() } &&
                        with(it.parameters[1]) { name == "nodeId" && type == typeOf<Int>() } &&
                        it.parameters.subList(2, it.parameters.size).all(KParameter::isOptional)
                } ?: throw Exception("No minimal constructor for payload ${"$"}{payloadClass.simpleName}")
            return constructor.call(0, nodeId)
        }
    }
    """
        .trimIndent()

fun writeFunction(types: List<String>, name: String, many: Boolean) =
    """
    suspend fun $name(${if (many) "entities: List<Any>" else "entity: Any"}) {
        when (${if (many) "entities[0]" else "entity"}) {
            ${types.joinToString("\n${indent(3)}") { "is $it -> ${daoCall(it)}.$name(${if (many) "entities as List<$it>" else "entity"})" }}
            else -> throw Exception("Invalid entity type")
        }
    }
    """
        .replaceIndent(indent(2))

fun bothWriteFunctions(name: String, types: List<String>) =
    writeFunction(types, name, false) + "\n\n" + writeFunction(types, "${name}Many", true)

fun indent(n: Int) = "    ".repeat(n)

fun camelCase(s: String) = s[0].lowercase() + s.substring(1)

fun daoCall(className: String) = "${camelCase(className)}Dao()"