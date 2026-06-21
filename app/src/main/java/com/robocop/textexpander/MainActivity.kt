package com.robocop.textexpander

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.robocop.textexpander.ui.navigation.RobocopNavHost
import com.robocop.textexpander.ui.theme.RobocopTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RobocopTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RobocopNavHost()
                }
            }
        }
    }
}
