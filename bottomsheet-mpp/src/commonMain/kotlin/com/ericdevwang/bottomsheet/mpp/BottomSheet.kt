package com.ericdevwang.bottomsheet.mpp

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * Properties used to customize the behavior of a [BottomSheet].
 *
 * @property dismissOnBackPress whether the bottom sheet can be dismissed by pressing the back
 *   button. If true, pressing the back button will call onDismissRequest.
 * @property dismissOnClickOutside whether the bottom sheet can be dismissed by clicking outside the
 *   bottom sheet's bounds. If true, clicking outside the bottom sheet will call onDismissRequest.
 */
@Immutable
class BottomSheetProperties(
    val dismissOnBackPress: Boolean = true,
    val dismissOnClickOutside: Boolean = true,
) {
    companion object {
        /**
         * Default [BottomSheetProperties], with dismiss on back press and click outside enabled.
         */
        val Default = BottomSheetProperties()

        /**
         * [BottomSheetProperties] that cannot be dismissed by back button press or clicks outside.
         */
        val NonDismissible = BottomSheetProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BottomSheetProperties) return false

        if (dismissOnBackPress != other.dismissOnBackPress) return false
        if (dismissOnClickOutside != other.dismissOnClickOutside) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dismissOnBackPress.hashCode()
        result = 31 * result + dismissOnClickOutside.hashCode()
        return result
    }
}

/**
 * A bottom sheet that presents content from the bottom of the screen.
 *
 * @param onDismissRequest executed when the user clicks outside of the bottom sheet,
 *   or when the back button is pressed
 * @param properties properties for the bottom sheet's behavior
 * @param content the content of the bottom sheet
 */
@Composable
expect fun BottomSheet(
    onDismissRequest: () -> Unit,
    properties: BottomSheetProperties = BottomSheetProperties.Default,
    content: @Composable ColumnScope.() -> Unit,
)
