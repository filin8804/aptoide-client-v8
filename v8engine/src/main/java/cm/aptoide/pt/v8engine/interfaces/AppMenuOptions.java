/*
 * Copyright (c) 2016.
 * Modified by SithEngineer on 19/07/2016.
 */

package cm.aptoide.pt.v8engine.interfaces;

import android.support.annotation.Nullable;

import rx.functions.Action0;

/**
 * Created by sithengineer on 19/07/16.
 */
public interface AppMenuOptions {

	/**
	 * Shows or hides the uninstall app option in menu according to parameter onClickListener.
	 *
	 * @param unInstallAction if set to null, uninstall option is hidden. Visible otherwise.
	 */
	void setUnInstallMenuOptionVisible(@Nullable Action0 unInstallAction);
}
