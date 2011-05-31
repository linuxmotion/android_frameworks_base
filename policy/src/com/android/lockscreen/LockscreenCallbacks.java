package com.android.lockscreen;

import com.android.internal.policy.impl.KeyguardScreen;
import com.android.internal.policy.impl.KeyguardUpdateMonitor;

public interface LockscreenCallbacks extends KeyguardScreen, KeyguardUpdateMonitor.InfoCallback,
KeyguardUpdateMonitor.SimStateCallback{

}
