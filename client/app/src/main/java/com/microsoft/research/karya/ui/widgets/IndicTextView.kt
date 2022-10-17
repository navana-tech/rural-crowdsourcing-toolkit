package com.microsoft.research.karya.ui.widgets

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import com.google.android.material.textview.MaterialTextView

class IndicTextView : MaterialTextView {

  private val font = Typeface.createFromAsset(context.assets, "mukta.ttf")

  constructor(context: Context) : this(context, null)
  constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
    typeface = font
  }
}
