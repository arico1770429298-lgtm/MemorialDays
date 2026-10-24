package com.example.memorialdays.widget

import androidx.glance.appwidget.GlanceAppWidget

class MultiEventWidgetReceiver : BaseGlanceWidgetReceiver() {
    override val widget: GlanceAppWidget = MultiEventWidget()
}
