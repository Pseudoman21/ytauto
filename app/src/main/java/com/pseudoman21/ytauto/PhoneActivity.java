package com.pseudoman21.ytauto;

import android.os.Bundle;
import android.view.KeyEvent;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Phone screen. Mostly for signing in to YouTube: the car and the phone share one cookie store.
 */
public class PhoneActivity extends AppCompatActivity {
	private Player player;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.player);
		player = new Player(findViewById(R.id.web), findViewById(R.id.fullscreen), null, false);
		getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				if (player.onBack()) return;
				setEnabled(false);
				getOnBackPressedDispatcher().onBackPressed();
			}
		});
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return player.onMediaKey(event) || super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onDestroy() {
		player.destroy();
		super.onDestroy();
	}
}
