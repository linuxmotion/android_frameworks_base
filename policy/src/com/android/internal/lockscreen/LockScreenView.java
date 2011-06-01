package com.android.internal.lockscreen;

import java.io.File;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.SystemProperties;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.internal.policy.impl.KeyguardScreen;
import com.android.internal.policy.impl.KeyguardScreenCallback;
import com.android.internal.policy.impl.KeyguardUpdateMonitor;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCard.State;
import com.android.internal.widget.LockPatternUtils;



import com.android.internal.R;


/**
 * 
 * A basic unimplemented view that has access to the keyguard.
 * The developer needs to extend this view.
 * 
 * @author John Weyrauch
 *
 */

public class LockScreenView extends View implements KeyguardScreen, KeyguardUpdateMonitor.InfoCallback,
KeyguardUpdateMonitor.SimStateCallback{

	
	

    private static final String ENABLE_MENU_KEY_FILE = "/data/local/enable_menu_key";
    private static final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
	
	   private LockPatternUtils mLockPatternUtils;
	   private KeyguardUpdateMonitor mUpdateMonitor;
	   private KeyguardScreenCallback mCallback;
	   
	   
	   private Context mContext;
	   

	    private TextView mEmergencyCallText;
	    private Button mEmergencyCallButton;
	
	
	
	public LockScreenView(Context context, Configuration configuration, LockPatternUtils lockPatternUtils,
            KeyguardUpdateMonitor updateMonitor,
            KeyguardScreenCallback callback){
		
		super(context); // Used because the view is not inflated here
		
		mContext = context;
		
		
		final LayoutInflater inflater = LayoutInflater.from(context);
	   
		
		 resolvePackageForView();
		/* We need to inflate the view from here
		* This in turn wil be passed to the keyguard which will then display it.
		* This is actually set before the screen turns off.
		* This means that a user shoudl not set it when the screen turns off
		* Since this is an view that can block the user from 
		* being able to unlock the screen we must first test the view
		* 
		* Use Intnet.ACTION_APP_LOCKSCREEN_TEST to pass along the view into the 
		* test application
		* 
		*/
		
		 testPackageForLock(); // Only needs to occur when a new package is used, even the system package, in case of modification
		
		/*
		 * If the view is able to unlock the screen through the
		 * testing facilities than the user can then set this app 
		 * as the default lockscreen.
		 * 
		 * 
		 * Use Intent.ACTION_APP_LOCKSCREEN_SET to set the new view
		 * as the lockscreen layout in the KeyGuardView.
		 * 
		 * 
		 * 
		 * 
		 */
		
		 setPackageForLock();
		
		
	}
	
	
	/**
     * The status of this lock screen.
     */
    enum Status {
        /**
         * Normal case (sim card present, it's not locked)
         */
        Normal(true),

        /**
         * The sim card is 'network locked'.
         */
        NetworkLocked(true),

        /**
         * The sim card is missing.
         */
        SimMissing(false),

        /**
         * The sim card is missing, and this is the device isn't provisioned, so we don't let
         * them get past the screen.
         */
        SimMissingLocked(false),

        /**
         * The sim card is PUK locked, meaning they've entered the wrong sim unlock code too many
         * times.
         */
        SimPukLocked(false),

        /**
         * The sim card is locked.
         */
        SimLocked(true);

        private final boolean mShowStatusLines;

        Status(boolean mShowStatusLines) {
            this.mShowStatusLines = mShowStatusLines;
        }

        /**
         * @return Whether the status lines (battery level and / or next alarm) are shown while
         *         in this state.  Mostly dictated by whether this is room for them.
         */
        public boolean showStatusLines() {
            return mShowStatusLines;
        }
    }
    
    
    /**
     * Determine the current status of the lock screen given the sim state and other stuff.
     */
    private Status getCurrentStatus(IccCard.State simState) {
        boolean missingAndNotProvisioned = (!mUpdateMonitor.isDeviceProvisioned()
                && simState == IccCard.State.ABSENT);
        if (missingAndNotProvisioned) {
            return Status.SimMissingLocked;
        }

        switch (simState) {
            case ABSENT:
                return Status.SimMissing;
            case NETWORK_LOCKED:
                return Status.SimMissingLocked;
            case NOT_READY:
                return Status.SimMissing;
            case PIN_REQUIRED:
                return Status.SimLocked;
            case PUK_REQUIRED:
                return Status.SimPukLocked;
            case READY:
                return Status.Normal;
            case UNKNOWN:
                return Status.SimMissing;
        }
        return Status.SimMissing;
    }
	
    static CharSequence getCarrierString(CharSequence telephonyPlmn, CharSequence telephonySpn) {
        if (telephonyPlmn != null && telephonySpn == null) {
            return telephonyPlmn;
        } else if (telephonyPlmn != null && telephonySpn != null) {
            return telephonyPlmn + "|" + telephonySpn;
        } else if (telephonyPlmn == null && telephonySpn != null) {
            return telephonySpn;
        } else {
            return "";
        }
    }
    
    
    /**
     * In general, we enable unlocking the insecure key guard with the menu key. However, there are
     * some cases where we wish to disable it, notably when the menu button placement or technology
     * is prone to false positives.
     *
     * @return true if the menu key should be enabled
     */
    private boolean shouldEnableMenuKey() {
        final Resources res = mContext.getResources();
        final boolean configDisabled = res.getBoolean(R.bool.config_disableMenuKeyInLockScreen);
        final boolean isMonkey = SystemProperties.getBoolean("ro.monkey", false);
        final boolean fileOverride = (new File(ENABLE_MENU_KEY_FILE)).exists();
        return !configDisabled || isMonkey || fileOverride;
    }
	
	  public void pokeWakelock() {
          mCallback.pokeWakelock();
      }

      public void pokeWakelock(int millis) {
    	  mCallback.pokeWakelock(millis);
      }

      public void keyguardDone(boolean authenticated) {
          mCallback.keyguardDone(authenticated);
      }
	
      
	
	public interface LockScreenCallbacks {

	}
	
	public boolean setPackageForLock(){
		
		
		
	}
	
	
	public boolean testPackageForLock(){
		
		
		
	}
	
	public void  resolvePackageForView(){
		
		
		
	}
	
	public View returnViewForCreation(){
		
		
		return this;
		
	}


	public void cleanUp() {
		// TODO Auto-generated method stub
		
	}


	public boolean needsInput() {
		// TODO Auto-generated method stub
		return false;
	}


	public void onPause() {
		// TODO Auto-generated method stub
		
	}


	public void onResume() {
		// TODO Auto-generated method stub
		
	}


	public void onMusicChanged() {
		// TODO Auto-generated method stub
		
	}


	public void onPhoneStateChanged(String newState) {
		// TODO Auto-generated method stub
		
	}


	public void onRefreshBatteryInfo(boolean showBatteryInfo,
			boolean pluggedIn, int batteryLevel) {
		// TODO Auto-generated method stub
		
	}


	public void onRefreshCarrierInfo(CharSequence plmn, CharSequence spn) {
		// TODO Auto-generated method stub
		
	}


	public void onRingerModeChanged(int state) {
		// TODO Auto-generated method stub
		
	}


	public void onTimeChanged() {
		// TODO Auto-generated method stub
		
	}


	public void onSimStateChanged(State simState) {
		// TODO Auto-generated method stub
		
	}
	
}
