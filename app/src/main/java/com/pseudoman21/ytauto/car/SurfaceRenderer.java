package com.pseudoman21.ytauto.car;

import android.app.Presentation;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.SurfaceContainer;

import com.pseudoman21.ytauto.About;
import com.pseudoman21.ytauto.Player;
import com.pseudoman21.ytauto.R;
import com.pseudoman21.ytauto.YtWebView;

/**
 * Draws the YouTube page onto the head unit's surface: a private VirtualDisplay renders into the
 * surface, and a Presentation on that display holds the same layout as the phone. Taps and scrolls
 * from the car arrive as {@link SurfaceCallback} calls and are replayed as touch events.
 */
final class SurfaceRenderer implements SurfaceCallback {
	// A drag ends this long after the last scroll event, since the host reports no "finger up".
	private static final long DRAG_END_DELAY = 150;
	private final CarContext ctx;
	private final YtWebView.TextInput textInput;
	private final Handler handler = new Handler(Looper.getMainLooper());
	private final Runnable endDrag = this::endDrag;
	@Nullable
	private VirtualDisplay display;
	@Nullable
	private Screen screen;
	private long dragDownTime;
	private float dragX;
	private float dragY;

	SurfaceRenderer(CarContext ctx, YtWebView.TextInput textInput) {
		this.ctx = ctx;
		this.textInput = textInput;
	}

	@Nullable
	Player getPlayer() {
		return (screen == null) ? null : screen.player;
	}

	@Nullable
	About getAbout() {
		return (screen == null) ? null : screen.about;
	}

	@Override
	public void onSurfaceAvailable(@NonNull SurfaceContainer c) {
		if (c.getSurface() == null) return;
		if (display == null) {
			// Without VIRTUAL_DISPLAY_FLAG_PUBLIC the display is private to this app, so no permission
			// is needed and nothing else can show up on it.
			display = ctx.getSystemService(DisplayManager.class).createVirtualDisplay("YtAuto",
					c.getWidth(), c.getHeight(), c.getDpi(), c.getSurface(),
					DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY);
			screen = new Screen(display.getDisplay());
			screen.show();
		} else {
			// Same page as before (e.g. back from Maps): keep it, just point it at the new surface.
			display.resize(c.getWidth(), c.getHeight(), c.getDpi());
			display.setSurface(c.getSurface());
		}
	}

	@Override
	public void onSurfaceDestroyed(@NonNull SurfaceContainer c) {
		// Keep the page alive, so playback continues while another app is on the car screen.
		if (display != null) display.setSurface(null);
	}

	void destroy() {
		handler.removeCallbacks(endDrag);
		if (screen != null) {
			screen.player.destroy();
			screen.dismiss();
			screen = null;
		}
		if (display != null) {
			display.release();
			display = null;
		}
	}

	@Override
	public void onClick(float x, float y) {
		if (dragDownTime != 0) endDrag();
		long t = SystemClock.uptimeMillis();
		touch(t, t, MotionEvent.ACTION_DOWN, x, y);
		touch(t, t + 50, MotionEvent.ACTION_UP, x, y);
	}

	@Override
	public void onScroll(float distanceX, float distanceY) {
		long t = SystemClock.uptimeMillis();
		if (dragDownTime == 0) {
			// The host doesn't say where the finger is, so drag from the middle of the screen.
			View root = (screen == null) ? null : screen.root;
			if (root == null) return;
			dragDownTime = t;
			dragX = root.getWidth() / 2f;
			dragY = root.getHeight() / 2f;
			touch(t, t, MotionEvent.ACTION_DOWN, dragX, dragY);
		}
		dragX -= distanceX;
		dragY -= distanceY;
		touch(dragDownTime, t, MotionEvent.ACTION_MOVE, dragX, dragY);
		handler.removeCallbacks(endDrag);
		handler.postDelayed(endDrag, DRAG_END_DELAY);
	}

	private void endDrag() {
		handler.removeCallbacks(endDrag);
		if (dragDownTime == 0) return;
		touch(dragDownTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, dragX, dragY);
		dragDownTime = 0;
	}

	private void touch(long downTime, long time, int action, float x, float y) {
		if (screen == null) return;
		MotionEvent e = MotionEvent.obtain(downTime, time, action, x, y, 0);
		screen.root.dispatchTouchEvent(e);
		e.recycle();
	}

	private final class Screen extends Presentation {
		Player player;
		About about;
		View root;

		Screen(Display display) {
			super(ctx, display, R.style.AppTheme);
		}

		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.player);
			YtWebView web = findViewById(R.id.web);
			root = web.getRootView();
			player = new Player(web, findViewById(R.id.fullscreen), textInput, true);
			about = new About(root);
		}
	}
}
