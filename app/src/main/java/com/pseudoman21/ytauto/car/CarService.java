package com.pseudoman21.ytauto.car;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarActivityService;

/**
 * Bound by Android Auto (via the CATEGORY_PROJECTION intent filter) to show {@link CarMainActivity}
 * on the head unit.
 */
public class CarService extends CarActivityService {

	@Override
	public Class<? extends CarActivity> getCarActivity() {
		return CarMainActivity.class;
	}
}
