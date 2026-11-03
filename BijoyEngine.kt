package com.bijoy.keyboard

/**
 * (বিজয় ২০২৪ নির্দেশিকা অনুযায়ী আপডেট: প্রাচীন বর্ণ, ৳, উদ্ধৃতি চিহ্ন, ॥, র‍্য, ঙ-এর আগে ি/ে/ৈ ঠিক করা হয়েছে)
 *
 * বিজয় ৫২ (সরকারি চাকরি পরীক্ষায় ব্যবহৃত আসল লেআউট) অনুযায়ী লজিক। Android-নিরপেক্ষ, তাই আলাদা রাখা হয়েছে।
 *
 * রেফারেন্স (একাধিক স্বাধীন সূত্র মিলিয়ে যাচাই করা):
 * - স্বরবর্ণ: অ=Shift+F, আ=G+F, ই=G+D, ঈ=G+Shift+D, উ=G+S, ঊ=G+Shift+S,
 *   ঋ=G+A, এ=G+C, ঐ=G+Shift+C, ও=X, ঔ=G+Shift+X
 * - সংশোধক: ৎ=\ (backslash), ং=Shift+Q, ঃ=Shift+\ , ঁ=Shift+7(&), ্=G, দাঁড়ি(।)=Shift+G
 * - ফলা: ্র=Z, ্য=Shift+Z, রেফ(র্)=Shift+A
 */
class BijoyEngine {
    /** কার্সরের আগের [del] অক্ষর মুছে [text] বসাতে হবে */
    data class Edit(val del: Int, val text: String)

    private var pend: String? = null   // অপেক্ষমাণ ি / ে / ৈ
    private var base = false           // কারের জন্য ব্যঞ্জন বসেছে কি না
    private var link = false           // হসন্ত বসেছে, পরের ব্যঞ্জন যুক্ত হবে

    fun reset() { pend = null; base = false; link = false }

    /** null মানে এই কী-এর ম্যাপ নেই, অ্যাপকেই সামলাতে দিন */
    fun handle(ch: Char, before: String): Edit? {
        // G (্) এর পরপরই বিশেষ কী (প্রাচীন বর্ণ, আনা চিহ্ন, ×, ÷, ॥ ইত্যাদি) — MAP-এর আগেই দেখতে হবে
        if (before.lastOrNull() == '্') {
            val sp = AFTER_HOSANTO_SPECIAL[ch]
            if (sp != null) { reset(); return Edit(1, sp) }
        }

        val m = MAP[ch] ?: run { reset(); return null }

        // G (্) এর পরপরই একটা "কার" কী চাপলে সেটা হসন্ত না হয়ে স্বরবর্ণ হয়ে যায়
        // যেমন: G তারপর F = আ (শুধু "অ্‌া" নয়, সত্যিকারের "আ" অক্ষর)
        if (before.lastOrNull() == '্') {
            val vowel = INDEPENDENT_VOWEL_AFTER_HOSANTO[ch]
            if (vowel != null) { reset(); return Edit(1, vowel) }
        }

        // ে + া = ো , ে + ৗ = ৌ
        if (before.lastOrNull() == 'ে' && (ch == 'f' || ch == 'X')) {
            reset(); return Edit(1, if (ch == 'f') "ো" else "ৌ")
        }

        // ব্যঞ্জনের আগে টাইপ হয় এমন কার
        if (ch == 'd' || ch == 'c' || ch == 'C') {
            pend = m; base = false; link = false; return Edit(0, m)
        }
        val v = pend
        if (v != null) {
            if (before.endsWith(v)) {
                if (ch == 'g' || ch == 'z' || ch == 'Z') {
                    if (base) { link = (ch == 'g'); return Edit(v.length, m + v) }
                } else if (ch in CONS && (!base || link)) {
                    base = true; link = false; return Edit(v.length, m + v)
                }
            }
            reset()
        }
        // র-এর পর য-ফলা (Shift+Z): ZWJ না দিলে ইউনিকোডে রেফের মতো দেখায় (র‍্যাব, র‍্যালি)
        if (ch == 'Z' && before.lastOrNull() == 'র' &&
            (before.length < 2 || before[before.length - 2] != '্')) {
            return Edit(0, "\u200D" + m)
        }
        return Edit(0, m)
    }

    companion object {
        const val CONS = "qwWeEryYuUiIoOpPtTRhHjJkKlLvVbBnNmM"

        /** G (হসন্ত) চাপার ঠিক পরে এই কী-গুলোর একটা চাপলে যে স্বরবর্ণ হয় */
        val INDEPENDENT_VOWEL_AFTER_HOSANTO: Map<Char, String> = mapOf(
            'f' to "আ", 'd' to "ই", 'D' to "ঈ", 's' to "উ", 'S' to "ঊ",
            'a' to "ঋ", 'c' to "এ", 'C' to "ঐ", 'x' to "ও", 'X' to "ঔ"
        )

        /**
         * G (হসন্ত) চাপার ঠিক পরে এই কী চাপলে হসন্তের জায়গায় এই বিশেষ চিহ্ন বসে
         * (বিজয় ২০২৪ নির্দেশিকা, পৃষ্ঠা ১১–১২: প্রাচীন বর্ণ ও অসমিয়া বর্ণ)
         */
        val AFTER_HOSANTO_SPECIAL: Map<Char, String> = mapOf(
            // আনা চিহ্ন ও প্রাচীন চিহ্ন
            '1' to "\u09F4", '2' to "\u09F5", '3' to "\u09F6", '4' to "\u09F7",   // ৴ ৵ ৶ ৷  (এক, দুই, তিন, চার আনা)
            '5' to "\u09F8", '0' to "\u09F8",                                         // ৸ (আট আনা)
            '6' to "\u09FB",                                                           // ৻ গেন্ডা মার্ক
            '7' to "\u09FA",                                                           // ৺ ঈশ্বর
            '8' to "\u09F2",                                                           // ৲ টাকা চিহ্ন
            '9' to "\u098C",                                                           // ঌ লী
            '-' to "\u00D7", '_' to "\u00F7",                                         // × গুণ, ÷ ভাগ

            // Shift + সংখ্যা
            '!' to "\u0980",                                                           // ঀ আনজি
            ')' to "\u00B0", '@' to "\u00B0",                                         // ° ডিগ্রি
            '#' to "\u09BD",                                                           // ঽ অভাগ্রহ
            '$' to "\u09F2",                                                           // ৲ টাকা চিহ্ন
            '^' to "\u09E0",                                                           // ৠ ঋ বর্ণ ও কার
            '&' to "\u09FC",                                                           // ৼ বৈদিক অনুস্বার
            '*' to "\u09E3",                                                           // ৣ দ্বৈত লী
            '(' to "\u09E2",                                                           // ৢ লী

            // অসমিয়া বর্ণ
            ',' to "\u09F1", '.' to "\u09F0",                                         // ৱ অসমিয়া ব, ৰ অসমিয়া র

            // দুই দাঁড়ি
            'G' to "\u0965"                                                            // ॥
        )

        val MAP: Map<Char, String> = mapOf(
            // স্বরচিহ্ন (কার) ও স্বরবর্ণ
            'a' to "ৃ", 'A' to "র্", 's' to "ু", 'S' to "ূ",
            'd' to "ি", 'D' to "ী", 'f' to "া", 'F' to "অ",
            'c' to "ে", 'C' to "ৈ", 'x' to "ও", 'X' to "ৗ",

            // ব্যঞ্জনবর্ণ
            'q' to "ঙ", 'w' to "য", 'W' to "য়", 'e' to "ড", 'E' to "ঢ",
            'r' to "প", 'R' to "ফ", 't' to "ট", 'T' to "ঠ", 'y' to "চ", 'Y' to "ছ",
            'u' to "জ", 'U' to "ঝ", 'i' to "হ", 'I' to "ঞ", 'o' to "গ", 'O' to "ঘ",
            'p' to "ড়", 'P' to "ঢ়", 'h' to "ব", 'H' to "ভ", 'j' to "ক", 'J' to "খ",
            'k' to "ত", 'K' to "থ", 'l' to "দ", 'L' to "ধ",
            'v' to "র", 'V' to "ল", 'b' to "ন", 'B' to "ণ", 'n' to "স", 'N' to "ষ",
            'm' to "ম", 'M' to "শ",

            // ফলা ও হসন্ত
            'g' to "্", 'G' to "।", 'z' to "্র", 'Z' to "্য",

            // সংশোধক বর্ণ — ৎ ও ঃ ব্যাকস্ল্যাশে, ঁ শিফট+৭ (&)
            'Q' to "ং", '&' to "ঁ", '\\' to "ৎ", '|' to "ঃ",

            // টাকা চিহ্ন (Shift+4) ও উদ্ধৃতি চিহ্ন
            '$' to "৳", '`' to "‘", '~' to "“", '\'' to "’", '"' to "”",

            // সংখ্যা
            '0' to "০", '1' to "১", '2' to "২", '3' to "৩", '4' to "৪",
            '5' to "৫", '6' to "৬", '7' to "৭", '8' to "৮", '9' to "৯"
        )
    }
}
