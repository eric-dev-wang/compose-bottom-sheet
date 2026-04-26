package com.ericdevwang.bottomsheet.mpp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
object BottomSheetDefaults {

    /** The default shape for a bottom sheets in and Expanded states. */
    val ExpandedShape: Shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

    val BorderStroke: BorderStroke
        @Composable get() = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

    /** The default container color for a bottom sheet. */
    val ContainerColor: Color
        @Composable get() = BottomSheetDefaults.ContainerColor

    val ContentColor: Color
        @Composable get() = contentColorFor(ContainerColor)

    /** The default elevation for a bottom sheet. */
    val Elevation = 0.dp

    /** The default color of the scrim overlay for background content. */
    val ScrimColor: Color
        @Composable get() = BottomSheetDefaults.ScrimColor

    val windowInsets: WindowInsets
        @Composable get() = WindowInsets()
}
