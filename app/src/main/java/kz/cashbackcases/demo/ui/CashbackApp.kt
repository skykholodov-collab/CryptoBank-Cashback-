@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package kz.cashbackcases.demo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kz.cashbackcases.demo.R
import kz.cashbackcases.demo.ui.components.DemoBanner
import kz.cashbackcases.demo.ui.screens.BalanceScreen
import kz.cashbackcases.demo.ui.screens.CatalogScreen
import kz.cashbackcases.demo.ui.screens.CollectionScreen
import kz.cashbackcases.demo.ui.screens.OfferDetailScreen
import kz.cashbackcases.demo.ui.theme.Cyan
import kz.cashbackcases.demo.ui.theme.Ink
import kz.cashbackcases.demo.ui.theme.InkRaised
import kz.cashbackcases.demo.ui.theme.Muted
import kz.cashbackcases.demo.ui.theme.Text

private object Routes {
    const val CATALOG = "catalog"
    const val BALANCE = "balance"
    const val COLLECTION = "collection/{id}"
    const val OFFER = "offer/{id}"

    fun collection(id: String) = "collection/$id"
    fun offer(id: String) = "offer/$id"
}

@Composable
fun CashbackApp(viewModel: CashbackViewModel) {
    val navController = rememberNavController()
    val snackbars = remember { SnackbarHostState() }
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val balanceSelected = route == Routes.BALANCE
    val casesSelected = !balanceSelected
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbars.showSnackbar(message)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF121826), Ink, Color(0xFF090C12)),
                ),
            ),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(containerColor = InkRaised) {
                    NavigationBarItem(
                        selected = casesSelected,
                        onClick = {
                            navController.navigate(Routes.CATALOG) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_cases)) },
                        modifier = Modifier.testTag(Tags.NAV_CASES),
                        colors = navColors(),
                    )
                    NavigationBarItem(
                        selected = balanceSelected,
                        onClick = {
                            navController.navigate(Routes.BALANCE) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_balance)) },
                        modifier = Modifier.testTag(Tags.NAV_BALANCE),
                        colors = navColors(),
                    )
                }
            },
            snackbarHost = {
                SnackbarHost(snackbars) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = InkRaised,
                        contentColor = Text,
                    )
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
            DemoBanner()
            NavHost(
                navController = navController,
                startDestination = Routes.CATALOG,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
            ) {
                composable(Routes.CATALOG) {
                    CatalogScreen(onOpen = { id -> navController.navigate(Routes.collection(id)) })
                }
                composable(
                    route = Routes.COLLECTION,
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                ) { entry ->
                    CollectionScreen(
                        collectionId = entry.arguments?.getString("id").orEmpty(),
                        onBack = { navController.popBackStack() },
                        onOffer = { id -> navController.navigate(Routes.offer(id)) },
                    )
                }
                composable(
                    route = Routes.OFFER,
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                ) { entry ->
                    OfferDetailScreen(
                        offerId = entry.arguments?.getString("id").orEmpty(),
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onOpenBalance = {
                            navController.navigate(Routes.BALANCE) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
                composable(Routes.BALANCE) {
                    BalanceScreen(viewModel = viewModel)
                }
            }
            }
        }
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Cyan,
    selectedTextColor = Cyan,
    unselectedIconColor = Muted,
    unselectedTextColor = Muted,
    indicatorColor = Color(0xFF143834),
)
