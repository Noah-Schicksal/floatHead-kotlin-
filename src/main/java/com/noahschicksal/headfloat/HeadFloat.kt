package com.noahschicksal.headfloat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

class HeadFloat private constructor(private val context: Context) {

    private var bubbleIconRes: Int? = null
    private var bubbleBitmap: Bitmap? = null
    private var bubbleSizeDp: Int? = null
    private var trashIconRes: Int? = null
    private var targetActivity: Class<out Activity>? = null
    private var magnetEnabled: Boolean = true
    private var dragEnabled: Boolean = true
    private var profileJsonPath: String? = null
    private var messageInterval: Long = 15000
    private var messageDuration: Long = 5000
    private var messageBackgroundColor: Int = Color.YELLOW

    // Nova propriedade para fragment
    private var miniFragment: Fragment? = null
    private var activityContext: FragmentActivity? = null

    companion object {
        fun init(context: Context): HeadFloat {
            return HeadFloat(context)
        }
    }

    fun setBubbleIcon(@DrawableRes resId: Int): HeadFloat {
        this.bubbleIconRes = resId
        return this
    }

    fun setBubbleIcon(bitmap: Bitmap): HeadFloat {
        this.bubbleBitmap = bitmap
        return this
    }

    fun setBubbleSizeDp(dp: Int): HeadFloat {
        this.bubbleSizeDp = dp
        return this
    }

    fun setTrashIcon(@DrawableRes resId: Int): HeadFloat {
        this.trashIconRes = resId
        return this
    }

    fun setTargetActivity(activity: Class<out Activity>): HeadFloat {
        this.targetActivity = activity
        return this
    }

    fun enableMagnet(enable: Boolean): HeadFloat {
        this.magnetEnabled = enable
        return this
    }

    fun enableDrag(enable: Boolean): HeadFloat {
        this.dragEnabled = enable
        return this
    }

    fun setProfileJsonPath(path: String): HeadFloat {
        this.profileJsonPath = path
        return this
    }

    fun setMessageInterval(ms: Long): HeadFloat {
        this.messageInterval = ms
        return this
    }

    fun setMessageDuration(ms: Long): HeadFloat {
        this.messageDuration = ms
        return this
    }

    fun setMessageBackgroundColor(@ColorInt color: Int): HeadFloat {
        this.messageBackgroundColor = color
        return this
    }

    fun setMiniFragment(activity: FragmentActivity?, fragment: Fragment?): HeadFloat {
        this.activityContext = activity
        this.miniFragment = fragment
        return this
    }

    fun show() {
        if (!OverlayPermission.hasPermission(context)) {
            OverlayPermission.requestPermission(context)
            return
        }

        val intent = Intent(context, OverlayService::class.java).apply {
            putExtra("bubbleIconRes", bubbleIconRes)
            putExtra("bubbleSizeDp", bubbleSizeDp)
            putExtra("trashIconRes", trashIconRes)
            putExtra("magnetEnabled", magnetEnabled)
            putExtra("dragEnabled", dragEnabled)
            putExtra("targetActivity", targetActivity?.name)
            putExtra("profileJsonPath", profileJsonPath)
            putExtra("messageInterval", messageInterval)
            putExtra("messageDuration", messageDuration)
            putExtra("messageBackgroundColor", messageBackgroundColor)
        }

        context.startService(intent)
    }
}
