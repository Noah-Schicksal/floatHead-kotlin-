package com.noahschicksal.headfloat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONObject
import java.io.File
import kotlin.random.Random

data class Profile(
    val id: Int,
    val imagePath: String,
    val messages: List<String>
) {
    val imageDrawable: Bitmap?
        get() = runCatching { BitmapFactory.decodeFile(imagePath) }.getOrNull()
}

class ProfileManager(private val context: Context, jsonFilePath: String) {

    private val profiles: List<Profile>

    init {
        val file = File(jsonFilePath)
        if (!file.exists()) {
            HeadFloatLogger.e("Profile JSON file not found: $jsonFilePath")
            profiles = emptyList()
        } else {
            val jsonString = file.readText()
            val json = JSONObject(jsonString)
            val arr = json.optJSONArray("perfis")
            val tmp = mutableListOf<Profile>()
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optInt("perfil", i)
                    val img = obj.optString("imagem")
                    val messages = mutableListOf<String>()
                    val msgsArr = obj.optJSONArray("mensagens")
                    if (msgsArr != null) {
                        for (j in 0 until msgsArr.length()) {
                            messages.add(msgsArr.optString(j))
                        }
                    }
                    tmp.add(Profile(id, img, messages))
                }
            }
            profiles = tmp
        }
    }

    fun getRandomProfile(): Profile? {
        if (profiles.isEmpty()) return null
        return profiles[Random.nextInt(profiles.size)]
    }

    fun getRandomMessage(profile: Profile): String {
        return if (profile.messages.isEmpty()) "" else profile.messages.random()
    }

    fun randomProfileMessage(): Pair<Bitmap?, String>? {
        val profile = getRandomProfile() ?: return null
        val msg = getRandomMessage(profile)
        return profile.imageDrawable to msg
    }
}
