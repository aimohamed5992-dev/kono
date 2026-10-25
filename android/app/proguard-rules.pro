# kotlinx.serialization — keep generated serializers and @Serializable models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class sa.kono.app.**$$serializer { *; }
-keepclassmembers class sa.kono.app.** { *** Companion; }
-keepclasseswithmembers class sa.kono.app.** { kotlinx.serialization.KSerializer serializer(...); }
# osmdroid
-dontwarn org.osmdroid.**
-keep class org.osmdroid.** { *; }
# okhttp / okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
