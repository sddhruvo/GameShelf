# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.gamevault.app.data.model.** { *; }
-keep class com.gamevault.app.domain.model.** { *; }
