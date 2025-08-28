package com.noahschicksal.headfloat

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView

class TrashView(context: Context) : FrameLayout(context) {

    private var windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val trashIcon: ImageView = ImageView(context)
    private var layoutParams: WindowManager.LayoutParams? = null

    init {
        trashIcon.layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        addView(trashIcon)
        visibility = GONE
    }

    fun setIcon(drawable: Drawable?) {
        trashIcon.setImageDrawable(drawable)
    }

    fun attachToWindow() {
        if (layoutParams != null) return

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 100 // distância da parte inferior (ajustável)
        }

        windowManager.addView(this, layoutParams)
        visibility = GONE
        HeadFloatLogger.d("TrashView attached to WindowManager")
    }

    fun detachFromWindow() {
        layoutParams?.let {
            windowManager.removeView(this)
            layoutParams = null
            visibility = GONE
            HeadFloatLogger.d("TrashView detached from WindowManager")
        }
    }

    fun show() {
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }

    /**
     * Verifica se o ponto fornecido (x, y) está dentro da trash
     */
    fun isPointInside(x: Int, y: Int): Boolean {
        val location = IntArray(2)
        getLocationOnScreen(location)
        val left = location[0]
        val top = location[1]
        val right = left + width
        val bottom = top + height
        return x in left..right && y in top..bottom
    }

    /**
     * Retorna a posição central da TrashView para cálculo de atração
     */
    fun getCenterPosition(): Pair<Int, Int> {
        val location = IntArray(2)
        getLocationOnScreen(location)
        val centerX = location[0] + width / 2
        val centerY = location[1] + height / 2
        return centerX to centerY
    }
}
