# kotlinx.serialization: keep generated serializers for DTOs
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-keepclassmembers class com.toobit.platform.data.** { *** Companion; }
-keepclasseswithmembers class com.toobit.platform.data.** { kotlinx.serialization.KSerializer serializer(...); }
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
