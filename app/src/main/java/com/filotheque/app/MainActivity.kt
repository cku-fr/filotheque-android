package com.filotheque.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.filotheque.app.ui.screens.MainScreen
import com.filotheque.app.ui.theme.FilothequeTheme
import com.filotheque.app.ui.viewmodel.ProjectViewModel
import com.filotheque.app.ui.viewmodel.ThreadViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as FilothequeApplication).container

        setContent {
            FilothequeTheme {
                val threadViewModel: ThreadViewModel = viewModel(
                    factory = ThreadViewModel.Factory(container.threadRepository)
                )
                val projectViewModel: ProjectViewModel = viewModel(
                    factory = ProjectViewModel.Factory(container.projectRepository)
                )
                MainScreen(
                    threadViewModel = threadViewModel,
                    projectViewModel = projectViewModel
                )
            }
        }
    }
}
