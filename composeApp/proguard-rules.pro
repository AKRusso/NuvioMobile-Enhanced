# Project-specific ProGuard rules for composeApp Android release builds.

# Keep useful metadata for crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve Kotlin metadata/signatures needed by reflection/generics-heavy libraries.
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations

# Ktor / Supabase client stack (runtime reflective paths in serializers/plugins).
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Keep @Serializable generated serializers.
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep class com.nuvio.app.features.catalog.CatalogTargetKind { *; }

# Avoid R8 merging/optimizing the stream badge chip used in lazy stream rows.
-keep class com.nuvio.app.features.streams.StreamBadgeChipKt { *; }
-keep class com.nuvio.app.features.streams.StreamBadgeChipSize { *; }
-keep class com.nuvio.app.features.streams.StreamBadgeChipDefaults { *; }

-keep class com.nuvio.app.features.streams.StreamsScreenKt { *; }
-keep class com.nuvio.app.features.streams.StreamsScreenKt$* { *; }

# Avoid R8 producing verifier-invalid bytecode for the large player composable.
-keep class com.nuvio.app.features.player.PlayerScreenKt { *; }
-keep class com.nuvio.app.features.player.PlayerScreenKt$* { *; }

# QuickJS plugin runtime is dynamic; keep runtime and app plugin classes.
-keep class com.dokar.quickjs.** { *; }
-keep class com.nuvio.app.features.plugins.** { *; }

# Standard CloudStream .cs3 packages link against these names at runtime.
# They must remain binary-stable and unobfuscated in Android full release builds.
-keep class com.lagradost.cloudstream3.** { *; }
-keep interface com.lagradost.cloudstream3.** { *; }
-keep class com.lagradost.api.** { *; }
-keep class com.lagradost.nicehttp.** { *; }
-keep class com.nuvio.app.features.cloudstream.CloudStreamPlatformRuntime* { *; }
-keep,allowoptimization class com.fasterxml.jackson.** { public protected *; }
-keep,allowoptimization class kotlin.** { public protected *; }
-keep,allowoptimization class kotlinx.coroutines.** { public protected *; }
-keep,allowoptimization class okhttp3.** { public protected *; }
-keep,allowoptimization class okio.** { public protected *; }
-keep,allowoptimization class org.jsoup.** { public protected *; }
-keep,allowoptimization class org.mozilla.javascript.** { public protected *; }
-keep,allowoptimization class com.uwetrottmann.tmdb2.** { public protected *; }
-dontwarn com.lagradost.cloudstream3.**
-dontwarn com.lagradost.nicehttp.**
# Rhino ships optional JVM integrations that are not present on Android.
# CloudStream providers use Rhino core APIs; these desktop/JDK integrations are not required there.
-dontwarn java.beans.**
-dontwarn javax.script.**
-dontwarn jdk.dynalink.**

# P2P runtime and Nuvio Engine JNI bridge. Native libraries are not processed
# by R8, but their Kotlin/JNI wrapper classes and method names must stay stable.
-keep class com.nuvio.app.features.p2p.** { *; }
-keep class com.nuvio.engine.** { *; }
-keep interface com.nuvio.engine.** { *; }

-keep class androidx.work.impl.WorkDatabase_Impl { *; }

# Media3 / ExoPlayer classes from local AAR decoders and stock modules.
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-keep class com.google.android.exoplayer2.** { *; }
-keep interface com.google.android.exoplayer2.** { *; }

-keep class is.xyz.mpv.** { *; }
-keep interface is.xyz.mpv.** { *; }

# Common optional security providers used by okhttp on some devices.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
