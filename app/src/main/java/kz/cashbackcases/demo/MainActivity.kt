package kz.cashbackcases.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.cashbackcases.demo.ui.CashbackApp
import kz.cashbackcases.demo.ui.CashbackViewModel
import kz.cashbackcases.demo.ui.theme.CashbackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(0xFF0B0E14.toInt()),
            navigationBarStyle = SystemBarStyle.dark(0xFF0B0E14.toInt()),
        )
        val app = application as CashbackApplication
        setContent {
            val viewModel: CashbackViewModel = viewModel(factory = CashbackViewModel.factory(app.repository))
            CashbackTheme {
                CashbackApp(viewModel)
            }
        }
    }
}
