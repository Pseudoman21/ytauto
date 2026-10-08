package com.pseudoman21.ytauto;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * m.youtube.com in a WebView, with an injected script that reports the page's &lt;video&gt;
 * state back to Java and lets Java drive it. Same approach as Fermata's YoutubeWebView,
 * without the addon/preferences framework.
 */
public class YtWebView extends WebView {
	static final String TAG = "YtAuto";
	public static final String HOME = "https://m.youtube.com";
	static final int EV_PLAYING = 1;
	static final int EV_PAUSED = 2;
	static final int EV_ENDED = 3;

	/** Receives video events from the page, on the main thread. */
	public interface Listener {
		void onVideoEvent(int event, @NonNull JSONObject info);
	}

	/** The car's keyboard. Android Auto does not show the phone IME, so text input is proxied. */
	public interface TextInput {
		boolean isInputActive();

		void startInput(String text, YtWebView target);

		void stopInput();
	}

	// Injected on every page load; the guard keeps it to one instance per document.
	// m.youtube.com is a single-page app, so the polling loop survives in-app navigation.
	private static final String PLAYER_JS = """
			(function() {
			  if (window.__ytAuto) return;
			  window.__ytAuto = true;
			  function info(v) {
			    return JSON.stringify({
			      t: v.currentTime || 0,
			      d: isFinite(v.duration) ? v.duration : 0,
			      title: document.title.replace(/ - YouTube$/, ''),
			      url: location.href,
			      ad: !!document.querySelector('.ad-showing')
			    });
			  }
			  function attach(v) {
			    if (v.__ytAuto) return;
			    v.__ytAuto = true;
			    var last = 0;
			    v.addEventListener('playing', function() { YtAuto.event(1, info(v)); });
			    v.addEventListener('pause', function() { YtAuto.event(2, info(v)); });
			    v.addEventListener('ended', function() { YtAuto.event(3, info(v)); });
			    v.addEventListener('seeked', function() { YtAuto.event(v.paused ? 2 : 1, info(v)); });
			    v.addEventListener('timeupdate', function() {
			      var now = Date.now();
			      if (!v.paused && (now - last > 5000)) { last = now; YtAuto.event(1, info(v)); }
			    });
			    if ((v.currentTime > 0) && !v.paused && !v.ended) YtAuto.event(1, info(v));
			  }
			  function skipAd() {
			    if (!document.querySelector('.ad-showing')) return;
			    var v = document.querySelector('video');
			    if (v && isFinite(v.duration)) v.currentTime = v.duration;
			    document.querySelectorAll('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button')
			      .forEach(function(b) { b.click(); });
			  }
			  setInterval(function() {
			    document.querySelectorAll('video').forEach(attach);
			    skipAd();
			  }, 1000);
			})();""";

	private static final String FOCUSED_TEXT_JS = """
			(function() {
			  var e = document.activeElement;
			  if (!e) return null;
			  if (e instanceof HTMLTextAreaElement) return e.value;
			  if (e instanceof HTMLInputElement &&
			      /^(text|search|email|url|tel|number|password|)$/.test(e.type)) return e.value;
			  if (e.isContentEditable) return e.innerText;
			  return null;
			})()""";

	private Listener listener;
	@Nullable
	private TextInput textInput;
	private boolean keepPlayingWhenHidden;
	private ViewGroup fullScreenView;
	private View customView;
	private WebChromeClient.CustomViewCallback customViewCallback;

	public YtWebView(Context context) {
		super(context);
	}

	public YtWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public YtWebView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@SuppressLint("SetJavaScriptEnabled")
	void init(Listener listener, ViewGroup fullScreenView, @Nullable TextInput textInput,
						boolean keepPlayingWhenHidden) {
		this.listener = listener;
		this.fullScreenView = fullScreenView;
		this.textInput = textInput;
		this.keepPlayingWhenHidden = keepPlayingWhenHidden;

		WebSettings s = getSettings();
		s.setJavaScriptEnabled(true);
		s.setDomStorageEnabled(true);
		s.setDatabaseEnabled(true);
		s.setMediaPlaybackRequiresUserGesture(false);
		s.setJavaScriptCanOpenWindowsAutomatically(true);
		s.setLoadWithOverviewMode(true);
		s.setUserAgentString(mobileUserAgent(s.getUserAgentString()));

		CookieManager.getInstance().setAcceptCookie(true);
		CookieManager.getInstance().setAcceptThirdPartyCookies(this, true);
		addJavascriptInterface(new Bridge(), "YtAuto");
		setWebViewClient(new Client());
		setWebChromeClient(new Chrome());
	}

	// region Player controls

	void play() {
		evaluateJavascript("var v = document.querySelector('video'); if (v) v.play();", null);
	}

	void pause() {
		evaluateJavascript("var v = document.querySelector('video'); if (v) v.pause();", null);
	}

	void stop() {
		evaluateJavascript(
				"var v = document.querySelector('video'); if (v) { v.pause(); v.currentTime = 0; }", null);
	}

	void seekTo(long ms) {
		evaluateJavascript("var v = document.querySelector('video'); if (v) v.currentTime = " +
				(ms / 1000.0) + ";", null);
	}

	void next() {
		prevNext(1);
	}

	void prev() {
		prevNext(0);
	}

	private void prevNext(int idx) {
		evaluateJavascript("var b = document.querySelectorAll(" +
				"'button.player-middle-controls-prev-next-button'); if (b[" + idx + "]) b[" + idx +
				"].click();", null);
	}

	/** Makes the YouTube player element fullscreen, which arrives at {@link Chrome#onShowCustomView}. */
	void enterFullScreen() {
		if (customView != null) return;
		// loadUrl("javascript:") counts as a user gesture, which requestFullscreen() needs.
		loadUrl("""
				javascript:(function() {
				  var p = document.querySelector('#player-container-id') ||
				    document.querySelector('.html5-video-player') || document.querySelector('video');
				  if (!p) return;
				  if (p.requestFullscreen) p.requestFullscreen();
				  else if (p.webkitRequestFullscreen) p.webkitRequestFullscreen();
				})()""");
	}

	boolean exitFullScreen() {
		if (customView == null) return false;
		fullScreenView.removeView(customView);
		fullScreenView.setVisibility(GONE);
		WebChromeClient.CustomViewCallback cb = customViewCallback;
		customView = null;
		customViewCallback = null;
		cb.onCustomViewHidden();
		return true;
	}

	boolean isFullScreen() {
		return customView != null;
	}

	// endregion

	// region Car text input

	/** Called by the car keyboard on each change. */
	public void setInputText(String text) {
		evaluateJavascript("(function(text) {\n" +
				"  var e = document.activeElement;\n" +
				"  if (!e) return;\n" +
				"  if (e.isContentEditable) e.innerText = text;\n" +
				"  else {\n" +
				"    var proto = (e instanceof HTMLTextAreaElement) ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;\n" +
				"    Object.getOwnPropertyDescriptor(proto, 'value').set.call(e, text);\n" +
				"  }\n" +
				"  e.dispatchEvent(new InputEvent('input', {bubbles: true, data: text, inputType: 'insertText'}));\n" +
				"  e.dispatchEvent(new Event('change', {bubbles: true}));\n" +
				"})(" + JSONObject.quote(text) + ");", null);
	}

	/** Called when the car keyboard's search/done key is pressed. */
	public void submitInput() {
		evaluateJavascript("""
				(function() {
				  var e = document.activeElement;
				  if (!e) return;
				  var text = (e.value !== undefined) ? e.value : e.innerText;
				  if ((e.type === 'search') || /search/i.test((e.className || '') + (e.name || '') + (e.placeholder || ''))) {
				    location.href = '/results?search_query=' + encodeURIComponent(text);
				    return;
				  }
				  ['keydown', 'keyup'].forEach(function(t) {
				    e.dispatchEvent(new KeyboardEvent(t, {code: 'Enter', key: 'Enter', keyCode: 13, view: window, bubbles: true}));
				  });
				})()""", null);
	}

	private void checkTextInput() {
		TextInput in = textInput;
		if ((in == null) || in.isInputActive()) return;
		// Give the page time to move focus to the tapped element.
		postDelayed(() -> evaluateJavascript(FOCUSED_TEXT_JS, r -> {
			if ((r == null) || "null".equals(r)) return;
			try {
				in.startInput(new JSONArray("[" + r + "]").getString(0), this);
			} catch (JSONException ex) {
				Log.w(TAG, "Unexpected focused text: " + r, ex);
			}
		}), 500);
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_UP) checkTextInput();
		return super.onTouchEvent(event);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		// Tapping the page while the car keyboard is up dismisses it.
		if ((textInput != null) && textInput.isInputActive()) {
			textInput.stopInput();
			return true;
		}
		return super.onInterceptTouchEvent(ev);
	}

	// endregion

	@Override
	protected void onWindowVisibilityChanged(int visibility) {
		// In the car, the window is hidden when the user switches to Maps etc. Pretending it is still
		// visible keeps YouTube from pausing on 'visibilitychange'. Taken from Fermata.
		if (!keepPlayingWhenHidden) super.onWindowVisibilityChanged(visibility);
		else if (visibility != GONE) super.onWindowVisibilityChanged(VISIBLE);
	}

	/** The stock WebView UA contains "; wv", which makes Google sign-in refuse the WebView. */
	private static String mobileUserAgent(String ua) {
		Matcher m = Pattern.compile(".+ AppleWebKit/(\\S+) .+ Chrome/(\\S+) .+").matcher(ua);
		if (!m.matches()) return ua;
		return "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + ") AppleWebKit/" + m.group(1) +
				" (KHTML, like Gecko) Chrome/" + m.group(2) + " Mobile Safari/" + m.group(1);
	}

	static boolean isAllowedHost(@Nullable String host) {
		if (host == null) return false;
		return host.equals("youtube.com") || host.endsWith(".youtube.com") || host.equals("youtu.be") ||
				host.equals("google.com") || host.endsWith(".google.com"); // sign-in
	}

	private final class Bridge {
		@Keep
		@JavascriptInterface
		public void event(int event, String data) {
			post(() -> {
				try {
					listener.onVideoEvent(event, new JSONObject(data));
				} catch (JSONException ex) {
					Log.w(TAG, "Bad event data: " + data, ex);
				}
			});
		}
	}

	private final class Client extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
			Uri uri = request.getUrl();
			String scheme = uri.getScheme();
			// Stay on YouTube: drop "open in app" intents and links to other sites.
			if (!"https".equals(scheme) && !"http".equals(scheme)) return true;
			if (isAllowedHost(uri.getHost())) return false;
			Log.d(TAG, "Blocked navigation to " + uri);
			return true;
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			view.evaluateJavascript(PLAYER_JS, null);
			CookieManager.getInstance().flush();
		}
	}

	private final class Chrome extends WebChromeClient {
		@Override
		public void onShowCustomView(View view, CustomViewCallback callback) {
			if (customView != null) {
				callback.onCustomViewHidden();
				return;
			}
			customView = view;
			customViewCallback = callback;
			// The WebView stays visible underneath; hiding it can make the page pause.
			fullScreenView.addView(view, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
			fullScreenView.setVisibility(VISIBLE);
		}

		@Override
		public void onHideCustomView() {
			exitFullScreen();
		}

		@Override
		public boolean onConsoleMessage(ConsoleMessage m) {
			Log.d(TAG, "[JS:" + m.lineNumber() + "] " + m.message());
			return true;
		}
	}
}
