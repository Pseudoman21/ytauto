package com.pseudoman21.ytauto;

import android.view.View;

/**
 * The About page: an overlay opened by tapping the watermark. Shared by the phone and the car,
 * since a CarActivity can't start a regular Activity on the head unit.
 */
public final class About {
	private final View page;

	/** @param root a view inflated from {@code R.layout.player} */
	public About(View root) {
		page = root.findViewById(R.id.about);
		root.findViewById(R.id.watermark).setOnClickListener(v -> page.setVisibility(View.VISIBLE));
		page.findViewById(R.id.about_close).setOnClickListener(v -> onBack());
		// Opens the channel in the app's own YouTube page, so it works on the head unit too.
		YtWebView web = root.findViewById(R.id.web);
		page.findViewById(R.id.about_channel).setOnClickListener(v -> {
			onBack();
			web.loadUrl(v.getContext().getString(R.string.channel_url));
		});
	}

	/** Closes the page if it's open. Returns false when it was already closed. */
	public boolean onBack() {
		if (page.getVisibility() != View.VISIBLE) return false;
		page.setVisibility(View.GONE);
		return true;
	}
}
