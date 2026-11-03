# Release builds are shrunk with R8. These rules keep what libGDX reaches through reflection.
-dontwarn android.support.**
-dontwarn com.badlogic.gdx.backends.android.AndroidFragmentApplication

# scene2d UI and text rendering.
-keep public class com.badlogic.gdx.scenes.scene2d.** { *; }
-keep public class com.badlogic.gdx.graphics.g2d.BitmapFont { *; }
-keep public class com.badlogic.gdx.graphics.Color { *; }

# Keeps line numbers so release stack traces can be retraced:
# https://developer.android.com/build/shrink-code#retracing
-keepattributes LineNumberTable,SourceFile
-renamesourcefileattribute SourceFile
