package dev.fr33zing.launcher.data.persistent.migrations

import androidx.room.RenameTable
import androidx.room.migration.AutoMigrationSpec

@RenameTable(fromTableName = "WebLink", toTableName = "Website")
class RenameWebLinkToWebsite : AutoMigrationSpec
