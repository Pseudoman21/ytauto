package com.pseudoman21.ytauto.car;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.SearchTemplate;
import androidx.car.app.model.Template;

import com.pseudoman21.ytauto.YtWebView;

/**
 * The car keyboard, shown when a text field on the page gets focus. Every change is mirrored into
 * the page; submitting runs the search and returns to the page.
 */
final class SearchScreen extends Screen {
	private final YtWebView target;
	private final String initialText;

	SearchScreen(CarContext ctx, YtWebView target, String initialText) {
		super(ctx);
		this.target = target;
		this.initialText = initialText;
	}

	@NonNull
	@Override
	public Template onGetTemplate() {
		return new SearchTemplate.Builder(new SearchTemplate.SearchCallback() {
			@Override
			public void onSearchTextChanged(@NonNull String text) {
				target.setInputText(text);
			}

			@Override
			public void onSearchSubmitted(@NonNull String text) {
				target.setInputText(text);
				target.submitInput();
				finish();
			}
		})
				.setInitialSearchText(initialText)
				.setSearchHint("Search YouTube")
				.setHeaderAction(Action.BACK)
				.setShowKeyboardByDefault(true)
				.build();
	}
}
