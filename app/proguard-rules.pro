# Keep kotlinx.serialization generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class com.davidcarranco.oneloop.medtracker.**$$serializer { *; }
-keepclassmembers class com.davidcarranco.oneloop.medtracker.** {
    *** Companion;
}
-keepclasseswithmembers class com.davidcarranco.oneloop.medtracker.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Supabase / Ktor / Kotlinx
-dontwarn org.slf4j.**
-dontwarn kotlinx.serialization.**
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }

# Glance widgets
-keep class com.davidcarranco.oneloop.medtracker.widget.** { *; }
