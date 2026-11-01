package com.bijoy.keyboard

/** বিজয় লেআউটের লজিক। Android-নিরপেক্ষ, তাই আলাদা রাখা হয়েছে। */
class BijoyEngine {
    /** কার্সরের আগের [del] অক্ষর মুছে [text] বসাতে হবে */
    data class Edit(val del: Int, val text: String)

    private var pend: String? = null   // অপেক্ষমাণ ি / ে / ৈ
    private var base = false           // কারের জন্য ব্যঞ্জন বসেছে কি না
    private var link = false           // হসন্ত বসেছে, পরের ব্যঞ্জন যুক্ত হবে

    fun reset() { pend = null; base = false; link = false }

    /** null মানে এই কী-এর ম্যাপ নেই, অ্যাপকেই সামলাতে দিন */
    fun handle(ch: Char, before: String): Edit? {
        val m = MAP[ch] ?: run { reset(); return null }

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
        return Edit(0, m)
    }

    companion object {
        const val CONS = "wWeEryYuUiIoOpPtTRhHjJkKlLvVbBnNmM;"
        val MAP: Map<Char, String> = mapOf(
            'q' to "ঙ", 'Q' to "ং", 'w' to "য", 'W' to "য়", 'e' to "ড", 'E' to "ঢ",
            'r' to "প", 'R' to "ফ", 't' to "ট", 'T' to "ঠ", 'y' to "চ", 'Y' to "ছ",
            'u' to "জ", 'U' to "ঝ", 'i' to "হ", 'I' to "ঞ", 'o' to "গ", 'O' to "ঘ",
            'p' to "ড়", 'P' to "ঢ়", 'a' to "ৃ", 'A' to "র্", 's' to "ু", 'S' to "ূ",
            'd' to "ি", 'D' to "ী", 'f' to "া", 'F' to "অ", 'g' to "্", 'G' to "।",
            'h' to "ব", 'H' to "ভ", 'j' to "ক", 'J' to "খ", 'k' to "ত", 'K' to "থ",
            'l' to "দ", 'L' to "ধ", ';' to "স", ':' to "ঃ", 'z' to "্র", 'Z' to "্য",
            'x' to "ও", 'X' to "ৗ", 'c' to "ে", 'C' to "ৈ", 'v' to "র", 'V' to "ল",
            'b' to "ন", 'B' to "ণ", 'n' to "স", 'N' to "ষ", 'm' to "ম", 'M' to "শ",
            '^' to "ঁ", '`' to "ৎ",
            '0' to "০", '1' to "১", '2' to "২", '3' to "৩", '4' to "৪",
            '5' to "৫", '6' to "৬", '7' to "৭", '8' to "৮", '9' to "৯"
        )
    }
}
