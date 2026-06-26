# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep JavaScript interfaces
-keepattributes *Annotation*
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep bridge classes
-keepclassmembers class com.toolbox.app.bridge.ToolBridge {
   public *;
}
-keepclassmembers class com.toolbox.app.bridge.BoxNative {
   public *;
}

# Keep model classes for Gson/JSON serialization
-keep class com.toolbox.app.model.** { *; }
-keep class org.json.** { *; }

# Keep AndroidX classes
-keep class androidx.appcompat.** { *; }

# Keep Material Design components
-keep class com.google.android.material.** { *; }

