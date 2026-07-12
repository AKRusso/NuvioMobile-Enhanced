# Full builds can load Android CloudStream .cs3 packages dynamically.
# Keep shrinking/obfuscation enabled, but skip R8 optimization passes that become
# prohibitively slow with the CloudStream compatibility runtime dependency graph.
-dontoptimize

# Some CloudStream providers are compiled against fuzzywuzzy and resolve this
# package by its original JVM name from dynamically loaded .cs3 dex files.
-keep class me.xdrop.fuzzywuzzy.** { *; }
-dontwarn me.xdrop.fuzzywuzzy.**

# Dynamic CloudStream providers can link directly against kotlinx.serialization
# ABI classes, so their JVM names must remain available in full release builds.
-keep class kotlinx.serialization.** { *; }
-keep interface kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# Several providers also call CloudStream cryptography helpers directly.
-keep class dev.whyoleg.cryptography.** { *; }
-keep interface dev.whyoleg.cryptography.** { *; }
-dontwarn dev.whyoleg.cryptography.**
