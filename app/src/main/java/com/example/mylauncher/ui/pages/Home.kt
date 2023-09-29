package com.example.mylauncher.ui.pages

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.mylauncher.data.persistent.AppDatabase
import com.example.mylauncher.ui.components.NodeList

@Composable
fun Home(db: AppDatabase, navController: NavController) {
    NodeList(db, navController)
}