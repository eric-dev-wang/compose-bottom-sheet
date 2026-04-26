@file:OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalForeignApi::class,
)

package com.ericdevwang.bottomsheet.mpp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import swiftPMImport.BottomSheet.bottomsheet.mpp.BottomSheetController

@Composable
actual fun BottomSheet(
    onDismissRequest: () -> Unit,
    properties: BottomSheetProperties,
    content: @Composable ColumnScope.() -> Unit,
) {
    val uiViewController = LocalUIViewController.current
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val currentContent by rememberUpdatedState(content)
    val currentDismissOnClickOutside by rememberUpdatedState(properties.dismissOnClickOutside)

    // Create content VC and sheet controller once
    val controller = remember {
        val contentVC = ComposeUIViewController(
            configure = {
                opaque = false
            },
        ) {
            val scrimInteractionSource = remember { MutableInteractionSource() }
            val sheetInteractionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (currentDismissOnClickOutside) {
                            Modifier.clickable(
                                interactionSource = scrimInteractionSource,
                                indication = null,
                                onClick = currentOnDismissRequest,
                            )
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .clickable(
                            interactionSource = sheetInteractionSource,
                            indication = null,
                            onClick = {},
                        ),
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
        BottomSheetController(contentViewController = contentVC)
    }

    // Set up dismiss callback - calls onDismissRequest which updates showBottomSheet state
    LaunchedEffect(controller) {
        controller.onDismissHandler = {
            currentOnDismissRequest()
        }
    }

    // Present once, dismiss with animation on removal
    DisposableEffect(controller) {
        if (controller.presentingViewController == null) {
            uiViewController.presentViewController(controller, animated = true, completion = null)
        }

        onDispose {
            // Dismiss with animation when the Composable leaves the tree
            // The state/back stack already changed before disposal, so avoid calling
            // onDismissRequest again from the UIKit dismiss completion.
            if (controller.view.window != null) {
                controller.onDismissHandler = null
                controller.dismissViewControllerAnimated(true, completion = null)
            }
        }
    }
}
