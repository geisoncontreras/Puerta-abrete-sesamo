package com.abretesesamo.app

import android.app.Application
import java.io.File

/**
 * Si la app se cierra de golpe por un error, lo guardamos para enseñarlo la próxima vez
 * que se abra (así se puede mandar una captura y saber qué pasó).
 */
class PuertaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                errorFile(this).writeText(
                    "Android ${android.os.Build.VERSION.RELEASE} · ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n\n" +
                        error.stackTraceToString().take(4000)
                )
            }
            previous?.uncaughtException(thread, error)
        }
    }

    companion object {
        fun errorFile(app: Application): File = File(app.filesDir, "ultimo_error.txt")
        fun errorFile(context: android.content.Context): File = File(context.filesDir, "ultimo_error.txt")
    }
}
