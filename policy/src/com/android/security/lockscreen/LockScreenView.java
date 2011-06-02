package com.android.security.lockscreen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.policy.impl.KeyguardScreen;
import com.android.internal.policy.impl.KeyguardScreenCallback;
import com.android.internal.policy.impl.KeyguardUpdateMonitor;
import com.android.internal.policy.impl.LockPatternKeyguardView;
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

public class LockScreenView extends LinearLayout implements KeyguardScreen, KeyguardUpdateMonitor.InfoCallback,
KeyguardUpdateMonitor.SimStateCallback{

	
    private static final boolean DBG = true;
    private static final String TAG = "LockScreen";
    private static final String ENABLE_MENU_KEY_FILE = "/data/local/enable_menu_key";
    private static final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
	
	private LockPatternUtils mLockPatternUtils;
	private KeyguardUpdateMonitor mUpdateMonitor;
	private KeyguardScreenCallback mCallback;	   
	

	private Context mContext;
	   
	private TextView mEmergencyCallText;
	private Button mEmergencyCallButton;
	    
	private View mUnlocker;
	
	
	private Status mStatus = Status.Normal;
	
	
	  // current configuration state of keyboard and display
    private int mKeyboardHidden;
    private int mCreationOrientation;
    
    // last known plugged in state
    private boolean mPluggedIn = false;

    // last known battery level
    private int mBatteryLevel = 100;
    

    private boolean mEnableMenuKeyInLockScreen;
	private AudioManager mAudioManager;
	private boolean mSilentMode;
	private boolean mIsMusicActive;
	private boolean mShowingBatteryInfo;
	
	private Button mUnlockButton;
	
	public TextView mCarrier;
    //public TextView mDate;
    //public TextView mTime;
    //public TextView mAmPm;
    //public TextView mStatus1;
    //public TextView mStatus2;
    
    private boolean mIsNewPackageViable = false;

    private boolean mUserIsSettingNewPackage = false;
	private Uri mPackageURI;
	
	
	private final int PACKAGE_OK = 0;
	private final int PACKAGE_CANCELLED = 1;
	private final int SET_PACKAGE = 10;
	private final int DO_NOT_SET_PACKAGE = 10;
	
	public LockScreenView(Context context, Configuration configuration, LockPatternUtils lockPatternUtils,
            KeyguardUpdateMonitor updateMonitor,
            KeyguardScreenCallback callback){
		
		super(context);
		if (DBG) Log.d(TAG,"Creating the lockscreen");
		
		mContext = context;
		
		  mLockPatternUtils = lockPatternUtils;
	        mUpdateMonitor = updateMonitor;
	        mCallback = callback;

	        mEnableMenuKeyInLockScreen = shouldEnableMenuKey();

	        mCreationOrientation = configuration.orientation;

	        mKeyboardHidden = configuration.hardKeyboardHidden;

	        if (LockPatternKeyguardView.isInDebugMode()) {
	            Log.v(TAG, "***** CREATING LOCK SCREEN", new RuntimeException());
	            Log.v(TAG, "Cur orient=" + mCreationOrientation
	                    + " res orient=" + context.getResources().getConfiguration().orientation);
	        }
		
		
	        

	        setFocusable(true);
	        setFocusableInTouchMode(true);
	        setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
	        

	        mUpdateMonitor.registerInfoCallback(this);
	        mUpdateMonitor.registerSimStateCallback(this);
		

        mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        mSilentMode = isSilentMode();
		
        
        // Inflates the view that the developers does not need access to
        // THis is the emergency call stuff
        if (DBG) Log.d(TAG,"Beginning to inflate the view");
        
		final LayoutInflater inflater = LayoutInflater.from(context);
		View v;
	   
		if (mCreationOrientation != Configuration.ORIENTATION_LANDSCAPE) {
           v = inflater.inflate(R.layout.keyguard_screen_app_unlock_port, this, true);
        } else {
           v = inflater.inflate(R.layout.keyguard_screen_tab_unlock_land, this, true);
        }
		
		// Now that the view is inflated from the xml
		// all of the view/buttons need to be initialized
		setUpOverlayView();
		
		
		
		//*************************************************************//
		
		
	
		
		
		mPackageURI =  resolvePackageForView();
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
		
		 this.mIsNewPackageViable = testPackageForLock(mPackageURI); // Only needs to occur when a new package is used, even the system package, in case of modification
		
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
		
		 if(this.mIsNewPackageViable && mUserIsSettingNewPackage )
			 setPackageForLock(mPackageURI);
		 
		 
		 /*
		  * 
		  * Add the view to the layout that has been confirmed
		  * 
		  */
		 
		 Button b = new Button(context);
		 b.setText("Unlock");
		 b.setClickable(true);
		 b.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				// TODO Auto-generated method stub
				mCallback.goToUnlockScreen();
				
			   }
			});
		 b.setVisibility(View.VISIBLE);
		 b.setTextSize(25);
		 b.setTextColor(Color.BLACK);
		 b.setLayoutParams(new LayoutParams(
				 ViewGroup.LayoutParams.WRAP_CONTENT,
				 ViewGroup.LayoutParams.WRAP_CONTENT));
		 
		 LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
				 ViewGroup.LayoutParams.WRAP_CONTENT);
		 b.setGravity(Gravity.CENTER_HORIZONTAL);
		 b.setGravity(Gravity.TOP);
		 
		
		 
		this.addView(b);
		 
		
		
	}
	 private void setUpOverlayView() {
		 if (DBG) Log.d(TAG,"Setting up the overlayview");
		 
		
			mUnlockButton = (Button) this.findViewById(R.id.UnlockButton);
			mUnlockButton.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					// TODO Auto-generated method stub
					mCallback.goToUnlockScreen();
					
				   }
				});
		 
			
			 mEmergencyCallText = (TextView) findViewById(R.id.emergencyCallText);
	        mEmergencyCallButton = (Button) findViewById(R.id.emergencyCallButton);
	        mEmergencyCallButton.setText(R.string.lockscreen_emergency_call);

	        mLockPatternUtils.updateEmergencyCallButtonState(mEmergencyCallButton);
	        
	        mEmergencyCallButton.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View v) {
	                mCallback.takeEmergencyCallAction();
	            }
	        });
			
	        
	        mCarrier = (TextView) findViewById(R.id.carrier);
	        // Required for Marquee to work
	        mCarrier.setSelected(true);
	        mCarrier.setTextColor(0xffffffff);

	    /*
	        mScreenLocked = (TextView) findViewById(R.id.screenLocked);
	        
	        mTime = (TextView) findViewById(R.id.timeDisplay);
	        mAmPm = (TextView) findViewById(R.id.am_pm);
	        mDate = (TextView) findViewById(R.id.date);
	        mStatus1 = (TextView) findViewById(R.id.status1);
	        mStatus2 = (TextView) findViewById(R.id.status2);
		    */
		 
	        
	        mUnlocker = new View(mContext);
	        
		
	}
	protected void onActivityResult(int requestCode, int resultCode,
             Intent data) {
         if (resultCode == this.PACKAGE_OK) {
        	 mIsNewPackageViable = true;
         }
         if (resultCode == this.PACKAGE_CANCELLED) {
        	 mIsNewPackageViable = false;
         }
         
         
         
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
	
    public static CharSequence getCarrierString(CharSequence telephonyPlmn, CharSequence telephonySpn) {
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
    
    public void toggleSilentMode() {
        // tri state silent<->vibrate<->ring if silent mode is enabled, otherwise toggle silent mode
        final boolean mVolumeControlSilent = Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.VOLUME_CONTROL_SILENT, 0) != 0;
        mSilentMode = mVolumeControlSilent
            ? ((mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) || !mSilentMode)
            : !mSilentMode;
        if (mSilentMode) {
            final boolean vibe = mVolumeControlSilent
            ? (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_VIBRATE)
            : (Settings.System.getInt(
                getContext().getContentResolver(),
                Settings.System.VIBRATE_IN_SILENT, 1) == 1);

            mAudioManager.setRingerMode(vibe
                ? AudioManager.RINGER_MODE_VIBRATE
                : AudioManager.RINGER_MODE_SILENT);
        } else {
            mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        }
}
    

 // shameless kang of music widgets
     public static Uri getArtworkUri(Context context, long song_id, long album_id) {

         if (album_id < 0) {
             // This is something that is not in the database, so get the album art directly
             // from the file.
             if (song_id >= 0) {
                 return getArtworkUriFromFile(context, song_id, -1);
             }
             return null;
         }

         ContentResolver res = context.getContentResolver();
         Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);
         if (uri != null) {
             InputStream in = null;
             try {
                 in = res.openInputStream(uri);
                 return uri;
             } catch (FileNotFoundException ex) {
                 // The album art thumbnail does not actually exist. Maybe the user deleted it, or
                 // maybe it never existed to begin with.
                 return getArtworkUriFromFile(context, song_id, album_id);
             } finally {
                 try {
                     if (in != null) {
                         in.close();
                     }
                 } catch (IOException ex) {
                 }
             }
         }
         return null;
     }

     private static Uri getArtworkUriFromFile(Context context, long songid, long albumid) {

         if (albumid < 0 && songid < 0) {
             return null;
         }

         try {
             if (albumid < 0) {
                 Uri uri = Uri.parse("content://media/external/audio/media/" + songid + "/albumart");
                 ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
                 if (pfd != null) {
                     return uri;
                 }
             } else {
                 Uri uri = ContentUris.withAppendedId(sArtworkUri, albumid);
                 ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
                 if (pfd != null) {
                     return uri;
                 }
             }
         } catch (FileNotFoundException ex) {
             //
         }
         return null;
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
      
      
      private boolean isSilentMode() {
          return mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL;
      }
	
      
	
	public interface LockScreenCallbacks {

	}
	
	public boolean setPackageForLock(Uri mPackageURI){
		
		
		return true;
	}
	
	
	public boolean testPackageForLock(Uri mPackageURI){
		if (DBG) Log.d(TAG,"Testing the package for lockscreen resolution");
		

		return true;
		
	}
	
	public Uri  resolvePackageForView(){
		if (DBG) Log.d(TAG,"Resolving URI for view parsing");
		
		
		Intent packageTester = new Intent(Intent.ACTION_APP_LOCKSCREEN_TEST);
		packageTester.putExtra("Package", mPackageURI);
		
		
		final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		final List pkgAppsList = mContext.getPackageManager().queryIntentActivities( mainIntent, 0);
		
		return Uri.EMPTY;
		
	}
	
	public View returnViewForCreation(){
		
		
		return this;
		
	}

	

    /** {@inheritDoc} */
    public boolean needsInput() {
        return false;
    }

    /** {@inheritDoc} */
    public void onPause() {

    }

    /** {@inheritDoc} */
    public void onResume() {
    	if (DBG) Log.d(TAG,"Resuming the lockscreen");
        resetStatusInfo(mUpdateMonitor);
        mLockPatternUtils.updateEmergencyCallButtonState(mEmergencyCallButton);
    }

    private void resetStatusInfo(KeyguardUpdateMonitor updateMonitor) {
    	if (DBG) Log.d(TAG,"Reseting the status information");
    	
        mShowingBatteryInfo = updateMonitor.shouldShowBatteryInfo();
        mPluggedIn = updateMonitor.isDevicePluggedIn();
        mBatteryLevel = updateMonitor.getBatteryLevel();
        mIsMusicActive = mAudioManager.isMusicActive();

        mStatus = getCurrentStatus(updateMonitor.getSimState());
        updateLayout(mStatus);

       // refreshBatteryStringAndIcon();
        //refreshAlarmDisplay();
        //refreshMusicStatus();
        //refreshPlayingTitle();

       // mDateFormatString = getContext().getString(R.string.full_wday_month_day_no_year);
       // refreshTimeAndDateDisplay();
       // updateStatusLines();
    }
    

    /**
     * Update the layout to match the current status.
     */
    private void updateLayout(Status status) {
        // The emergency call button no longer appears on this screen.
        if (DBG) Log.d(TAG, "updateLayout: status=" + status);


        mEmergencyCallButton.setVisibility(View.GONE); // in almost all cases

        switch (status) {
            case Normal:
                // text
                mCarrier.setText(
                        getCarrierString(
                                mUpdateMonitor.getTelephonyPlmn(),
                                mUpdateMonitor.getTelephonySpn()));

                // Empty now, but used for sliding tab feedback
                mUnlocker.setVisibility(View.VISIBLE);
                // layout
                //mScreenLocked.setVisibility(View.VISIBLE);
                mEmergencyCallText.setVisibility(View.GONE);
                break;
            case NetworkLocked:
                // The carrier string shows both sim card status (i.e. No Sim Card) and
                // carrier's name and/or "Emergency Calls Only" status
                mCarrier.setText(
                        getCarrierString(
                                mUpdateMonitor.getTelephonyPlmn(),
                                getContext().getText(R.string.lockscreen_network_locked_message)));
                //mScreenLocked.setText(R.string.lockscreen_instructions_when_pattern_disabled);
                

                mUnlocker.setVisibility(View.VISIBLE);
                // layout
                //mScreenLocked.setVisibility(View.VISIBLE);
                
                mEmergencyCallText.setVisibility(View.GONE);
                break;
            case SimMissing:
                // text
                mCarrier.setText(R.string.lockscreen_missing_sim_message_short);
                //mScreenLocked.setText(R.string.lockscreen_missing_sim_instructions);

                // layout
                //mScreenLocked.setVisibility(View.VISIBLE);

                mUnlocker.setVisibility(View.VISIBLE);
                mEmergencyCallText.setVisibility(View.VISIBLE);
                // do not need to show the e-call button; user may unlock
                break;
            case SimMissingLocked:
                // text
                mCarrier.setText(
                        getCarrierString(
                                mUpdateMonitor.getTelephonyPlmn(),
                                getContext().getText(R.string.lockscreen_missing_sim_message_short)));
                //mScreenLocked.setText(R.string.lockscreen_missing_sim_instructions);

                // layout
                //mScreenLocked.setVisibility(View.VISIBLE);
                

                mUnlocker.setVisibility(View.INVISIBLE);
                
                mEmergencyCallText.setVisibility(View.VISIBLE);
                mEmergencyCallButton.setVisibility(View.VISIBLE);
                break;
            case SimLocked:
                // text
                mCarrier.setText(
                        getCarrierString(
                                mUpdateMonitor.getTelephonyPlmn(),
                                getContext().getText(R.string.lockscreen_sim_locked_message)));

                // layout
                //mScreenLocked.setVisibility(View.INVISIBLE);

                mUnlocker.setVisibility(View.VISIBLE);
                mEmergencyCallText.setVisibility(View.GONE);
                break;
            case SimPukLocked:
                // text
                mCarrier.setText(
                        getCarrierString(
                                mUpdateMonitor.getTelephonyPlmn(),
                                getContext().getText(R.string.lockscreen_sim_puk_locked_message)));
                //mScreenLocked.setText(R.string.lockscreen_sim_puk_locked_instructions);

                // layout
                //mScreenLocked.setVisibility(View.VISIBLE);
                
                mEmergencyCallText.setVisibility(View.VISIBLE);

                mUnlocker.setVisibility(View.GONE); // Cannot unlock
                
                mEmergencyCallButton.setVisibility(View.VISIBLE);
                break;
        }
        
    }
    
    /** {@inheritDoc} */
    public void cleanUp() {
        mUpdateMonitor.removeCallback(this); // this must be first
        mLockPatternUtils = null;
        mUpdateMonitor = null;
        mCallback = null;
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
