package com.pseudoman21.ytauto;

import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

/**
 * Wires a {@link YtWebView} to a MediaSession, so steering wheel buttons, the notification and
 * the Android Auto media controls drive the page's video. Replaces Fermata's YoutubeMediaEngine.
 */
public final class Player implements YtWebView.Listener {
	private static final long ACTIONS = PlaybackStateCompat.ACTION_PLAY |
			PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_PLAY_PAUSE |
			PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SEEK_TO |
			PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
	private final YtWebView web;
	private final MediaSessionCompat session;
	private final boolean autoFullScreen;
	private String videoUrl;
	private String title;
	private long duration;

	/**
	 * @param textInput      car keyboard proxy, or null on the phone
	 * @param autoFullScreen switch to fullscreen whenever a new video starts
	 */
	public Player(YtWebView web, ViewGroup fullScreenView, @Nullable YtWebView.TextInput textInput,
				 boolean autoFullScreen) {
		this.web = web;
		this.autoFullScreen = autoFullScreen;
		session = new MediaSessionCompat(web.getContext().getApplicationContext(), YtWebView.TAG);
		session.setCallback(new MediaSessionCompat.Callback() {
			@Override
			public void onPlay() {
				web.play();
			}

			@Override
			public void onPause() {
				web.pause();
			}

			@Override
			public void onStop() {
				web.stop();
			}

			@Override
			public void onSeekTo(long pos) {
				web.seekTo(pos);
			}

			@Override
			public void onSkipToNext() {
				web.next();
			}

			@Override
			public void onSkipToPrevious() {
				web.prev();
			}
		});
		web.init(this, fullScreenView, textInput, textInput != null);
		web.loadUrl(YtWebView.HOME);
	}

	@Override
	public void onVideoEvent(int event, @NonNull JSONObject info) {
		long pos = (long) (info.optDouble("t", 0) * 1000);
		boolean ad = info.optBoolean("ad");

		if (!ad) {
			String t = info.optString("title");
			long d = (long) (info.optDouble("d", 0) * 1000);
			if (!t.equals(title) || (d != duration)) {
				title = t;
				duration = d;
				session.setMetadata(new MediaMetadataCompat.Builder()
						.putString(MediaMetadataCompat.METADATA_KEY_TITLE, t)
						.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, d)
						.build());
			}
		}

		switch (event) {
			case YtWebView.EV_PLAYING -> {
				session.setActive(true);
				setState(PlaybackStateCompat.STATE_PLAYING, pos);
				String url = info.optString("url");
				if (!ad && !url.equals(videoUrl)) {
					videoUrl = url;
					if (autoFullScreen && url.contains("/watch")) web.enterFullScreen();
				}
			}
			case YtWebView.EV_PAUSED -> setState(PlaybackStateCompat.STATE_PAUSED, pos);
			case YtWebView.EV_ENDED -> setState(PlaybackStateCompat.STATE_STOPPED, pos);
		}
	}

	private void setState(int state, long pos) {
		float speed = (state == PlaybackStateCompat.STATE_PLAYING) ? 1f : 0f;
		session.setPlaybackState(new PlaybackStateCompat.Builder()
				.setActions(ACTIONS)
				.setState(state, pos, speed, SystemClock.elapsedRealtime())
				.build());
	}

	/** Back: leave fullscreen, then go back in history. Returns false when there is nothing to undo. */
	public boolean onBack() {
		if (web.exitFullScreen()) return true;
		if (web.canGoBack()) {
			web.goBack();
			return true;
		}
		return false;
	}

	public boolean onMediaKey(KeyEvent e) {
		switch (e.getKeyCode()) {
			case KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE,
					KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK,
					KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_PREVIOUS,
					KeyEvent.KEYCODE_MEDIA_STOP -> {
				return session.getController().dispatchMediaButtonEvent(e);
			}
			default -> {
				return false;
			}
		}
	}

	public void destroy() {
		session.release();
		web.destroy();
	}
}
