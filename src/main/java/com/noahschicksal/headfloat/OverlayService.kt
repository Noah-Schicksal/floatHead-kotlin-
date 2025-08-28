package com.noahschicksal.headfloat

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.IBinder
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubbleView: BubbleView? = null
    private var trashView: TrashView? = null
    private var messageView: MessageBubbleView? = null
    private var handler: Handler = Handler()
    private var profileManager: ProfileManager? = null

    private var targetActivityName: String? = null
    private var messageInterval: Long = 15000
    private var messageDuration: Long = 5000
    private var messageBackgroundColor: Int = android.graphics.Color.YELLOW
    private var bubbleSizeDp: Int = 56

    private var dragEnabled: Boolean = true
    private var magnetEnabled: Boolean = true
    private var bubbleBitmap: Bitmap? = null

    // Mini Tela 
    private var pendingMiniFragment: Pair<FragmentActivity, Fragment>? = null

    private val messageRunnable = object : Runnable {
        override fun run() {
            showRandomMessage()
            handler.postDelayed(this, messageInterval)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        HeadFloatLogger.d("OverlayService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        HeadFloatLogger.d("OverlayService started")

        val bubbleIconRes = intent?.getIntExtra("bubbleIconRes", -1)
        val trashIconRes = intent?.getIntExtra("trashIconRes", -1)
        dragEnabled = intent?.getBooleanExtra("dragEnabled", true) ?: true
        magnetEnabled = intent?.getBooleanExtra("magnetEnabled", true) ?: true
        targetActivityName = intent?.getStringExtra("targetActivity")
        val profileJsonPath = intent?.getStringExtra("profileJsonPath")
        messageInterval = intent?.getLongExtra("messageInterval", 15000) ?: 15000
        messageDuration = intent?.getLongExtra("messageDuration", 5000) ?: 5000
        messageBackgroundColor = intent?.getIntExtra("messageBackgroundColor", android.graphics.Color.YELLOW) ?: android.graphics.Color.YELLOW
        bubbleSizeDp = intent?.getIntExtra("bubbleSizeDp", 56) ?: 56
        bubbleBitmap = intent?.getParcelableExtra("bubbleBitmap")

        // Carregar perfis 
        profileJsonPath?.let {
            profileManager = ProfileManager(this, it)
        }

        // Criar TrashView primeiro 
        if (trashIconRes != null && trashIconRes != -1) {
            trashView = TrashView(this).apply {
                setIcon(ContextCompat.getDrawable(this@OverlayService, trashIconRes))
            }
            trashView?.attachToWindow()
            trashView?.hide() // só mostra quando o usuário segura a bolha
        }

        // Criar bolha
        bubbleView = BubbleView(this).apply {
            setSizeDp(bubbleSizeDp)
            setTrashView(trashView!!)
            enableDrag(dragEnabled)
            enableMagnet(magnetEnabled)
            setOnBubbleClickListener { openTargetActivity() }

            // Aplicar bitmap ou drawable
            bubbleBitmap?.let { setIcon(BitmapDrawable(resources, it)) }
            bubbleIconRes?.takeIf { it != -1 }?.let {
                setIcon(ContextCompat.getDrawable(this@OverlayService, it))
            }
        }

        // Aplicar fragment pendente se houver
        pendingMiniFragment?.let { (activity, fragment) ->
            bubbleView?.setFragment(activity, fragment)
            pendingMiniFragment = null
        }

        // Adicionar bolha na tela
        bubbleView?.attachToWindow()

        // Detecta colisão com lixeira para destruir a bolha
        bubbleView?.setOnDropListener { x, y ->
            trashView?.let { trash ->
                if (trash.isPointInside(x, y)) {
                    bubbleView?.detachFromWindow()
                    trash.hide()
                    HeadFloatLogger.d("Bubble removed via TrashView")
                }
            }
        }

        // Criar mensagem flutuante
        messageView = MessageBubbleView(this).apply {
            setBackgroundColor(messageBackgroundColor)
        }

        // Iniciar ciclo de mensagens
        handler.postDelayed(messageRunnable, messageInterval)

        return START_STICKY
    }

    private fun showRandomMessage() {
        profileManager?.let { manager ->
            val profile = manager.getRandomProfile()
            profile?.let {
                val msg = manager.getRandomMessage(profile)
                messageView?.setMessage(msg)
                messageView?.setIcon(profile.imageDrawable)

                // Posicionar mensagem ao lado da bolha
                val bubbleX = bubbleView?.x?.toInt() ?: 0
                val bubbleY = bubbleView?.y?.toInt() ?: 0
                messageView?.showAtPosition(
                    windowManager,
                    bubbleX + (bubbleView?.getSizePx() ?: 0),
                    bubbleY,
                    messageDuration
                )

                HeadFloatLogger.d("Showing message: $msg")
            }
        }
    }

    private fun openTargetActivity() {
        try {
            targetActivityName?.let {
                val clazz = Class.forName(it)
                val intent = Intent(this, clazz)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                HeadFloatLogger.d("Opened activity: $it")
            }
        } catch (e: Exception) {
            HeadFloatLogger.e("Error opening activity", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bubbleView?.detachFromWindow()
        trashView?.detachFromWindow()
        messageView?.dismiss(windowManager)
        handler.removeCallbacks(messageRunnable)
        HeadFloatLogger.d("OverlayService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    //Mini Tela
    fun setMiniScreenFragment(activity: FragmentActivity, fragment: Fragment) {
        if (bubbleView != null) {
            bubbleView?.setFragment(activity, fragment)
        } else {
            pendingMiniFragment = Pair(activity, fragment)
        }
    }

    fun toggleMiniScreen() {
        bubbleView?.toggleMiniScreen()
    }
}
