# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.gamevault.app.data.model.** { *; }
-keep class com.gamevault.app.domain.model.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-dontwarn dagger.hilt.internal.aggregatedroot.codegen.**

# Hilt ViewModels
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Hilt entry points (used by AdBlockVpnService)
-keep @dagger.hilt.EntryPoint class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }

# Vico charts
-keep class com.patrykandpatrick.vico.** { *; }
-dontwarn com.patrykandpatrick.vico.**

# Glance widgets
-keep class androidx.glance.** { *; }
-dontwarn androidx.glance.**

# WorkManager + Hilt Worker
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class androidx.hilt.work.** { *; }

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**

# DataStore
-keep class androidx.datastore.** { *; }

# VPN Service
-keep class com.gamevault.app.service.AdBlockVpnService$VpnServiceEntryPoint { *; }
