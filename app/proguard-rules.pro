# Room, Compose and Glance ship their own consumer rules.
# Widget receivers are referenced from AndroidManifest.xml; keep them for
# framework instantiation and for the R8 full-mode reflection checks.
-keep class com.example.memorialdays.widget.**Receiver { *; }
