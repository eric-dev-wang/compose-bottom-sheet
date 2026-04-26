package com.ericdevwang.bottomsheet.androidx

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.BitmapDrawable
import android.view.ContextThemeWrapper
import android.view.Gravity
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.animation.addListener
import androidx.core.view.doOnPreDraw
import androidx.core.view.drawToBitmap
import kotlin.math.roundToInt

private const val SCALE_START = 0.8F
private const val ALPHA_START = 0F
private const val ANIMATION_END = 1F
private const val DURATION_ANIMATION = 150L
private const val COLOR_SCRIM = 0xBF0B0B10.toInt()

class BottomSheetWrapper(
    private val composeView: View,
    density: Density,
    private var onDismissRequest: () -> Unit,
    private var dismissOnBackPress: Boolean,
    private var dismissOnClickOutside: Boolean,
    layoutDirection: LayoutDirection,
) : ComponentDialog(
    ContextThemeWrapper(
        composeView.context,
        R.style.BottomSheetFloatingDialogWindowTheme
    )
) {

    private val container: FrameLayout
    private val dialogLayout: BottomSheetLayout
    private var snapshotImageView: ImageView? = null
    private var snapshotWidth: Int = 0
    private var snapshotHeight: Int = 0
    private var isDismissing = false
    private var currentAnimator: Animator? = null

    init {
        val window = window ?: error("Dialog has no window")
        window.requestFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        container = FrameLayout(context).apply {
            clipChildren = false
            with(density) { elevation = 8.dp.toPx() }
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, result: Outline) {
                    result.setRect(0, 0, view.width, view.height)
                    result.alpha = 0f
                }
            }

            var isPressOutside = false
            setOnTouchListener { _, event ->
                if (isDismissing) return@setOnTouchListener false
                var consumed = false
                if (dismissOnClickOutside && !dialogLayout.isInsideContent(event)) {
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            isPressOutside = true
                            consumed = true
                        }

                        MotionEvent.ACTION_UP -> {
                            if (isPressOutside) {
                                isDismissing = true
                                onDismissRequest()
                                consumed = true
                            }
                            isPressOutside = false
                        }

                        MotionEvent.ACTION_CANCEL -> isPressOutside = false
                    }
                } else if (
                    event.actionMasked == MotionEvent.ACTION_DOWN ||
                    event.actionMasked == MotionEvent.ACTION_UP ||
                    event.actionMasked == MotionEvent.ACTION_CANCEL
                ) {
                    isPressOutside = false
                }
                consumed
            }
        }

        dialogLayout = BottomSheetLayout(context)

        container.addView(
            dialogLayout,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM,
            ),
        )

        setContentView(
            container,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        updateParameters(
            onDismissRequest = onDismissRequest,
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
            layoutDirection = layoutDirection,
        )

        onBackPressedDispatcher.addCallback(this) {
            if (dismissOnBackPress && !isDismissing) {
                isDismissing = true
                onDismissRequest()
            }
        }
    }

    fun setContent(parentComposition: CompositionContext, children: @Composable () -> Unit) {
        dialogLayout.setContent(parentComposition, children)
    }

    fun updateParameters(
        onDismissRequest: () -> Unit,
        dismissOnBackPress: Boolean,
        dismissOnClickOutside: Boolean,
        layoutDirection: LayoutDirection,
    ) {
        this.onDismissRequest = onDismissRequest
        this.dismissOnBackPress = dismissOnBackPress
        this.dismissOnClickOutside = dismissOnClickOutside

        container.layoutDirection = when (layoutDirection) {
            LayoutDirection.Ltr -> android.util.LayoutDirection.LTR
            LayoutDirection.Rtl -> android.util.LayoutDirection.RTL
        }

        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
        )
    }

    override fun show() {
        super.show()
        container.doOnPreDraw {
            startEnterAnim()
        }
    }

    fun captureSnapshotAndDispose() {
        cleanupSnapshot()

        if (dialogLayout.width > 0 && dialogLayout.height > 0 && dialogLayout.childCount > 0) {
            val bitmap = dialogLayout.drawToBitmap(config = Bitmap.Config.RGB_565)
            snapshotWidth = dialogLayout.width
            snapshotHeight = dialogLayout.height
            snapshotImageView = ImageView(context).apply {
                setImageBitmap(bitmap)
                layoutParams = FrameLayout.LayoutParams(snapshotWidth, snapshotHeight).apply {
                    gravity = Gravity.BOTTOM
                }
                pivotX = snapshotWidth / 2F
                pivotY = snapshotHeight.toFloat()
            }
            dialogLayout.visibility = View.GONE
            container.addView(snapshotImageView)
        }

        dialogLayout.disposeComposition()
    }

    fun dismissWithExitAnim() {
        val animationTarget = snapshotImageView ?: dialogLayout
        val targetWidth =
            if (snapshotImageView != null && snapshotWidth > 0) snapshotWidth else animationTarget.width
        val targetHeight =
            if (snapshotImageView != null && snapshotHeight > 0) snapshotHeight else animationTarget.height

        animationTarget.pivotX = targetWidth / 2F
        animationTarget.pivotY = targetHeight.toFloat()

        val backgroundAnim = ValueAnimator.ofObject(
            ArgbEvaluator(),
            COLOR_SCRIM,
            Color.TRANSPARENT,
        ).apply {
            addUpdateListener { animator -> container.setBackgroundColor(animator.animatedValue as Int) }
        }
        val scaleXAnim =
            ObjectAnimator.ofFloat(animationTarget, "scaleX", ANIMATION_END, SCALE_START)
        val scaleYAnim =
            ObjectAnimator.ofFloat(animationTarget, "scaleY", ANIMATION_END, SCALE_START)
        val alphaAnim = ObjectAnimator.ofFloat(animationTarget, "alpha", ANIMATION_END, ALPHA_START)

        currentAnimator?.cancel()
        currentAnimator = AnimatorSet().apply {
            playTogether(backgroundAnim, scaleXAnim, scaleYAnim, alphaAnim)
            duration = DURATION_ANIMATION
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

    private fun startEnterAnim() {
        dialogLayout.pivotX = dialogLayout.width / 2F
        dialogLayout.pivotY = dialogLayout.height.toFloat()

        dialogLayout.scaleX = SCALE_START
        dialogLayout.scaleY = SCALE_START
        dialogLayout.alpha = ALPHA_START

        val backgroundAnim = ValueAnimator.ofObject(
            ArgbEvaluator(),
            Color.TRANSPARENT,
            COLOR_SCRIM,
        ).apply {
            addUpdateListener { animator -> container.setBackgroundColor(animator.animatedValue as Int) }
        }

        val scaleXAnim = ObjectAnimator.ofFloat(dialogLayout, "scaleX", SCALE_START, ANIMATION_END)
        val scaleYAnim = ObjectAnimator.ofFloat(dialogLayout, "scaleY", SCALE_START, ANIMATION_END)
        val alphaAnim = ObjectAnimator.ofFloat(dialogLayout, "alpha", ALPHA_START, ANIMATION_END)

        currentAnimator?.cancel()
        currentAnimator = AnimatorSet().apply {
            playTogether(backgroundAnim, scaleXAnim, scaleYAnim, alphaAnim)
            duration = DURATION_ANIMATION
            addListener(onEnd = { currentAnimator = null }, onCancel = { currentAnimator = null })
            start()
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ESCAPE && dismissOnBackPress && !isDismissing) {
            isDismissing = true
            onDismissRequest()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun cancel() {
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        currentAnimator?.cancel()
        currentAnimator = null
        cleanupSnapshot()
    }

    private fun cleanupSnapshot() {
        snapshotImageView?.let { imageView ->
            (imageView.drawable as? BitmapDrawable)?.bitmap?.let { bmp ->
                if (!bmp.isRecycled) bmp.recycle()
            }
            container.removeView(imageView)
            snapshotImageView = null
        }
        snapshotWidth = 0
        snapshotHeight = 0
    }
}

@Suppress("ViewConstructor")
private class BottomSheetLayout(context: Context) :
    AbstractComposeView(context) {
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
