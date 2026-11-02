package com.bijoy.keyboard

import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.Gravity
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast

class BijoyIME : InputMethodService() {
    private val engine = BijoyEngine()
    private var bangla = true
    private val consumed = HashSet<Int>()
    private lateinit var statusView: TextView

    /** এই মুহূর্তে বাংলায় রূপান্তর করা অনুমোদিত কিনা (পেইডে লাইসেন্স, ট্রায়ালে সময় বাকি থাকা লাগে) */
    private fun allowed(): Boolean =
        if (BuildConfig.IS_PAID) License.isLicensed(this) else !Trial.isExpired(this)

    private fun statusText(): String = when {
        BuildConfig.IS_PAID && !License.isLicensed(this) ->
            "লাইসেন্স সক্রিয় নেই\nঅ্যাপ খুলে লাইসেন্স কোডটি বসান"
        !BuildConfig.IS_PAID && Trial.isExpired(this) ->
            "ট্রায়াল শেষ\nসম্পূর্ণ সংস্করণের জন্য অ্যাপ খুলুন"
        else -> "ফিজিক্যাল কিবোর্ড লাগিয়ে লিখুন"
    }

    private fun refreshStatus() {
        if (::statusView.isInitialized) statusView.text = statusText()
    }

    override fun onCreateInputView(): View {
        statusView = TextView(this).apply {
            text = statusText()
            gravity = Gravity.CENTER
            setPadding(24, 32, 24, 32)
        }
        return statusView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (!BuildConfig.IS_PAID) Trial.startSession(this)
        refreshStatus()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        if (!BuildConfig.IS_PAID) Trial.endSession(this)
        super.onFinishInputView(finishingInput)
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        engine.reset()
    }

    /** পাসওয়ার্ড, সংখ্যা, ফোন ইত্যাদি ঘরে রূপান্তর বন্ধ */
    private fun skipField(): Boolean {
        val t = currentInputEditorInfo?.inputType ?: return true
        val cls = t and InputType.TYPE_MASK_CLASS
        val v = t and InputType.TYPE_MASK_VARIATION
        if (cls == InputType.TYPE_NULL || cls == InputType.TYPE_CLASS_NUMBER ||
            cls == InputType.TYPE_CLASS_PHONE || cls == InputType.TYPE_CLASS_DATETIME) return true
        return cls == InputType.TYPE_CLASS_TEXT && (
            v == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            v == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            v == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            v == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val ic = currentInputConnection ?: return super.onKeyDown(keyCode, event)

        if (event.isCtrlPressed && event.isAltPressed && keyCode == KeyEvent.KEYCODE_B) {
            bangla = !bangla; engine.reset()
            Toast.makeText(this, if (bangla) "বাংলা" else "English", Toast.LENGTH_SHORT).show()
            consumed.add(keyCode); return true
        }
        if (!bangla || skipField()) return super.onKeyDown(keyCode, event)

        if (!allowed()) {
            // ট্রায়াল শেষ অথবা লাইসেন্স সক্রিয় নয় — ইংরেজিই থাকুক, রূপান্তর বন্ধ
            refreshStatus()
            return super.onKeyDown(keyCode, event)
        }
        if (!BuildConfig.IS_PAID) {
            // প্রতিটি কী-চাপে সময় আপডেট করে নেওয়া, যাতে ৭ মিনিট পার হওয়া মাত্রই বন্ধ হয়ে যায়
            Trial.endSession(this)
            if (Trial.isExpired(this)) {
                refreshStatus()
                return super.onKeyDown(keyCode, event)
            }
            Trial.startSession(this)
        }

        if (event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) {
            engine.reset(); return super.onKeyDown(keyCode, event)
        }
        val c = event.getUnicodeChar(event.metaState)
        if (c == 0 || (c and KeyCharacterMap.COMBINING_ACCENT) != 0) {
            engine.reset(); return super.onKeyDown(keyCode, event)
        }
        val before = ic.getTextBeforeCursor(2, 0)?.toString() ?: ""
        val edit = engine.handle(c.toChar(), before) ?: return super.onKeyDown(keyCode, event)

        ic.beginBatchEdit()
        if (edit.del > 0) ic.deleteSurroundingText(edit.del, 0)
        ic.commitText(edit.text, 1)
        ic.endBatchEdit()
        consumed.add(keyCode)
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (consumed.remove(keyCode)) return true
        return super.onKeyUp(keyCode, event)
    }
}
