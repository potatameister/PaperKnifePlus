# PDFBox-Android specific rules
-keep class com.tomroush.pdfbox.** { *; }
-dontwarn com.tomroush.pdfbox.**

# Maintain Compose and Material 3 functionality
-keep class androidx.compose.material3.** { *; }
-dontwarn androidx.compose.material3.**

# Generic Android rules
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
