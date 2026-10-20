package com.pseudoman21.ytauto.car;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.car.app.CarAppService;
import androidx.car.app.Screen;
import androidx.car.app.Session;
import androidx.car.app.validation.HostValidator;

/**
 * Android Auto entry point through the Car App Library. Newer Android Auto versions hide
 * sideloaded CATEGORY_PROJECTION apps ({@link CarService}), but still list navigation apps, and a
 * navigation app gets a raw surface on the head unit, which {@link SurfaceRenderer} draws the
 * YouTube page into.
 */
public class YtCarAppService extends CarAppService {

	@NonNull
	@Override
	public HostValidator createHostValidator() {
		// Sideload-only app: no allowlist of Android Auto host signatures to check against.
		return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
	}

	@NonNull
	@Override
	public Session onCreateSession() {
		return new Session() {
			@NonNull
			@Override
			public Screen onCreateScreen(@NonNull Intent intent) {
				return new PlayerScreen(getCarContext());
			}
		};
	}
}
