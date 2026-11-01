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

    override fun onCreateInputView(): View = TextView(this).apply {
        text = "ফিজিক্যাল কিবোর্ড লাগিয়ে লিখুন"
        gravity = Gravity.CENTER
        setPadding(24, 32, 24, 32)
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
