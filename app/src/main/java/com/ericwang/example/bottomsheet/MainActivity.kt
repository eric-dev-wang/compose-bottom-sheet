package com.ericwang.example.bottomsheet

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.ericwang.example.bottomsheet.ui.theme.BottomSheetTheme
import kotlinx.serialization.Serializable

@Serializable
data object HomeEntry : NavKey

@Serializable
data object BottomSheetEntry : NavKey

@Serializable
data object BottomSheetWithViewModelEntry : NavKey

@Serializable
data object BottomSheetWithoutBackGestureAndClickOutsideDismissEntry : NavKey

@Serializable
data object BottomSheetWithoutClickOutsideDismissEntry : NavKey

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkTheme = isSystemInDarkTheme()
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        DefaultLightScrim,
                        DefaultDarkScrim,
                    ) { darkTheme },
                )
                onDispose {}
            }
            BottomSheetTheme {
                val backStack = rememberNavBackStack(HomeEntry)
                NavDisplay(
                    backStack = backStack,
                    sceneStrategy = BottomSheetSceneStrategy(),
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    entryProvider = entryProvider {
                        entry<HomeEntry> {
                            HomeScreen(
                                onNavigateToBottomSheet = {
                                    backStack.add(BottomSheetEntry)
                                },
                                onNavigateToBottomSheetWithViewModel = {
                                    backStack.add(BottomSheetWithViewModelEntry)
                                },
                                onNavigateToBottomSheetWithoutBackGestureAndClickOutside = {
                                    backStack.add(
                                        BottomSheetWithoutBackGestureAndClickOutsideDismissEntry
                                    )
                                },
                                onNavigateToBottomSheetWithoutClickOutside = {
                                    backStack.add(
                                        BottomSheetWithoutClickOutsideDismissEntry
                                    )
                                },
                            )
                        }

                        entry<BottomSheetEntry>(
                            metadata = BottomSheetSceneStrategy.bottomSheet()
                        ) {
                            BottomSheetContent(
                                title = "Bottom Sheet",
                                onBackClick = {
                                    if (backStack.lastOrNull() == BottomSheetEntry) {
                                        backStack.removeLastOrNull()
                                    }
                                },
                            )
                        }

                        entry<BottomSheetWithViewModelEntry>(
                            metadata = BottomSheetSceneStrategy.bottomSheet()
                        ) {
                            BottomSheetNavScreen(
                                title = "Bottom Sheet with ViewModel",
                                onBackClick = {
                                    if (backStack.lastOrNull() == BottomSheetWithViewModelEntry) {
                                        backStack.removeLastOrNull()
                                    }
                                },
                            )
                        }

                        entry<BottomSheetWithoutBackGestureAndClickOutsideDismissEntry>(
                            metadata = BottomSheetSceneStrategy.bottomSheet(
                                properties = BottomSheetProperties(
                                    dismissOnBackPress = false,
                                    dismissOnClickOutside = false,
                                )
                            )
                        ) {
                            BottomSheetContent(
                                title = "Bottom Sheet without Back Gesture & Click Outside Dismiss",
                                onBackClick = {
                                    if (backStack.lastOrNull() == BottomSheetWithoutBackGestureAndClickOutsideDismissEntry) {
                                        backStack.removeLastOrNull()
                                    }
                                },
                            )
                        }

                        entry<BottomSheetWithoutClickOutsideDismissEntry>(
                            metadata = BottomSheetSceneStrategy.bottomSheet(
                                properties = BottomSheetProperties(
                                    dismissOnClickOutside = false,
                                )
                            )
                        ) {
                            BackHandler {
                                Log.d(
                                    "MainActivity",
                                    "Back pressed in BottomSheetWithoutClickOutsideDismissEntry",
                                )
                                if (backStack.lastOrNull() == BottomSheetWithoutClickOutsideDismissEntry) {
                                    backStack.removeLastOrNull()
                                }
                            }
                            BottomSheetContent(
                                title = "Bottom Sheet without Click Outside Dismiss",
                                onBackClick = {
                                    if (backStack.lastOrNull() == BottomSheetWithoutClickOutsideDismissEntry) {
                                        backStack.removeLastOrNull()
                                    }
                                },
                            )
                        }
                    })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToBottomSheet: () -> Unit,
    onNavigateToBottomSheetWithViewModel: () -> Unit,
    onNavigateToBottomSheetWithoutBackGestureAndClickOutside: () -> Unit,
    onNavigateToBottomSheetWithoutClickOutside: () -> Unit,
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var showBottomSheetWithoutBackGestureAndClickOutside by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Home") }
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(horizontal = 16.dp),
        ) {
            Button(
                onClick = { showBottomSheet = true },
            ) {
                Text(text = "Show Bottom Sheet")
            }

            Button(
                onClick = { showBottomSheetWithoutBackGestureAndClickOutside = true },
            ) {
                Text(text = "Show Bottom Sheet (No Back Gesture & Click Outside Dismiss)")
            }

            Button(
                onClick = onNavigateToBottomSheet,
            ) {
                Text(text = "Go to Bottom Sheet")
            }

            Button(
                onClick = onNavigateToBottomSheetWithViewModel,
            ) {
                Text(text = "Go to Bottom Sheet with ViewModel")
            }

            Button(
                onClick = onNavigateToBottomSheetWithoutBackGestureAndClickOutside,
            ) {
                Text(text = "Go to Bottom Sheet without Back Gesture & Click Outside Dismiss")
            }

            Button(
                onClick = onNavigateToBottomSheetWithoutClickOutside,
            ) {
                Text(text = "Go to Bottom Sheet without Click Outside Dismiss")
            }
        }
    }
    if (showBottomSheet) {
        BottomSheet(
            onDismissRequest = { showBottomSheet = false },
        ) {
            BottomSheetContent(
                title = "Bottom Sheet",
                onBackClick = { showBottomSheet = false }
            )
        }
    }
    if (showBottomSheetWithoutBackGestureAndClickOutside) {
        BottomSheet(
            onDismissRequest = { showBottomSheetWithoutBackGestureAndClickOutside = false },
            properties = BottomSheetProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            )
        ) {
            BottomSheetContent(
                title = "Bottom Sheet without Back Gesture & Click Outside Dismiss",
                onBackClick = { showBottomSheetWithoutBackGestureAndClickOutside = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetContent(
    title: String,
    onBackClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CenterAlignedTopAppBar(
                title = { Text(text = title) },
                windowInsets = WindowInsets(),
            )

            Text(
                text = "This is a bottom sheet screen.",
                modifier = Modifier.padding(16.dp)
            )
            Button(
                onClick = onBackClick,
                modifier = Modifier.padding(horizontal = 48.dp)
            ) {
                Text(text = "Go Back")
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
        }
    }
}

@Composable
fun BottomSheetNavScreen(
    title: String,
    onBackClick: () -> Unit
) {
    val viewModel: BottomSheetViewModel = viewModel()
    BottomSheetContent(
        title = title,
        onBackClick = onBackClick,
    )
}

class BottomSheetViewModel : ViewModel() {
    companion object {
        private const val TAG = "BottomSheetViewModel"
    }

    init {
        Log.d(TAG, "Initialized")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "Cleared")
    }
}

// The light scrim color used in the platform API 29+
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/com/android/internal/policy/DecorView.java;drc=6ef0f022c333385dba2c294e35b8de544455bf19;l=142
val DefaultLightScrim = android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF)

// The dark scrim color used in the platform.
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/res/res/color/system_bar_background_semi_transparent.xml
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/res/remote_color_resources_res/values/colors.xml;l=67
val DefaultDarkScrim = android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)
