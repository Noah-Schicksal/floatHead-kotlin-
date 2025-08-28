package com.noahschicksal.headfloat

import android.util.Log

object HeadFloatLogger {
    private const val TAG = "HeadFloat"

    fun d(message: String) {
        Log.d(TAG, message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }

    fun i(message: String) {
        Log.i(TAG, message)
    }
}
