# Room, Compose and Glance ship consumer rules, but Glance's update pipeline
# is reached through internal entry points (AppWidgetSession,
# GlanceAppWidgetManager, provideContent) that R8 cannot see from app code.
# Trimming any of them left the widget stuck on its loading spinner in release.
-keep class androidx.glance.** { *; }

# Keep the Room runtime and generated DAO implementations intact; the widget
# reads the database from a background coroutine and must never hit a missing
# implementation in release.
-keep class androidx.room.** { *; }
-keep class com.example.memorialdays.data.** { *; }

# Widget receivers are referenced from AndroidManifest.xml; keep them for
# framework instantiation and for the R8 full-mode reflection checks.
-keep class com.example.memorialdays.widget.**Receiver { *; }

# R8 reports the provideGlance override of a GlanceAppWidget subclass as
# unused (the call goes through Glance's runtime), so keep the whole widget
# package as well.
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class com.example.memorialdays.widget.** { *; }

# The lunar engine is a third-party calendar library; keep it intact so R8
# never strips internals used while resolving solar/lunar dates.
-keep class com.nlf.calendar.** { *; }
