package com.microsoft.research.karya

import android.app.Application
import android.util.Log
import android.view.Gravity
import com.zabaan.sdk.AssistantUi
import com.zabaan.sdk.CUSTOM_POSITION
import com.zabaan.sdk.InitializeParams
import com.zabaan.sdk.Zabaan
import com.zabaan.sdk.internal.views.AssistantMargin
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp class KaryaApp : Application() {

    override fun onCreate() {
        super.onCreate()

        try {
            val params = InitializeParams.Builder()
                .context(this)
                .assistantImage(R.drawable.img_asst_new)
                .accessToken(BuildConfig.ZABAAN_ACCESS_TOKEN)
                .build()

            Zabaan.setDebug(BuildConfig.DEBUG)
            Zabaan.init(params)

            val assistantUi = AssistantUi.Builder()
                .setAssistantPosition(CUSTOM_POSITION(Gravity.CENTER_HORIZONTAL)) // optional
                .setAssistantMargin(AssistantMargin(32)) // optional
                .build()

            Zabaan.getInstance().setAssistantUi(assistantUi)
        } catch(e: Exception){
            Log.e("ZabaanException", e.stackTrace.toString())
        }
    }
}
