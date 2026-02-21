# Optimization settings
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively

# PDFBox-Android R8/ProGuard Rules (Refined)
# Only keep necessary components for font loading and reflection
-keep class com.tom_roush.pdfbox.pdmodel.font.PDFont { *; }
-keep class com.tom_roush.pdfbox.pdmodel.font.PDType0Font { *; }
-keep class com.tom_roush.pdfbox.pdmodel.font.PDType1Font { *; }
-keep class com.tom_roush.pdfbox.pdmodel.font.PDTrueTypeFont { *; }
-keep class com.tom_roush.pdfbox.pdmodel.font.PDCIDFontType2 { *; }
-keep class com.tom_roush.pdfbox.pdmodel.font.PDSimpleFont { *; }
-keep class com.tom_roush.pdfbox.pdmodel.font.PDType3Font { *; }

# Allow stripping of unused filters and components
-dontwarn com.tom_roush.pdfbox.filter.JPXFilter
-dontwarn com.gemalto.jp2.**
-dontwarn org.bouncycastle.**
-dontwarn javax.xml.stream.**

# Compose and Material3 rules are automatically included in the libraries.
# Generic keep rules here are redundant and prevent optimization.
# (Removed)

# Coil
-keep class coil.** { *; }
-dontwarn coil.**
