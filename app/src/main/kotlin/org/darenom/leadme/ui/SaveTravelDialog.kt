package org.darenom.leadme.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import org.darenom.leadme.BuildConfig
import org.darenom.leadme.R


/**
 * Created by admadmin on 01/03/2018.
 */

class SaveTravelDialog : DialogFragment() {

    interface SaveTravelDialogListener {
        fun onCancel()

        fun onKeyListener(name: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context !is SaveTravelDialogListener) {
            throw ClassCastException(context.toString() + " must implement SaveTravelDialogListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val input = EditText(context)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setSingleLine(true)
        input.maxEms = 20

        return AlertDialog.Builder(context!!)
                .setTitle(R.string.app_name)
                .setMessage(R.string.file_save)
                .setView(input)

                .setOnCancelListener { (context as SaveTravelDialogListener).onCancel() }

                .setOnKeyListener { dialog, keyCode, event ->
                    if ((event != null && (event.keyCode == KeyEvent.KEYCODE_ENTER)) ||
                            (keyCode == EditorInfo.IME_ACTION_NEXT) ||
                            (keyCode == EditorInfo.IME_ACTION_DONE)) {

                        when {
                            input.text.toString().isEmpty() -> Toast.makeText(context, getString(R.string.no_name), Toast.LENGTH_SHORT).show()
                            input.text.toString().contentEquals(BuildConfig.TMP_NAME) -> Toast.makeText(context, getString(R.string.same_name), Toast.LENGTH_SHORT).show()
                            else -> {
                                var tmp = input.text.toString().replace("[^a-zA-Z0-9]".toRegex(), "")
                                tmp = tmp.substring(0, if (tmp.length > 20) 20 else tmp.length)
                                (context as SaveTravelDialogListener).onKeyListener(tmp)
                            }
                        }
                        (context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                            .hideSoftInputFromWindow(input.windowToken, 0)
                        dialog.dismiss()
                    }
                    false
                }
                .create()
    }
}