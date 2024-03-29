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

-dontobfuscate
-keep public class com.lunabeestudio.** { *; }
-keep public class dgca.verifier.app.decoder.model.** { *; }

# Protobuf https://github.com/protocolbuffers/protobuf/issues/6463#issuecomment-632884075
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }

# Bouncy Castle -- Keep ECDH
-keep class org.bouncycastle.jcajce.provider.asymmetric.EC$* { *; }
-keep class org.bouncycastle.jcajce.provider.asymmetric.ec.KeyPairGeneratorSpi$ECDH { *; }
-keep class org.bouncycastle.jcajce.provider.asymmetric.ec.KeyFactorySpi$ECDH { *; }
-keep class org.bouncycastle.jcajce.provider.asymmetric.ec.KeyFactorySpi$ECDSA { *; }

# json-schema-validator
-keep class com.github.fge.jsonschema.** { *; }
-keep class org.mozilla.javascript.** { *; }