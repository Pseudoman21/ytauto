package com.pseudoman21.ytauto.car;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.ViewGroup;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarUiController;
import com.pseudoman21.ytauto.About;
import com.pseudoman21.ytauto.Player;
import com.pseudoman21.ytauto.R;
import com.pseudoman21.ytauto.YtWebView;

/**
 * The screen Android Auto shows on the car display: the YouTube page, nothing else.
 */
public class CarMainActivity extends CarActivity implements YtWebView.TextInput {
	private Player player;
	private About about;
	private CarEditText editText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setIgnoreConfigChanges(0xFFFFFFFF);
		CarUiController ctrl = getCarUiController();
		ctrl.getStatusBarController().hideAppHeader();
		ctrl.getMenuController().hideMenuButton();
		setContentView(R.layout.player);
		player = new Player((YtWebView) findViewById(R.id.web),
				(ViewGroup) findViewById(R.id.fullscreen), this, true);
		about = new About(findViewById(R.id.web).getRootView());
	}

	@Override
	public void onDestroy() {
		stopInput();
		player.destroy();
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		if (!about.onBack() && !player.onBack()) super.onBackPressed();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (player.onMediaKey(event)) return true;
		if ((keyCode == KeyEvent.KEYCODE_BACK) && (about.onBack() || player.onBack())) return true;
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean isInputActive() {
		return a().isInputActive();
	}

	@Override
	public void startInput(String text, YtWebView target) {
		if (editText == null) editText = new CarEditText(this);
		editText.bind(target, text, this::stopInput);
		a().startInput(editText);
	}

	@Override
	public void stopInput() {
		if (editText != null) editText.unbind();
		if (a().isInputActive()) a().stopInput();
	}
}
