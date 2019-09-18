# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile



#-dontobfuscate #없애면 난독화 X
#-dontoptimize #없애면 최적화 X
#-keepresourcexmlattributenames manifest/** #없애면 manifest 난독화 X

#-dontshrink # 사용하지 않는 메소드 유지
#-keepparameternames
-dontwarn android.support.v4.**,org.slf4j.**,com.google.android.gms.**
#-dontskipnonpubliclibraryclasses


-renamesourcefileattribute SourceFile

-keep interface com.platbread.vrix.adsdk.** { *; }
-keep public class com.platbread.vrix.adsdk.** { *; }
