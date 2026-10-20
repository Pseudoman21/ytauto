package com.pseudoman21.ytauto.car;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.AppManager;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.pseudoman21.ytauto.About;
import com.pseudoman21.ytauto.Player;
import com.pseudoman21.ytauto.YtWebView;

/**
 * The car screen: a navigation template whose "map" is the YouTube page drawn by
 * {@link SurfaceRenderer}, plus a Back/Home action strip.
 */
final class PlayerScreen extends Screen implements YtWebView.TextInput {
	private final SurfaceRenderer renderer;
	@Nullable
	private SearchScreen search;

	PlayerScreen(CarContext ctx) {
		super(ctx);
		renderer = new SurfaceRenderer(ctx, this);
		ctx.getCarService(AppManager.class).setSurfaceCallback(renderer);
		getLifecycle().addObserver(new DefaultLifecycleObserver() {
			@Override
			public void onDestroy(@NonNull LifecycleOwner owner) {
				renderer.destroy();
			}
		});
	}

	@NonNull
	@Override
	public Template onGetTemplate() {
		ActionStrip actions = new ActionStrip.Builder()
				.addAction(new Action.Builder().setTitle("Back").setOnClickListener(this::back).build())
				.addAction(new Action.Builder().setTitle("Home").setOnClickListener(this::home).build())
				.build();
		// PAN lets the host deliver drag gestures (onScroll) on every head unit.
		ActionStrip map = new ActionStrip.Builder().addAction(Action.PAN).build();
		return new NavigationTemplate.Builder()
				.setActionStrip(actions)
				.setMapActionStrip(map)
				.setPanModeListener(isInPanMode -> {
				})
				.build();
	}

	private void back() {
		About about = renderer.getAbout();
		Player player = renderer.getPlayer();
		if ((about != null) && about.onBack()) return;
		if (player != null) player.onBack();
	}

	private void home() {
		Player player = renderer.getPlayer();
		if (player != null) player.home();
	}

	@Override
	public boolean isInputActive() {
		return search != null;
	}

	@Override
	public void startInput(String text, YtWebView target) {
		search = new SearchScreen(getCarContext(), target, text);
		getScreenManager().pushForResult(search, r -> search = null);
	}

	@Override
	public void stopInput() {
		if (search != null) search.finish();
	}
}
