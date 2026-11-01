# Tesseract / Leptonica: native code looks up these classes, fields and callbacks by name.
-keep class com.googlecode.tesseract.android.** { *; }
-keep class com.googlecode.leptonica.android.** { *; }

# Readable stack traces in logcat (personal app; name obfuscation saves little here).
-dontobfuscate
-keepattributes SourceFile,LineNumberTable
