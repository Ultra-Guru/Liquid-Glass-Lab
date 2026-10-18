package com.ultraguru.liquidglass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ultraguru.liquidglass.ui.AppRoot
import com.ultraguru.liquidglass.ui.theme.GlassLabTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            GlassLabTheme {
                AppRoot()
            }
        }
    }
}
