# PDFBox-Android R8/ProGuard Rules
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.filter.JPXFilter
-dontwarn com.gemalto.jp2.**

# Keep Compose/Material classes
-keep class androidx.compose.** { *; }
-keep class androidx.material3.** { *; }
