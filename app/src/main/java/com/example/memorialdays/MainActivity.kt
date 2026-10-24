package com.example.memorialdays

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.memorialdays.ui.EventViewModel
import com.example.memorialdays.ui.HomeScreen
import com.example.memorialdays.ui.theme.MemorialDaysTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT
            )
        )
        setContent {
            MemorialDaysTheme {
                val viewModel: EventViewModel = viewModel(
                    factory = EventViewModel.factory(application as MemorialApplication)
                )
                HomeScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as? MemorialApplication)?.container?.widgetUpdater?.requestUpdate()
    }
}
