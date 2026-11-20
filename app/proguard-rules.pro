# Project-specific R8 rules for the release build.
#
# No keep rules for libraries are needed here: kotlinx.serialization, Hilt, Compose and the
# other AndroidX libraries ship their own consumer rules inside their artifacts. The
# @Serializable DTOs of the data layer need no rules either, because the serialization
# plugin generates their serializers at compile time and JSON field names come from the
# serial descriptors, not from class or field names, so renaming them is safe.
#
# If a release build ever crashes where the debug build does not, a missing keep rule is the
# first suspect. Add it here with a comment that says which class needs it and why.

# Keep file names and line numbers so that crash stack traces can be decoded with the
# mapping file (app/build/outputs/mapping/release/mapping.txt).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
