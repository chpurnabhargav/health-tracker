# Keep all of the app's own classes (small app; avoids surprises with reflection in Room,
# the accessibility service and notification receivers).
-keep class com.healthtracker.app.** { *; }
