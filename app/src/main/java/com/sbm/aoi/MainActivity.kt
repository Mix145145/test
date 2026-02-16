package com.sbm.aoi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.sbm.aoi.ui.activation.ActivationGate
import com.sbm.aoi.ui.navigation.MainNavigation
import com.sbm.aoi.ui.theme.SbmAoiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SbmAoiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ActivationGate {
                        MainNavigation()
                    }
                }
            }
        }
    }
}
