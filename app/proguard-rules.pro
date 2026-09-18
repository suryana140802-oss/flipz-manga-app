# Proguard rules for Flipz Manga

# Room
-keep class * extends androidx.room.RoomDatabase
-keep class com.example.data.dao.** { *; }
-keep class com.example.data.entity.** { *; }
-dontwarn androidx.room.paging.**

# Data Models
-keep class com.example.model.** { *; }
-keep class com.example.domain.model.** { *; }

# Junrar (Archive extraction)
-keep class com.github.junrar.** { *; }
-dontwarn com.github.junrar.**

# Jsoup (Web scraping)
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# ML Kit (OCR & Translation)
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# OkHttp & Coil
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn coil.**

# SLF4J (Junrar optional logger)
-dontwarn org.slf4j.**

# Start.io (StartApp)
-keep class com.startapp.** { *; }
-keep class com.truenet.** { *; }
-keepattributes Exceptions, InnerClasses, Signature, Deprecated, SourceFile, LineNumberTable, *Annotation*, EnclosingMethod
-dontwarn android.webkit.JavascriptInterface
-dontwarn com.startapp.**
-dontwarn org.jetbrains.annotations.**

