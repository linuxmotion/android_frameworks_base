package com.android.internal.lockscreen;

import com.android.internal.policy.impl.KeyguardScreen;
import com.android.internal.policy.impl.KeyguardUpdateMonitor;

public class LockScreen{

	public interface LockScreenCallbacks extends KeyguardScreen, KeyguardUpdateMonitor.InfoCallback,
	KeyguardUpdateMonitor.SimStateCallback{

	}
}
