package com.ericwang.example.bottomsheet

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Outline
import android.os.Build
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.ComponentDialog
import androidx.activity.addCallback
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.core.animation.addListener
import androidx.core.view.WindowCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.drawToBitmap
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import java.util.UUID
import kotlin.math.roundToInt

private const val SCALE_START = 0.8F
private const val ALPHA_START = 0F
private const val ANIMATION_END = 1F
private const val DURATION_ANIMATION = 150L
private const val COLOR_SCRIM = 0xBF0B0B10.toInt()

/**
 * Properties used to customize the behavior of a [Dialog].
 *
 * @property dismissOnBackPress whether the dialog can be dismissed by pressing the back or escape
 *   buttons. If true, pressing the back button will call onDismissRequest.
 * @property dismissOnClickOutside whether the dialog can be dismissed by clicking outside the
 *   dialog's bounds. If true, clicking outside the dialog will call onDismissRequest.
 * @property securePolicy Policy for setting [WindowManager.LayoutParams.FLAG_SECURE] on the
 *   dialog's window.
 * @property decorFitsSystemWindows Sets [WindowCompat.setDecorFitsSystemWindows] value. Set to
 *   `false` to use WindowInsets. If `false`, the
 *   [soft input mode][WindowManager.LayoutParams.softInputMode] will be changed to
 *   [WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE] on [Build.VERSION_CODES.R] and below and
 *   [WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING] on [Build.VERSION_CODES.S] and above.
 *   [Window.isFloating] will be `false` when `decorFitsSystemWindows` is `false`.
 */
@Immutable
class BottomSheetProperties(
    val dismissOnBackPress: Boolean = true,
    val dismissOnClickOutside: Boolean = true,
    val securePolicy: SecureFlagPolicy = SecureFlagPolicy.Inherit,
    val decorFitsSystemWindows: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BottomSheetProperties) return false

        if (dismissOnBackPress != other.dismissOnBackPress) return false
        if (dismissOnClickOutside != other.dismissOnClickOutside) return false
        if (securePolicy != other.securePolicy) return false
        if (decorFitsSystemWindows != other.decorFitsSystemWindows) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dismissOnBackPress.hashCode()
        result = 31 * result + dismissOnClickOutside.hashCode()
        result = 31 * result + securePolicy.hashCode()
        result = 31 * result + decorFitsSystemWindows.hashCode()
        return result
    }
}

@Composable
fun BottomSheet(
    onDismissRequest: () -> Unit,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    border: BorderStroke? = BottomSheetDefaults.BorderStroke,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = BottomSheetDefaults.ContentColor,
    tonalElevation: Dp = BottomSheetDefaults.Elevation,
    properties: BottomSheetProperties = BottomSheetProperties(),
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets },
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val composition = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val dialogId = rememberSaveable { UUID.randomUUID() }
    val darkThemeEnabled = isSystemInDarkTheme()
    val dialog = remember(view, density) {
        BottomSheetWrapper(
            onDismissRequest,
            properties,
            view,
            layoutDirection,
            density,
            dialogId,
            darkThemeEnabled,
        ).apply {
            setContent(composition) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .semantics { dialog() }
                        .windowInsetsPadding(contentWindowInsets()),
                    shape = shape,
                    border = border,
                    color = containerColor,
                    contentColor = contentColor,
                    tonalElevation = tonalElevation,
                ) {
                    currentContent()
                }
            }
        }
    }

    DisposableEffect(dialog) {
        dialog.show()

        onDispose {
            // Capture bitmap snapshot before disposing composition
            // This preserves the visual for exit animation while allowing immediate cleanup
            dialog.captureSnapshotAndDispose()
            dialog.dismissWithExitAnim()
        }
    }

    SideEffect {
        dialog.updateParameters(
            onDismissRequest = onDismissRequest,
            properties = properties,
            layoutDirection = layoutDirection
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private class BottomSheetWrapper(
    private var onDismissRequest: () -> Unit,
    private var properties: BottomSheetProperties,
    private val composeView: View,
    layoutDirection: LayoutDirection,
    density: Density,
    dialogId: UUID,
    darkThemeEnabled: Boolean,
) : ComponentDialog(
    /**
     * [Window.setClipToOutline] is only available from 22+, but the style attribute exists on 21.
     * So use a wrapped context that sets this attribute for compatibility back to 21.
     */
    ContextThemeWrapper(
        composeView.context,
        R.style.BottomSheetFloatingDialogWindowTheme,
    )
) {

    private val container: FrameLayout
    private val dialogLayout: BottomSheetLayout
    private var snapshotImageView: ImageView? = null
    private var snapshotWidth: Int = 0
    private var snapshotHeight: Int = 0
    private var isDismissing = false

    // On systems older than Android S, there is a bug in the surface insets matrix math used by
    // elevation, so high values of maxSupportedElevation break accessibility services: b/232788477.
    private val maxSupportedElevation = 8.dp

    private val defaultSoftInputMode: Int

    init {
        val window = window ?: error("Dialog has no window")
        defaultSoftInputMode =
            window.attributes.softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST
        window.requestFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        @OptIn(ExperimentalComposeUiApi::class)
        WindowCompat.setDecorFitsSystemWindows(window, properties.decorFitsSystemWindows)
        container = FrameLayout(context).apply {
            // Set unique id for AbstractComposeView. This allows state restoration for the state
            // defined inside the Dialog via rememberSaveable()
            setTag(androidx.compose.ui.R.id.compose_view_saveable_id_tag, "Dialog:$dialogId")
            // Enable children to draw their shadow by not clipping them
            clipChildren = false
            // Allocate space for elevation
            with(density) { elevation = maxSupportedElevation.toPx() }
            // Simple outline to force window manager to allocate space for shadow.
            // Note that the outline affects clickable area for the dismiss listener. In case of
            // shapes like circle the area for dismiss might be too small (rectangular outline
            // consuming clicks outside of the circle).
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, result: Outline) {
                    result.setRect(0, 0, view.width, view.height)
                    // We set alpha to 0 to hide the view's shadow and let the composable to draw
                    // its own shadow. This still enables us to get the extra space needed in the
                    // surface.
                    result.alpha = 0f
                }
            }

            var isPressOutside = false
            @SuppressLint("ClickableViewAccessibility")
            setOnTouchListener { _, event ->
                var result = false
                // Ignore touch events if dismiss is already in progress
                if (isDismissing) {
                    return@setOnTouchListener false
                }
                if (properties.dismissOnClickOutside && !dialogLayout.isInsideContent(event)) {
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            isPressOutside = true
                            result = true
                        }

                        MotionEvent.ACTION_UP ->
                            if (isPressOutside) {
                                isDismissing = true
                                onDismissRequest()
                                result = true
                                isPressOutside = false
                            }

                        MotionEvent.ACTION_CANCEL -> isPressOutside = false
                    }
                } else {
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL -> isPressOutside = false
                    }
                }
                result
            }
        }
        dialogLayout = BottomSheetLayout(context)

        /**
         * Disables clipping for [this] and all its descendant [ViewGroup]s until we reach a
         * [BottomSheetLayout] (the [ViewGroup] containing the Compose hierarchy).
         */
        fun ViewGroup.disableClipping() {
            clipChildren = false
            if (this is BottomSheetLayout) return
            for (i in 0 until childCount) {
                (getChildAt(i) as? ViewGroup)?.disableClipping()
            }
        }

        // Turn off all clipping so shadows can be drawn outside the window
        (window.decorView as? ViewGroup)?.disableClipping()

        container.addView(
            dialogLayout,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.BOTTOM,
            )
        )
        setContentView(
            container,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        )
        container.setViewTreeLifecycleOwner(composeView.findViewTreeLifecycleOwner())
        container.setViewTreeViewModelStoreOwner(composeView.findViewTreeViewModelStoreOwner())
        container.setViewTreeSavedStateRegistryOwner(
            composeView.findViewTreeSavedStateRegistryOwner()
        )
        dialogLayout.setViewTreeLifecycleOwner(composeView.findViewTreeLifecycleOwner())
        dialogLayout.setViewTreeViewModelStoreOwner(composeView.findViewTreeViewModelStoreOwner())
        dialogLayout.setViewTreeSavedStateRegistryOwner(
            composeView.findViewTreeSavedStateRegistryOwner()
        )

        // Initial setup
        updateParameters(onDismissRequest, properties, layoutDirection)

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkThemeEnabled
            isAppearanceLightNavigationBars = !darkThemeEnabled
        }

        // Due to how the onDismissRequest callback works
        // (it enforces a just-in-time decision on whether to update the state to hide the dialog)
        // we need to unconditionally add a callback here that is always enabled,
        // meaning we'll never get a system UI controlled predictive back animation
        // for these dialogs
        onBackPressedDispatcher.addCallback(this) {
            if (properties.dismissOnBackPress && !isDismissing) {
                isDismissing = true
                onDismissRequest()
            }
        }
    }

    /**
     * Captures a bitmap snapshot of the dialog content and immediately disposes the composition.
     * This allows proper cleanup of ViewModels and resources while preserving the visual for exit animation.
     */
    fun captureSnapshotAndDispose() {
        // Clean up any existing snapshot first
        cleanupSnapshot()

        try {
            // Only capture if dialogLayout has been laid out and has content
            if (dialogLayout.width > 0 && dialogLayout.height > 0 && dialogLayout.childCount > 0) {
                // Capture the current visual state as a bitmap
                // Use RGB_565 to save memory (50% less than ARGB_8888)
                // Transparency and rounded corners are already rendered in the captured view
                val bitmap = dialogLayout.drawToBitmap(config = Bitmap.Config.RGB_565)

                val width = dialogLayout.width
                val height = dialogLayout.height

                // Save dimensions for later use in animation
                snapshotWidth = width
                snapshotHeight = height

                // Create ImageView to display the snapshot
                snapshotImageView = ImageView(context).apply {
                    setImageBitmap(bitmap)
                    layoutParams = FrameLayout.LayoutParams(
                        width,
                        height
                    ).apply {
                        gravity = android.view.Gravity.BOTTOM
                    }
                    // Set pivot point before adding to container
                    // Pivot should be at bottom center for bottom sheet animation
                    pivotX = width / 2F
                    pivotY = height.toFloat()
                }

                // Hide the original content and show snapshot
                dialogLayout.visibility = View.GONE
                container.addView(snapshotImageView)
            }

            // Now safe to dispose composition - ViewModels will be properly cleaned up
            dialogLayout.disposeComposition()
        } catch (e: Exception) {
            // If snapshot fails, dispose anyway to prevent leaks
            Log.e("BottomSheet", "Failed to capture snapshot", e)
            dialogLayout.disposeComposition()
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (
            properties.dismissOnBackPress &&
            event.isTracking &&
            !event.isCanceled &&
            keyCode == KeyEvent.KEYCODE_ESCAPE &&
            !isDismissing
        ) {
            isDismissing = true
            onDismissRequest()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun setLayoutDirection(layoutDirection: LayoutDirection) {
        container.layoutDirection = when (layoutDirection) {
            LayoutDirection.Ltr -> android.util.LayoutDirection.LTR
            LayoutDirection.Rtl -> android.util.LayoutDirection.RTL
        }
    }

    fun setContent(parentComposition: CompositionContext, children: @Composable () -> Unit) {
        dialogLayout.setContent(parentComposition, children)
    }

    private fun setSecurePolicy(securePolicy: SecureFlagPolicy) {
        val secureFlagEnabled =
            securePolicy.shouldApplySecureFlag(composeView.isFlagSecureEnabled())
        window!!.setFlags(
            if (secureFlagEnabled) {
                WindowManager.LayoutParams.FLAG_SECURE
            } else {
                WindowManager.LayoutParams.FLAG_SECURE.inv()
            },
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    fun updateParameters(
        onDismissRequest: () -> Unit,
        properties: BottomSheetProperties,
        layoutDirection: LayoutDirection
    ) {
        this.onDismissRequest = onDismissRequest
        this.properties = properties
        setSecurePolicy(properties.securePolicy)
        setLayoutDirection(layoutDirection)

        // Window flags to span parent window.
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
        )

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            @OptIn(ExperimentalComposeUiApi::class)
            if (properties.decorFitsSystemWindows) {
                window?.setSoftInputMode(defaultSoftInputMode)
            } else {
                @Suppress("DEPRECATION")
                window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
        }
    }

    override fun cancel() {
        // Prevents the dialog from dismissing itself
        return
    }

    override fun show() {
        super.show()
        // start the enter animation after the layout is ready
        container.doOnPreDraw {
            startEnterAnim()
        }
    }

    private var currentAnimator: Animator? = null

    private fun startEnterAnim() {
        dialogLayout.pivotX = dialogLayout.width / 2F
        dialogLayout.pivotY = dialogLayout.height.toFloat()

        dialogLayout.scaleX = SCALE_START
        dialogLayout.scaleY = SCALE_START
        dialogLayout.alpha = ALPHA_START
        val backgroundAnim = ValueAnimator.ofObject(
            ArgbEvaluator(),
            android.graphics.Color.TRANSPARENT,
            COLOR_SCRIM,
        ).apply {
            addUpdateListener { animator -> container.setBackgroundColor(animator.animatedValue as Int) }
        }
        val contentScaleXAnim = ObjectAnimator.ofFloat(
            dialogLayout,
            "scaleX",
            SCALE_START,
            ANIMATION_END,
        )
        val contentScaleYAnim = ObjectAnimator.ofFloat(
            dialogLayout,
            "scaleY",
            SCALE_START,
            ANIMATION_END,
        )
        val contentAlphaAnim = ObjectAnimator.ofFloat(
            dialogLayout,
            "alpha",
            ALPHA_START,
            ANIMATION_END,
        )

        currentAnimator?.cancel()
        currentAnimator = AnimatorSet().apply {
            playTogether(backgroundAnim, contentScaleXAnim, contentScaleYAnim, contentAlphaAnim)
            setDuration(DURATION_ANIMATION)
            addListener(
                onEnd = { currentAnimator = null },
                onCancel = { currentAnimator = null },
            )
            start()
        }
    }

    fun dismissWithExitAnim() {
        // Use snapshot view for animation if available, otherwise use dialog layout
        val animationTarget = snapshotImageView ?: dialogLayout

        // Use saved dimensions if available (for ImageView before layout), otherwise use measured size
        val targetWidth =
            if (snapshotImageView != null && snapshotWidth > 0) snapshotWidth else animationTarget.width
        val targetHeight =
            if (snapshotImageView != null && snapshotHeight > 0) snapshotHeight else animationTarget.height

        animationTarget.pivotX = targetWidth / 2F
        animationTarget.pivotY = targetHeight.toFloat()

        val backgroundAnim = ValueAnimator.ofObject(
            ArgbEvaluator(),
            COLOR_SCRIM,
            android.graphics.Color.TRANSPARENT,
        ).apply {
            addUpdateListener { animator -> container.setBackgroundColor(animator.animatedValue as Int) }
        }
        val contentScaleXAnim = ObjectAnimator.ofFloat(
            animationTarget,
            "scaleX",
            ANIMATION_END,
            SCALE_START
        )
        val contentScaleYAnim = ObjectAnimator.ofFloat(
            animationTarget,
            "scaleY",
            ANIMATION_END,
            SCALE_START
        )
        val contentAlphaAnim = ObjectAnimator.ofFloat(
            animationTarget,
            "alpha",
            ANIMATION_END,
            ALPHA_START,
        )

        currentAnimator?.cancel()
        currentAnimator = AnimatorSet().apply {
            playTogether(backgroundAnim, contentScaleXAnim, contentScaleYAnim, contentAlphaAnim)
            setDuration(DURATION_ANIMATION)
            addListener(
                onEnd = {
                    currentAnimator = null
                    isDismissing = false
                    cleanupSnapshot()
                    dismiss()
                },
                onCancel = {
                    currentAnimator = null
                    isDismissing = false
                    cleanupSnapshot()
                    dismiss()
                },
            )
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        clearAnimation()
        cleanupSnapshot()
    }

    private fun clearAnimation() {
        currentAnimator?.cancel()
        currentAnimator = null
    }

    private fun cleanupSnapshot() {
        snapshotImageView?.let { imageView ->
            // Recycle bitmap to free memory
            (imageView.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap?.let { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
            container.removeView(imageView)
            snapshotImageView = null
        }
        snapshotWidth = 0
        snapshotHeight = 0
    }
}

@Suppress("ViewConstructor")
private class BottomSheetLayout(context: Context) : AbstractComposeView(context) {

    private var content: @Composable () -> Unit by mutableStateOf({})

    override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    fun setContent(parent: CompositionContext, content: @Composable () -> Unit) {
        setParentCompositionContext(parent)
        this.content = content
        shouldCreateCompositionOnAttachedToWindow = true
        createComposition()
    }

    @Composable
    override fun Content() {
        content()
    }

    fun isInsideContent(event: MotionEvent): Boolean {
        if (!event.x.isFinite() || !event.y.isFinite()) return false
        val child = getChildAt(0) ?: return false
        val left = left + child.left
        val right = left + child.width
        val top = top + child.top
        val bottom = top + child.height
        return event.x.roundToInt() in left..right && event.y.roundToInt() in top..bottom
    }

}

// Taken from AndroidPopup.android.kt
private fun View.isFlagSecureEnabled(): Boolean {
    val windowParams = rootView.layoutParams as? WindowManager.LayoutParams
    if (windowParams != null) {
        return (windowParams.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
    }
    return false
}

// Taken from AndroidPopup.android.kt
private fun SecureFlagPolicy.shouldApplySecureFlag(isSecureFlagSetOnParent: Boolean): Boolean {
    return when (this) {
        SecureFlagPolicy.SecureOff -> false
        SecureFlagPolicy.SecureOn -> true
        SecureFlagPolicy.Inherit -> isSecureFlagSetOnParent
    }
}
