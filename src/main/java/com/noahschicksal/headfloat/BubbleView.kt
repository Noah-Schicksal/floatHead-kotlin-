package com.noahschicksal.headfloat

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import kotlin.math.hypot

class BubbleView(context: Context) : FrameLayout(context) {

    private var windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val bubbleIcon: ImageView = ImageView(context)
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isDragging = false
    private var lastX = 0f
    private var lastY = 0f

    private var dragEnabled = true
    private var magnetEnabled = true

    private var onBubbleClickListener: (() -> Unit)? = null
    private var onDropListener: ((x: Int, y: Int) -> Unit)? = null

    private var sizeDp: Int = 56

    private var trashView: TrashView? = null
    private val attractDistance = 200 // px de proximidade para atração
    private val sideMarginPx = 30     // margem da bolha em px das laterais

    // Mini tela
    private var miniScreenView: FrameLayout? = null
    private var miniScreenOpen = false
    private var fragmentManager: FragmentManager? = null
    private var fragmentToShow: Fragment? = null
    private var activityContext: FragmentActivity? = null

    init {
        bubbleIcon.layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        addView(bubbleIcon)

        setOnClickListener {
            if (!isDragging) toggleMiniScreen()
        }

        setOnTouchListener(createTouchListener())
    }

    fun setFragment(activity: FragmentActivity, fragment: Fragment) {
        activityContext = activity
        fragmentManager = activity.supportFragmentManager
        fragmentToShow = fragment
    }

    fun setIcon(drawable: Drawable?) {
        bubbleIcon.setImageDrawable(drawable)
    }

    fun setOnBubbleClickListener(listener: () -> Unit) {
        onBubbleClickListener = listener
    }

    fun setSizeDp(dp: Int) {
        sizeDp = dp
        val sizePx = dpToPx(dp)
        layoutParams?.width = sizePx
        layoutParams?.height = sizePx
        layoutParams?.let { windowManager.updateViewLayout(this, it) }
    }

    fun getSizePx(): Int = dpToPx(sizeDp)

    fun enableDrag(enable: Boolean) {
        dragEnabled = enable
    }

    fun enableMagnet(enable: Boolean) {
        magnetEnabled = enable
    }

    fun setOnDropListener(listener: (x: Int, y: Int) -> Unit) {
        onDropListener = listener
    }

    fun setTrashView(trash: TrashView) {
        trashView = trash
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    fun toggleMiniScreen() {
        if (fragmentManager == null || fragmentToShow == null) return

        if (miniScreenOpen) {
            miniScreenView?.let {
                windowManager.removeView(it)
                miniScreenView = null
            }
            miniScreenOpen = false
        } else {
            miniScreenView = FrameLayout(context).apply {
                id = View.generateViewId()
                layoutParams = LayoutParams(
                    dpToPx(250),
                    dpToPx(300)
                )
            }

            val params = WindowManager.LayoutParams(
                dpToPx(250),
                dpToPx(300),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = (layoutParams?.x ?: 0) + getSizePx() + dpToPx(10)
                y = layoutParams?.y ?: 0
            }

            windowManager.addView(miniScreenView, params)

            fragmentManager?.beginTransaction()?.replace(miniScreenView!!.id, fragmentToShow!!)?.commit()
            miniScreenOpen = true
        }
    }

    private fun createTouchListener(): OnTouchListener {
        return OnTouchListener { _, event ->
            layoutParams ?: return@OnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = false
                    lastX = event.rawX
                    lastY = event.rawY
                    trashView?.show()
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!dragEnabled) return@OnTouchListener true

                    isDragging = true
                    var deltaX = (event.rawX - lastX).toInt()
                    var deltaY = (event.rawY - lastY).toInt()

                    layoutParams?.let { params ->
                        params.x += deltaX
                        params.y += deltaY

                        // Atração para TrashView
                        trashView?.let { trash ->
                            val bubbleCenterX = params.x + getSizePx() / 2
                            val bubbleCenterY = params.y + getSizePx() / 2
                            val (trashCenterX, trashCenterY) = trash.getCenterPosition()

                            val distance = hypot(
                                (bubbleCenterX - trashCenterX).toDouble(),
                                (bubbleCenterY - trashCenterY).toDouble()
                            )

                            if (distance < attractDistance && distance > 20) {
                                val attractSpeed = 0.05
                                params.x += ((trashCenterX - bubbleCenterX) * attractSpeed).toInt()
                                params.y += ((trashCenterY - bubbleCenterY) * attractSpeed).toInt()
                            }
                        }

                        windowManager.updateViewLayout(this, params)
                    }

                    lastX = event.rawX
                    lastY = event.rawY
                }
                MotionEvent.ACTION_UP -> {
                    layoutParams?.let { params ->
                        var removed = false
                        // Prioriza TrashView
                        trashView?.let { trash ->
                            val bubbleCenterX = params.x + getSizePx() / 2
                            val bubbleCenterY = params.y + getSizePx() / 2
                            if (trash.isPointInside(bubbleCenterX, bubbleCenterY)) {
                                detachFromWindow()
                                trash.hide()
                                miniScreenView?.let { windowManager.removeView(it) }
                                miniScreenOpen = false
                                removed = true
                                HeadFloatLogger.d("Bubble removed via TrashView")
                            }
                        }

                        if (!removed) {
                            trashView?.hide()
                            // Grudar lateral
                            val screenWidth = resources.displayMetrics.widthPixels
                            val targetX = if (params.x + getSizePx() / 2 < screenWidth / 2)
                                sideMarginPx
                            else
                                screenWidth - getSizePx() - sideMarginPx

                            val animator = ValueAnimator.ofInt(params.x, targetX)
                            animator.duration = 250
                            animator.addUpdateListener { valueAnimator ->
                                layoutParams?.let {
                                    it.x = valueAnimator.animatedValue as Int
                                    windowManager.updateViewLayout(this, it)
                                }
                            }
                            animator.start()

                            val centerX = targetX + getSizePx() / 2
                            val centerY = params.y + getSizePx() / 2
                            onDropListener?.invoke(centerX, centerY)
                        }
                    }
                }
            }
            true
        }
    }

    fun attachToWindow(initialX: Int = 100, initialY: Int = 100) {
        if (layoutParams != null) return

        val sizePx = getSizePx()
        layoutParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
        }

        windowManager.addView(this, layoutParams)
        HeadFloatLogger.d("BubbleView attached to WindowManager")
    }

    fun detachFromWindow() {
        layoutParams?.let {
            windowManager.removeView(this)
            layoutParams = null
            miniScreenView?.let { windowManager.removeView(it) }
            miniScreenView = null
            miniScreenOpen = false
            HeadFloatLogger.d("BubbleView detached from WindowManager")
        }
    }
}
