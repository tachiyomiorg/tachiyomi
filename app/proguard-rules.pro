-dontobfuscate

-dontwarn eu.kanade.tachiyomi.**
-keep class eu.kanade.tachiyomi.**
-keep class eu.kanade.tachiyomi.source.model.** { *; }

# Design library
-dontwarn com.google.android.material.**
-keep class com.google.android.material.** { *; }
-keep interface com.google.android.material.** { *; }
-keep public class com.google.android.material.R$* { *; }

-keep class com.hippo.image.** { *; }
-keep interface com.hippo.image.** { *; }
-dontwarn nucleus.view.NucleusActionBarActivity

# Extensions may require methods unused in the core app
-keep class org.jsoup.** { *; }
-keep class kotlin.** { *; }
-keep class okhttp3.** { *; }
-keep class com.google.gson.** { *; }
-keep class com.github.salomonbrys.kotson.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn retrofit2.Platform$Java8

# Glide specific rules #
# https://github.com/bumptech/glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.AppGlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# RxJava 1.1.0
-dontwarn sun.misc.**

-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
   long producerIndex;
   long consumerIndex;
}

-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}

-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}

# ReactiveNetwork
-dontwarn com.github.pwittchen.reactivenetwork.**

## GSON ##

# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# Gson specific classes
-keep class sun.misc.Unsafe { *; }

# Prevent proguard from stripping interface information from TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# SnakeYaml
-keep class org.yaml.snakeyaml.** { public protected private *; }
-dontwarn org.yaml.snakeyaml.**

# Duktape
-keep class com.squareup.duktape.** { *; }
