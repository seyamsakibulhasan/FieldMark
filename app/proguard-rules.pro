# Keep model classes used by reflection / serialization
-keep class com.fieldmark.app.annotation.** { *; }

# Keep Compose runtime metadata
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**
