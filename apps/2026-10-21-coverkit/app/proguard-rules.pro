# AGP 9 uses R8 full mode with strict full-mode keep-rule semantics. A rule such
# as `-keep class X` no longer preserves X's default constructor; frameworks that
# instantiate a type reflectively need an explicit `-keep class X { <init>(); }`.

-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault

# kotlin.reflect reads this annotation to reconstruct Kotlin declarations.
-keep class kotlin.Metadata { *; }

# AndroidX Startup discovers these entry points through manifest metadata, and
# the framework instantiates the provider and initializers reflectively.
-keep class androidx.startup.InitializationProvider { public <init>(); }
-keep class androidx.profileinstaller.ProfileInstallerInitializer { public <init>(); }
-keep class * implements androidx.startup.Initializer { public <init>(); }

# kotlinx.serialization (enable when the serialization plugin/runtime is added).
# Its consumer rules cover ordinary serializers. R8 full mode needs this block
# for @Serializable types with named companion objects in the app package.
#-keepattributes InnerClasses
#-if @kotlinx.serialization.Serializable class com.gexetr.coverkit.** {
#    static **$* *;
#}
#-keepnames class <1>$$serializer {
#    static <1>$$serializer INSTANCE;
#}
#-keepclasseswithmembers,allowshrinking,allowobfuscation,allowaccessmodification class com.gexetr.coverkit.** {
#    *;
#}

# Room (enable when Room is added). This is Room's full-mode-safe constructor
# rule; its artifact normally contributes the same consumer rule automatically.
#-keep class * extends androidx.room.RoomDatabase { void <init>(); }
#-dontwarn androidx.room.paging.**
#-dontwarn androidx.lifecycle.LiveData

# Retrofit / OkHttp (enable when those libraries are added). Current artifacts
# ship consumer rules; this block mirrors Retrofit's full-mode reflection rules.
#-keepattributes Signature,InnerClasses,EnclosingMethod
#-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
#-keepattributes AnnotationDefault
#-keepclassmembers,allowshrinking,allowobfuscation interface * {
#    @retrofit2.http.* <methods>;
#}
#-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
#-dontwarn javax.annotation.**
#-dontwarn kotlin.Unit
#-dontwarn retrofit2.KotlinExtensions
#-dontwarn retrofit2.KotlinExtensions$*
#-if interface * { @retrofit2.http.* <methods>; }
#-keep,allowobfuscation interface <1>
#-if interface * { @retrofit2.http.* <methods>; }
#-keep,allowobfuscation interface * extends <1>
#-keep,allowoptimization,allowshrinking,allowobfuscation class kotlin.coroutines.Continuation
#-if interface * { @retrofit2.http.* public *** *(...); }
#-keep,allowoptimization,allowshrinking,allowobfuscation class <3>
#-keep,allowoptimization,allowshrinking,allowobfuscation class retrofit2.Response
# OkHttp needs no app-side keep rule beyond its bundled consumer rules.

# Compose is compiler-generated and needs no blanket keep rules.

# These can improve shrinking, but enable them only after on-device regression tests.
#-allowaccessmodification
#-repackageclasses
