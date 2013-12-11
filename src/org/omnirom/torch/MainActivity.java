/*
 *  Copyright (C) 2013 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.omnirom.torch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final String TAG = "TorchActivity";
	
	private TorchWidgetProvider mWidgetProvider;
	private ImageView mButtonOnView;
	private ImageView mButtonSosView;
	private boolean mTorchOn;
	private Context mContext;
	private SharedPreferences mPrefs;
    private boolean BrightMode;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Get preferences
		this.mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		// Fullscreen mode
		if (mPrefs.getBoolean(SettingsActivity.KEY_FULLSCREEN, false)) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			this.getWindow().setFlags(
				WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN
				);
		}
		setContentView(R.layout.mainnew);

		mContext = this.getApplicationContext();
		mButtonOnView = (ImageView) findViewById(R.id.buttoOnImage);
		mButtonOnView.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
                //Animate
                if (mTorchOn){
                    //Turning off
                    AlphaAnimation anim = new AlphaAnimation((float) 1,(float) 0.5);
                    anim.setInterpolator(new LinearInterpolator());
                    anim.setRepeatCount(0);
                    anim.setDuration(400);
                    anim.setFillAfter(true);
                    mButtonOnView.setAnimation(anim);
                    mButtonOnView.startAnimation(anim);
                }
                else
                {
                    //Turning on
                    AlphaAnimation anim = new AlphaAnimation((float) 0.5,(float) 1);
                    anim.setInterpolator(new LinearInterpolator());
                    anim.setRepeatCount(0);
                    anim.setDuration(400);
                    anim.setFillAfter(true);
                    mButtonOnView.setAnimation(anim);
                    mButtonOnView.startAnimation(anim);
                }
				createIntent();
			}
		});
		
		mTorchOn = false;
		mWidgetProvider = TorchWidgetProvider.getInstance();

		updateBigButtonState();
	}

	private void createIntent() {
		Log.d(TAG, mPrefs.getAll().toString());
		Intent intent = new Intent(TorchSwitch.TOGGLE_FLASHLIGHT);
		intent.putExtra("strobe", mPrefs.getBoolean(SettingsActivity.KEY_STROBE, false));
		intent.putExtra("period", mPrefs.getInt(SettingsActivity.KEY_STROBE_FREQ, 5));
		intent.putExtra("bright", mPrefs.getBoolean(SettingsActivity.KEY_BRIGHT, false));
		intent.putExtra("sos", mPrefs.getBoolean(SettingsActivity.KEY_SOS, false));
		mContext.sendBroadcast(intent);
	}

	private void createSosIntent() {
        if (mTorchOn){
            // stop it first
            createIntent();
        }
		Log.d(TAG, mPrefs.getAll().toString());
		Intent intent = new Intent(TorchSwitch.TOGGLE_FLASHLIGHT);
		intent.putExtra("strobe", mPrefs.getBoolean(SettingsActivity.KEY_STROBE, false));
		intent.putExtra("period", mPrefs.getInt(SettingsActivity.KEY_STROBE_FREQ, 5));
		intent.putExtra("bright", mPrefs.getBoolean(SettingsActivity.KEY_BRIGHT, false));
		intent.putExtra("sos", true);
		mContext.sendBroadcast(intent);
	}

	public void onPause() {
		this.updateWidget();
		mContext.unregisterReceiver(mStateReceiver);
		super.onPause();
	}

	public void onDestroy() {
		this.updateWidget();
		super.onDestroy();
	}

	public void onResume() {
		updateBigButtonState();
		this.updateWidget();
		mContext.registerReceiver(mStateReceiver, new IntentFilter(TorchSwitch.TORCH_STATE_CHANGED));
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_sos:
			createSosIntent();
			return true;
		case R.id.action_about:
			this.openAboutDialog();
			return true;
		case R.id.action_settings:
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivityIfNeeded(intent, -1);
            case R.id.action_bright:
                if (mPrefs.getBoolean(SettingsActivity.KEY_BRIGHT, false))
                {
                    //Disabling flash if it's enabled
                    Boolean wasOn = mTorchOn;
                    if (mTorchOn) {
                        createIntent();
                    }
                    mPrefs.edit().putBoolean(SettingsActivity.KEY_BRIGHT, false).commit();
                    Toast.makeText(mContext, getString(R.string.toast_brightmode_disable),
                            Toast.LENGTH_LONG).show();
                    Log.v(TAG, "Bright off");

                    if (wasOn)
                        createIntent(); //Enabling it again
                }
                else {
                    Boolean wasOn = mTorchOn;
                    //Disabling flash if it's enabled
                    if (mTorchOn) {
                        createIntent();
                    }
                    mPrefs.edit().putBoolean(SettingsActivity.KEY_BRIGHT, true).commit();
                    Toast.makeText(mContext, getString(R.string.toast_brightmode_enable),
                            Toast.LENGTH_LONG).show();
                    Log.v(TAG, "Bright on");

                    if (wasOn) //Do not enable it, if it was disabled before
                        createIntent(); //Enabling it again
                }
                return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void openAboutDialog() {
		LayoutInflater li = LayoutInflater.from(this);
		View view = li.inflate(R.layout.aboutview, null);
		new AlertDialog.Builder(MainActivity.this).setTitle(this.getString(R.string.about_title)).setView(view)
		.setNegativeButton(this.getString(R.string.about_close), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		}).show();
	}

	public void updateWidget() {
		this.mWidgetProvider.updateAllStates(mContext);
	}

	private void updateBigButtonState() {
		mButtonOnView.setImageResource(mTorchOn ? R.drawable.button_off : R.drawable.button_on);
	}

	private BroadcastReceiver mStateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(TorchSwitch.TORCH_STATE_CHANGED)) {
				mTorchOn = intent.getIntExtra("state", 0) != 0;
				updateBigButtonState();
			}
		}
	};
}
