# Keep kotlinx.serialization generated serializers for the layout models.
-keepclassmembers class com.verbum.launcher.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.verbum.launcher.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
