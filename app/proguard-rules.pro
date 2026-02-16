# ONNX Runtime
-keep class com.microsoft.onnxruntime.** { *; }
-keep class ai.onnxruntime.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.sbm.aoi.**$$serializer { *; }
-keepclassmembers class com.sbm.aoi.** {
    *** Companion;
}
-keepclasseswithmembers class com.sbm.aoi.** {
    kotlinx.serialization.KSerializer serializer(...);
}
