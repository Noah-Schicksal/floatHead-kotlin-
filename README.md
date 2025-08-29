# floatHead-kotlin-

I created this because I was having some trouble adding a floating bubble to one of my projects. It's probably far from ideal, but it's easy and straightforward to use:

First, you need to download the aar file, then add your project's libs folder. If it doesn't exist, simply create it.

After that, go to build.gradle at the app level and declare it within android:

```gradle
repositories {
    flatDir {
        dirs 'libs'
    }
}
```

then the implementation: 

```gradle
implementation(name: 'floatHead-release', ext: 'aar')
// or
implementation(name: 'floatHead-debug', ext: 'aar') // if you are using the debug version
```

and then you should have something like:

```gradle
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    // all the rest of your build.gradle
    dependencies {
        // all your other dependencies
        implementation(name: 'floatHead-release', ext: 'aar') // or --> implementation(name: 'floatHead-debug', ext: 'aar') if you are using the debug version
    }

    repositories {
        flatDir {
            dirs 'libs'
        }
    }
}
```

how to use:

(I really tried to make this as simple as possible and I don't know if that's a problem or gets in the way of anything)

create a class for extensions, example:

# HeadFloatExtensions.kt

```kotlin

import android.content.Context
import com.noahschicksal.headfloat.HeadFloat

fun HeadFloat.Companion.initWithConfig(context: Context, config: HeadFloatConfig) {
    HeadFloat.init(context)
        .apply {
            config.bubbleIconRes?.let { setBubbleIcon(it) }
            config.bubbleBitmap?.let { setBubbleIcon(it) }
            config.trashIconRes?.let { setTrashIcon(it) }
            config.targetActivity?.let { setTargetActivity(it as Class<android.app.Activity>) }
            enableMagnet(config.magnetEnabled)
            enableDrag(config.dragEnabled)
            config.profileJsonPath?.let { setProfileJsonPath(it) }
            setMessageInterval(config.messageInterval)
            setMessageDuration(config.messageDuration)
            setMessageBackgroundColor(config.messageBackgroundColor)
            setBubbleSizeDp(config.bubbleSizeDp)
        }
        .show()
}
```

create a class for configuration, example:

# HeadFloatConfig.kt

```kotlin

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes

data class HeadFloatConfig(
    @DrawableRes val bubbleIconRes: Int? = null,
    val bubbleBitmap: Bitmap? = null,
    @DrawableRes val trashIconRes: Int? = null,
    val targetActivity: Class<*>? = null,
    val magnetEnabled: Boolean = true,
    val dragEnabled: Boolean = true,
    val profileJsonPath: String? = null,
    val messageInterval: Long = 15000,
    val messageDuration: Long = 5000,
    @ColorInt val messageBackgroundColor: Int = 0xFFFFFF,
    val bubbleSizeDp: Int = 56
)
 ```

and finally create a service class, for example:

# HeadFloatService.kt

```kotlin

import com.noahschicksal.headfloat.HeadFloat


object HeadFloatService {

    fun start(context: Context) {
        // Configuração da bolha
        val config = HeadFloatConfig(
            bubbleIconRes = R.drawable.ic_bubble,        // sets the floating bubble icon
            trashIconRes = R.drawable.ic_trash,         // sets the trash can icon
            targetActivity = MainActivity::class.java,   // activity that will open when you tap the bubble
            magnetEnabled = true,                        // defines whether your bubble sticks to the sides of the screen or not (true or false)
            dragEnabled = true,                          // if the app user can drag the bubble and position it as they prefer on the screen (true or false)
            profileJsonPath = "null",                    // you can leave it as false, this function just manages a json, and displays some random messages, if you want here you must define the file path
            messageInterval = 15000,                     // defines the range of messages provided by the json file, if json is null, it makes no difference
            messageDuration = 5000,                      // sets the duration of the messages provided by the json file, if json is null, it makes no difference
            messageBackgroundColor = Color.parseColor("#FFBB33"), // sets the background color of the messages provided by the json file, if json is null, it makes no difference
            bubbleSizeDp = 80                            // set the bubble size in dp, this is not working, the size of your icon in "bubbleIconRes = R.drawable.ic_bubble,", but avoid leaving this value at 0, or your bubble will be invisible
        )

        // Inicializa a bolha com a configuração
        HeadFloat.initWithConfig(context, config)
    }
}
```

if you want to use profileJsonPath = "" for some fun, the json template is:

```json
{
    "profile": [
        {
          "profiles": 1,
          "image": "path to your image" //This changes your balloon image, randomly switching between the profiles you list here.
          "messages": ["message1", "message2", "..."] // messages should also be displayed randomly, but will always follow the profile
        }
]
```

```kotlin
binding.appBarMain.fab.setOnClickListener { view ->
            // Inicia a bolha da biblioteca
            HeadFloatService.start(this)
```

Finally, when everything is set up, you can use a simple button to execute the bubble; in the example, I only use the fab in my MainActivity

I don't really know what I'm doing, since Kotlin is new to me. If you have any issues or want to contribute, feel free to do so.

The main goal is to have a bubble that works like the Messenger one, but it randomly changes the image and displays one of the random messages from the profile listed in the JSON, and then repeats. When you click on the bubble, it should expand exactly like the Messenger window, perhaps in a smaller size, displaying an activity or a fragment. I don't know anymore. After three days of sleepless nights trying to get this to work in such an unsatisfactory way, it's a bit difficult to think about.

You might be wondering, "Why are random images and random messages being displayed in the bubble?" Well, that's what my project needs, nothing more. But I believe that if this works, it shouldn't be difficult to adapt it to work as a messenger, or something similar. 

I took the trouble to leave some things already defined by default in the library, such as the magnetism on the sides, the way the trash can is displayed, etc.
