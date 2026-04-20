# Domain module ProGuard rules

# Keep all domain models
-keep class com.sperrmuell.finder.domain.model.** { *; }

# Keep repository interfaces
-keep interface com.sperrmuell.finder.domain.repository.** { *; }

# Keep use cases
-keep class com.sperrmuell.finder.domain.usecase.** { *; }

# Keep enum classes
-keepclassmembers enum com.sperrmuell.finder.domain.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep data classes for serialization
-keepclassmembers class com.sperrmuell.finder.domain.model.** {
    <fields>;
    <init>(...);
}
