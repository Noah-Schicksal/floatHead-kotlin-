package com.noahschicksal.headfloat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class MessageBubbleView(context: Context) : FrameLayout(context) {

    private val textView: TextView = TextView(context)
    private var iconView: ImageView? = null
    private var windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var layoutParams: WindowManager.LayoutParams? = null
    private val handler = Handler()

    init {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 16, 24, 16)
            setBackgroundColor(Color.parseColor("#AA333333"))
        }

        iconView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(64, 64)
            visibility = GONE
        }
        container.addView(iconView)

        textView.setTextColor(Color.WHITE)
        textView.textSize = 14f
        container.addView(textView)

        addView(container)
    }

    fun setMessage(message: String) {
        textView.text = message
    }

    fun setIcon(bitmap: Bitmap?) {
        bitmap?.let {
            iconView?.setImageBitmap(it)
            iconView?.visibility = VISIBLE
        } ?: run {
            iconView?.visibility = GONE
        }
    }

    /**
     * Mostra a mensagem próximo a uma âncora (ex.: bolha)
     */
    fun show(windowManager: WindowManager, anchor: FrameLayout?, duration: Long) {
        if (layoutParams != null) return

        val anchorX = anchor?.x?.toInt() ?: 0
        val anchorY = anchor?.y?.toInt() ?: 0

        showAtPosition(windowManager, anchorX + 150, anchorY, duration)
    }

    
     // Mostra a mensagem em uma posição específica da tela
     
    fun showAtPosition(windowManager: WindowManager, x: Int, y: Int, duration: Long) {
        if (layoutParams != null) return

        // Pegando dimensões da tela
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Medir a largura e altura da view antes de adicionar
        measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        val viewWidth = measuredWidth
        val viewHeight = measuredHeight

        // Ajuste X: se ultrapassar borda direita, reposicionar
        var adjustedX = x
        if (x + viewWidth > screenWidth - 16) { // 16px de margem
            adjustedX = screenWidth - viewWidth - 16
        }
        if (adjustedX < 16) adjustedX = 16 // margem esquerda

        // Ajuste Y: se ultrapassar borda inferior, reposicionar
        var adjustedY = y
        if (y + viewHeight > screenHeight - 16) {
            adjustedY = screenHeight - viewHeight - 16
        }
        if (adjustedY < 16) adjustedY = 16 // margem superior

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = adjustedX
            this.y = adjustedY
        }

        try {
            windowManager.addView(this, layoutParams)
        } catch (e: Exception) {
            HeadFloatLogger.e("Error adding MessageBubbleView", e)
        }

        handler.postDelayed({
            dismiss(windowManager)
        }, duration)
    }


    fun dismiss(windowManager: WindowManager) {
        layoutParams?.let {
            try {
                windowManager.removeView(this)
            } catch (_: Exception) { }
            layoutParams = null
        }
    }
}
