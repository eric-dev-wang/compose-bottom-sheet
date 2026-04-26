package com.ericdevwang.bottomsheet.mpp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import com.ericdevwang.bottomsheet.androidx.BottomSheetWrapper

@Composable
actual fun BottomSheet(
    onDismissRequest: () -> Unit,
    properties: BottomSheetProperties,
    content: @Composable ColumnScope.() -> Unit,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val composition = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)

    val dialog = remember(view, density) {
        BottomSheetWrapper(
            composeView = view,
            density = density,
            onDismissRequest = onDismissRequest,
            dismissOnBackPress = properties.dismissOnBackPress,
            dismissOnClickOutside = properties.dismissOnClickOutside,
            layoutDirection = layoutDirection,
        ).apply {
            setContent(composition) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .semantics { dialog() },
                    shape = BottomSheetDefaults.ExpandedShape,
                    color = BottomSheetDefaults.ContainerColor,
                    contentColor = BottomSheetDefaults.ContentColor,
                    tonalElevation = BottomSheetDefaults.Elevation,
                ) {
                    Column {
                        currentContent()
                    }
                }
            }
        }
    }

    DisposableEffect(dialog) {
        dialog.show()

        onDispose {
            dialog.captureSnapshotAndDispose()
            dialog.dismissWithExitAnim()
        }
    }

    SideEffect {
        dialog.updateParameters(
            onDismissRequest = onDismissRequest,
            dismissOnBackPress = properties.dismissOnBackPress,
            dismissOnClickOutside = properties.dismissOnClickOutside,
            layoutDirection = layoutDirection,
        )
    }
}
