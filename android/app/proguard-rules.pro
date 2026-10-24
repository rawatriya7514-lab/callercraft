# No minify in release; keep Firebase DT provenance rules if you enable R8 later.
-keep class com.craftcaller.app.data.model.** { *; }
-dontwarn okhttp3.**
