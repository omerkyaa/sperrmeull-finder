# Core module ProGuard rules

# Keep all public API classes
-keep public class com.sperrmuell.finder.core.** { *; }

# Keep utility classes
-keep class com.sperrmuell.finder.core.util.** { *; }

# Keep UI theme classes
-keep class com.sperrmuell.finder.core.ui.theme.** { *; }

# Keep DI modules
-keep class com.sperrmuell.finder.core.di.** { *; }

# Keep Result class and extensions
-keep class com.sperrmuell.finder.core.util.Result { *; }
-keep class com.sperrmuell.finder.core.util.ResultKt { *; }

# Keep Logger
-keep class com.sperrmuell.finder.core.util.Logger { *; }
