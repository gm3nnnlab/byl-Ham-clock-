# Release minification is disabled for the MVP. If you enable it, keep serialization DTOs:
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.hamdash.chornobyl.**$$serializer { *; }
-keepclassmembers class com.hamdash.chornobyl.** { *** Companion; }
-keepclasseswithmembers class com.hamdash.chornobyl.** { kotlinx.serialization.KSerializer serializer(...); }
