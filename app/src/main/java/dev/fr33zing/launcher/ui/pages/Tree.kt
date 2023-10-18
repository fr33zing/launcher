package dev.fr33zing.launcher.ui.pages

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.ui.components.RecursiveNodeListSetup

@Composable
fun Tree(db: AppDatabase, navController: NavController, rootNodeId: Int?) {
    RecursiveNodeListSetup(db, navController, rootNodeId)
}
