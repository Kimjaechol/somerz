# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Firebase AI classes
-keep class com.google.firebase.ai.** { *; }

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.somerz.translator.**$$serializer { *; }
-keepclassmembers class com.somerz.translator.** {
    *** Companion;
}
-keepclasseswithmembers class com.somerz.translator.** {
    kotlinx.serialization.KSerializer serializer(...);
}
