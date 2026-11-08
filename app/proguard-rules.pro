# Keep native methods (JNI)
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep LSParanoid annotations
-keep @org.lsposed.lsparanoid.Obfuscate class *

# Keep all Activities, Services
-keep class * extends android.app.Activity
-keep class * extends android.app.Service
-keepclassmembers class * extends android.app.Activity {
    public void onCreate(android.os.Bundle);
}

# Keep main package
-keep class com.bgmi.** { *; }

# Keep SboxCore and related classes
-keep class com.sbox.** { *; }
-keep class com.zcore.** { *; }

# Keep package manager / signature related code
-keep class android.content.pm.** { *; }
-keep class java.security.MessageDigest { *; }

# Keep BlackReflection
-keep class top.niunaijun.blackreflection.** { *; }

# Keep FreeReflection
-keep class me.weishu.reflection.** { *; }

# Keep OkHttp internals
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Keep Zip4j
-keep class net.lingala.zip4j.** { *; }

# Keep LibSU
-keep class com.topjohnwu.superuser.** { *; }

# Suppress warnings
-dontwarn javax.annotation.**
-dontwarn com.squareup.javapoet.**