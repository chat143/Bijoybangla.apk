package com.bijoy.keyboard

import android.content.Context
import android.provider.Settings
import android.util.Base64
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * পেইড ভার্সনের লাইসেন্স যাচাই।
 *
 * এই ফাইলে শুধু "পাবলিক" (যাচাই করার) চাবি থাকে — কোড বানানোর "প্রাইভেট" চাবি
 * এই অ্যাপে কোথাও নেই, তাই অ্যাপ ডিকম্পাইল করলেও নতুন লাইসেন্স কোড বানানো যাবে না।
 *
 * লাইসেন্স কোড = RSA(SHA-256) স্বাক্ষর, বানানো হয় ডিভাইস আইডি-র উপর — অর্থাৎ একটা
 * কোড শুধু ঠিক সেই ডিভাইসেই কাজ করবে যার আইডি দিয়ে কোডটা বানানো হয়েছিল।
 */
object License {

    // পাবলিক কী (X.509 / DER, base64) — শুধু যাচাই করতে পারে, কোড বানাতে পারে না
    private const val PUBLIC_KEY_B64 =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA07HOr8MKTPjwT69cn4cM" +
        "oqfeWywAQy9dYdPv9lJ5hyCLAY1U3VnaEYvHViKWQNCKuOmRssi+KheOZ00Ic2Ab" +
        "S/FSiKh/gee4EWbNoGfGto+W1+u4sNP43/TylG4U6H8JGA6mrtY6vZ+FDOdrXTVT" +
        "tYkdTAOwf5KNN1fP71OR1xOIscpjQJFjDDgF+/5Gkm4ceSObiGjxORpzEgI3HZev" +
        "+y+MJr61OdMHtCs6RLWmasiJkZ3ksrKEskhDve4mcntaglaCIpIbtXWsHVMpOcTY" +
        "Wi6fQlXk5a2Y0A4etCsK0AhQpd/GXaXPOhxI4VwfgEVlVYvsGNgtzrX0ELtEuE1d" +
        "YQIDAQAB"

    private const val PREFS = "bijoy_license"
    private const val KEY_CODE = "code"

    /** ডিভাইস আইডি, বড় হরফে ৪টি করে ভাগে — স্ক্রিনে যা দেখানো/কপি হয় আর যার ওপর কোড বানানো হয়,
     *  দুটোই ঠিক এই একই লেখা, যাতে কপি-পেস্ট করলে ভুল না হয় */
    fun deviceId(ctx: Context): String {
        val raw = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWNDEVICEID"
        return raw.uppercase().chunked(4).joinToString("-")
    }

    /** নির্দিষ্ট ডিভাইস আইডির জন্য দেওয়া কোডটি সত্যিকারের কিনা যাচাই করে */
    private fun verify(deviceId: String, rawCode: String): Boolean {
        return try {
            val clean = rawCode.trim().replace("\\s".toRegex(), "")
            val sigBytes = try {
                Base64.decode(clean, Base64.NO_WRAP)
            } catch (e: IllegalArgumentException) {
                Base64.decode(clean, Base64.URL_SAFE or Base64.NO_WRAP)
            }
            val keyBytes = Base64.decode(PUBLIC_KEY_B64, Base64.NO_WRAP)
            val pubKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(keyBytes))
            val sig = Signature.getInstance("SHA256withRSA")
            sig.initVerify(pubKey)
            sig.update(deviceId.toByteArray(Charsets.UTF_8))
            sig.verify(sigBytes)
        } catch (e: Exception) {
            false
        }
    }

    /** ইউজার যা পেস্ট করেছে তা এই ডিভাইসের জন্য বৈধ হলে সংরক্ষণ করে true ফেরত দেয় */
    fun activate(ctx: Context, enteredCode: String): Boolean {
        if (!verify(deviceId(ctx), enteredCode)) return false
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_CODE, enteredCode.trim()).apply()
        return true
    }

    /** আগে সংরক্ষিত কোড এখনো এই ডিভাইসের জন্য বৈধ কিনা — প্রতিবার সত্যিকারের স্বাক্ষর যাচাই করেই বলা হয়,
     *  শুধু একটা true/false ফ্ল্যাগের ওপর ভরসা করা হয় না */
    fun isLicensed(ctx: Context): Boolean {
        val saved = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_CODE, null) ?: return false
        return verify(deviceId(ctx), saved)
    }
}

/**
 * ট্রায়াল ভার্সনের সময় গোনা।
 *
 * ফোনের ঘড়ি (তারিখ/সময়) পেছনে বা সামনে ঘোরালেও এতে কোনো প্রভাব পড়ে না, কারণ এটা
 * System.currentTimeMillis() নয়, SystemClock.elapsedRealtime() ব্যবহার করে — যা বুট
 * হওয়ার পর থেকে একটানা বাড়ে, ইউজার বদলাতে পারে না। মোট ব্যবহৃত সময়টুকু প্রেফারেন্সে
 * জমা থাকে, তাই ফোন রিস্টার্ট করলেও আগের জমা সময় মুছে যায় না।
 */
object Trial {
    const val LIMIT_MS = 7 * 60 * 1000L

    private const val PREFS = "bijoy_trial"
    private const val KEY_TOTAL = "total_ms"
    private const val KEY_SESSION_START = "session_start_elapsed"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun totalMs(ctx: Context): Long = prefs(ctx).getLong(KEY_TOTAL, 0L)

    fun remainingMs(ctx: Context): Long = (LIMIT_MS - totalMs(ctx)).coerceAtLeast(0L)

    fun isExpired(ctx: Context): Boolean = totalMs(ctx) >= LIMIT_MS

    /** কিবোর্ড সুইচ চালু/স্ক্রিনে আসা অবস্থায় গোনা শুরু */
    fun startSession(ctx: Context) {
        if (isExpired(ctx)) return
        prefs(ctx).edit()
            .putLong(KEY_SESSION_START, android.os.SystemClock.elapsedRealtime())
            .apply()
    }

    /** কিবোর্ড সুইচ বন্ধ/সরে যাওয়ায় যতটুকু সময় গেল তা মোটের সাথে যোগ করা */
    fun endSession(ctx: Context) {
        val p = prefs(ctx)
        val start = p.getLong(KEY_SESSION_START, -1L)
        if (start < 0) return
        val elapsed = android.os.SystemClock.elapsedRealtime() - start
        val newTotal = (totalMs(ctx) + elapsed.coerceAtLeast(0L)).coerceAtMost(LIMIT_MS)
        p.edit().putLong(KEY_TOTAL, newTotal).remove(KEY_SESSION_START).apply()
    }
}
