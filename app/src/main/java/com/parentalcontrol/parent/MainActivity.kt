package com.parentalcontrol.parent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.parentalcontrol.parent.ui.screens.ChildDetailScreen
import com.parentalcontrol.parent.ui.screens.ChildListScreen
import com.parentalcontrol.parent.ui.screens.PairingScreen
import com.parentalcontrol.parent.ui.theme.ParentAppTheme
import com.parentalcontrol.parent.viewmodel.ParentDashboardViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ParentAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ParentAppNavHost()
                }
            }
        }
    }
}

private object Routes {
    const val LIST = "list"
    const val PAIRING = "pairing"
    const val DETAIL = "detail/{childId}"
    fun detail(childId: String) = "detail/$childId"
}

@Composable
fun ParentAppNavHost(viewModel: ParentDashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val navController = rememberNavController()
    val children by viewModel.children.collectAsState()
    val pairingState by viewModel.pairingState.collectAsState()
    val selectedChild by viewModel.selectedChild.collectAsState()

    NavHost(navController = navController, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            ChildListScreen(
                children = children,
                onChildClick = { child ->
                    viewModel.selectChild(child.childId)
                    navController.navigate(Routes.detail(child.childId))
                },
                onAddChildClick = { navController.navigate(Routes.PAIRING) }
            )
        }
        composable(Routes.PAIRING) {
            PairingScreen(
                pairingState = pairingState,
                onStartPairing = { viewModel.startPairing() },
                onDone = {
                    viewModel.resetPairingState()
                    navController.popBackStack()
                },
                onBack = {
                    viewModel.resetPairingState()
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            val child = selectedChild
            if (child != null && child.childId == childId) {
                ChildDetailScreen(
                    child = child,
                    onBack = {
                        viewModel.clearSelectedChild()
                        navController.popBackStack()
                    },
                    onToggleLock = { viewModel.toggleLock(childId, child.status == "locked") },
                    onSetLimit = { minutes -> viewModel.setDailyLimit(childId, minutes) },
                    onToggleAppBlock = { pkg, blocked -> viewModel.toggleAppBlock(childId, pkg, blocked) },
                    onRequestLiveScreen = { viewModel.requestLiveScreen(childId) },
                    onStopLiveScreen = { viewModel.stopLiveScreen(childId) },
                    onRemoveChild = {
                        viewModel.removeChild(childId)
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
