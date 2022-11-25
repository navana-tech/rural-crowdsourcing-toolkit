package com.microsoft.research.karya.utils

import android.content.Context
import android.graphics.Typeface
import android.util.Log

object WorkerLanguage {
  var language = ""

  fun getFontForLanguage(context: Context): Typeface? {
    return when (language) {
      "kn" -> Typeface.createFromAsset(context.assets, "kannada.ttf")
      "te" -> Typeface.createFromAsset(context.assets, "telugu.ttf")
      "ta" -> Typeface.createFromAsset(context.assets, "tamil.ttf")
      "ml" -> Typeface.createFromAsset(context.assets, "malayalam.ttf")
      else -> Typeface.createFromAsset(context.assets, "devanagari.ttf")
    }.also { Log.d("WorkerLanguage", "Sending font for $language") }
  }
}
