package com.ericwang.example.bottomsheet

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

/** An [OverlayScene] that renders an [entry] within a [BottomSheet]. */
internal class BottomSheetScene<T : Any>(
    override val key: Any,
    private val entry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val bottomSheetProperties: BottomSheetProperties,
    private val onBack: () -> Unit,
) : OverlayScene<T> {

    override val entries: List<NavEntry<T>> = listOf(entry)

    override val content: @Composable (() -> Unit) = {
        BottomSheet(
            onDismissRequest = onBack,
            properties = bottomSheetProperties
        ) { entry.Content() }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BottomSheetScene<*>) return false

        if (key != other.key) return false
        if (entry != other.entry) return false
        if (previousEntries != other.previousEntries) return false
        if (overlaidEntries != other.overlaidEntries) return false
        if (bottomSheetProperties != other.bottomSheetProperties) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + entry.hashCode()
        result = 31 * result + previousEntries.hashCode()
        result = 31 * result + overlaidEntries.hashCode()
        result = 31 * result + bottomSheetProperties.hashCode()
        return result
    }

    override fun toString(): String {
        return """BottomSheetScene(
            |key=$key, 
            |entry=$entry, 
            |previousEntries=$previousEntries, 
            |overlaidEntries=$overlaidEntries, 
            |bottomSheetProperties=$bottomSheetProperties
            |)"""
            .trimMargin()
            .replace("\n", "")
    }
}

/**
 * A [SceneStrategy] that displays entries that have added [bottom sheet] to their [NavEntry.metadata]
 * within a [BottomSheet] instance.
 *
 * This strategy should always be added before any non-overlay scene strategies.
 */

public class BottomSheetSceneStrategy<T : Any> : SceneStrategy<T> {

    public override fun SceneStrategyScope<T>.calculateScene(
        entries: List<NavEntry<T>>
    ): Scene<T>? {
        val lastEntry = entries.lastOrNull()
        val bottomSheetProperties =
            lastEntry?.metadata?.get(BOTTOM_SHEET_KEY) as? BottomSheetProperties
        return bottomSheetProperties?.let { properties ->
            BottomSheetScene(
                key = lastEntry.contentKey,
                entry = lastEntry,
                previousEntries = entries.dropLast(1),
                overlaidEntries = entries.dropLast(1),
                bottomSheetProperties = properties,
                onBack = onBack,
            )
        }
    }

    public companion object {
        /**
         * Function to be called on the [NavEntry.metadata] to mark this entry as something that
         * should be displayed within a [BottomSheet].
         *
         * @param properties properties that should be passed to the containing
         * [BottomSheet].
         */
        public fun bottomSheet(
            properties: BottomSheetProperties = BottomSheetProperties(),
        ): Map<String, Any> = mapOf(BOTTOM_SHEET_KEY to properties)

        internal const val BOTTOM_SHEET_KEY = "bottom_sheet"
    }
}
