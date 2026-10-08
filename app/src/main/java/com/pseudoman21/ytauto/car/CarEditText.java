package com.pseudoman21.ytauto.car;

import android.content.Context;
import android.support.car.input.CarRestrictedEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.Nullable;

import com.google.android.gms.car.input.CarEditable;
import com.google.android.gms.car.input.CarEditableListener;
import com.pseudoman21.ytauto.YtWebView;

/**
 * Off-screen text field that the Android Auto keyboard types into; every change is mirrored into
 * the focused element of the page.
 */
class CarEditText extends CarRestrictedEditText implements CarEditable, TextWatcher {
	@Nullable
	private YtWebView target;
	@Nullable
	private Runnable onDone;

	CarEditText(Context context) {
		super(context, null);
		setSingleLine();
		setImeOptions(EditorInfo.IME_ACTION_SEARCH);
		addTextChangedListener(this);
	}

	void bind(YtWebView target, String text, Runnable onDone) {
		this.target = null; // don't echo the initial text back to the page
		setText(text);
		setSelection(text.length());
		this.target = target;
		this.onDone = onDone;
	}

	void unbind() {
		target = null;
		onDone = null;
	}

	@Override
	public void setCarEditableListener(CarEditableListener listener) {
		super.setCarEditableListener(listener::onUpdateSelection);
	}

	@Override
	public void onEditorAction(int actionCode) {
		switch (actionCode) {
			case EditorInfo.IME_ACTION_GO, EditorInfo.IME_ACTION_SEARCH, EditorInfo.IME_ACTION_SEND,
					EditorInfo.IME_ACTION_NEXT, EditorInfo.IME_ACTION_DONE -> {
				YtWebView t = target;
				Runnable done = onDone;
				if (t != null) t.submitInput();
				if (done != null) done.run();
			}
			default -> super.onEditorAction(actionCode);
		}
	}

	@Override
	public void afterTextChanged(Editable s) {
		if (target != null) target.setInputText(s.toString());
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}
}
