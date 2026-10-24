package com.example.memorialdays.widget

import androidx.glance.appwidget.GlanceAppWidget

class SingleEventWidgetReceiver : BaseGlanceWidgetReceiver() {
    override val widget: GlanceAppWidget = SingleEventWidget()
}
