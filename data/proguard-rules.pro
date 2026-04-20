# Data module ProGuard rules

# Keep repository implementations
-keep class com.sperrmuell.finder.data.repository.** { *; }

# Keep data models and DTOs
-keep class com.sperrmuell.finder.data.model.** { *; }

# Keep Firebase data sources
-keep class com.sperrmuell.finder.data.source.firebase.** { *; }

# Keep Room entities and DAOs
-keep class com.sperrmuell.finder.data.source.local.** { *; }

# Keep managers
-keep class com.sperrmuell.finder.data.manager.** { *; }

# Keep mappers
-keep class com.sperrmuell.finder.data.mapper.** { *; }

# Keep workers
-keep class com.sperrmuell.finder.data.worker.** { *; }

# Keep data classes for Firestore serialization
-keepclassmembers class com.sperrmuell.finder.data.model.** {
    <fields>;
    <init>(...);
    public <fields>;
    public <methods>;
}

# Keep Room annotations
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
