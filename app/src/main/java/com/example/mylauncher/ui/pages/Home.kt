package com.example.mylauncher.ui.pages

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.mylauncher.data.AppDatabase
import com.example.mylauncher.ui.components.NodeList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(db: AppDatabase, navController: NavController) {
    NodeList(db)
}