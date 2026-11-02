# ম্যানিফেস্টে উল্লেখিত Activity/Service স্বয়ংক্রিয়ভাবে R8 রক্ষা করে।
# এখানে অতিরিক্ত কোনো ক্লাস/মেথড রাখার দরকার নেই — বাকি সব কোড
# সর্বোচ্চ obfuscate ও shrink হবে।

-dontusemixedcaseclassnames
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
