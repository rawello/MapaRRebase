package com.rawello.mapar.presentation

import android.graphics.Color.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColor
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.rawello.mapar.MapaRApplication
import com.rawello.mapar.data.TokenManager
import com.rawello.mapar.data.database.NodeViewModel
import com.rawello.mapar.data.network.ApiFactory
import com.rawello.mapar.presentation.buildSelection.SelectBuildScreen
import com.rawello.mapar.presentation.onboarding.OnboardingScreen
import com.rawello.mapar.presentation.qrAlpha.QrScanViewModel
import com.rawello.mapar.presentation.qrAlpha.QrScanningScreen
import com.rawello.mapar.presentation.scenePage.ArPage
import com.rawello.mapar.presentation.scenePage.ArPageViewModel
import com.rawello.mapar.presentation.textRecognition.TextRecognitionScreen
import com.rawello.mapar.ui.theme.MapaRTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainActivity : ComponentActivity() {

    @Inject
    lateinit var nodeViewModel: NodeViewModel

    @Inject
    lateinit var arPageViewModel: ArPageViewModel

    @OptIn(ExperimentalGetImage::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            // Инициализируем TokenManager при запуске
            TokenManager.getInstance(this@MainActivity).initialize()
        }

        (application as MapaRApplication).appComponent.inject(this)

        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            MapaRTheme {
                val systemUiController = rememberSystemUiController()
                val darkTheme = isSystemInDarkTheme()

                Surface {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = Routes.First.string) {
                        composable(Routes.First.string) { OnboardingScreen(navController) }
                        composable(Routes.Second.string) { SelectBuildScreen(navController, nodeViewModel) }
                        composable(Routes.QrAlpha.string) { QrScanningScreen(navController = navController) }
                        composable(Routes.Text.string) { TextRecognitionScreen(navController = navController) }
                        composable(
                            route = Routes.ArScene.string,
                            arguments = listOf(
                                navArgument("buildingId") { type = NavType.IntType },
                                navArgument("buildingName") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            ArPage(
                                navController = navController,
                                buildingId = backStackEntry.arguments?.getInt("buildingId") ?: 0,
                                buildingName = backStackEntry.arguments?.getString("buildingName") ?: "",
                                arPageViewModel = arPageViewModel,
                                nodeViewModel = nodeViewModel
                            )
                        }
                        
                        composable(
                            route = Routes.ArSceneWithDestination.string,
                            arguments = listOf(
                                navArgument("buildingId") { type = NavType.IntType },
                                navArgument("buildingName") { type = NavType.StringType },
                                navArgument("destinationNodeId") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            ArPage(
                                navController = navController,
                                buildingId = backStackEntry.arguments?.getInt("buildingId") ?: 0,
                                buildingName = backStackEntry.arguments?.getString("buildingName") ?: "",
                                destinationNodeId = backStackEntry.arguments?.getInt("destinationNodeId"),
                                arPageViewModel = arPageViewModel,
                                nodeViewModel = nodeViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}