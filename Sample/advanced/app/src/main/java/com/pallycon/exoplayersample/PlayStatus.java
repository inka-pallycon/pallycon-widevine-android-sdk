package com.pallycon.exoplayersample;

import com.google.android.gms.cast.framework.media.RemoteMediaClient;

/**
 * Created by PallyconTeam
 */

//// Chromecast and local (Exo)
public class PlayStatus {
	private	static PlayStatus mObject = new PlayStatus();
	private PlayStatus() {}

	public static final int SCREEN_LOCAL	= 1;
	public static final int SCREEN_CAST		= 2;

	public static final int STATE_IDLE		= 1;
	public static final int STATE_BUFFERING	= 2;
	public static final int STATE_PLAYING	= 3;
	public static final int STATE_PAUSED	= 4;

	public int	mScreen = SCREEN_LOCAL;
	public int	mCurrentState = STATE_IDLE;
	public long	mPosition = 0;

	public static PlayStatus getObject() {
		return mObject;
	}

	public void clear() {
		mCurrentState = STATE_IDLE;
		mPosition = 0;
	}
}
//// CC
