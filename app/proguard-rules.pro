# ---- FeaturesticBox SDK (reflection heavy, keep everything) ----
-keep class com.featurestic.** { *; }
-keep class com.sbox.** { *; }
-keep class net_62v.** { *; }
-keep class android.MetaCore.** { *; }
-keep class org.lsposed.hiddenapibypass.** { *; }
-keep class com.bgmi.** { *; }

-keepclassmembers class * {
    native <methods>;
}

-dontwarn **
-dontoptimize
