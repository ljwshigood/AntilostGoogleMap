package com.publicnumber.satellite.activity;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.publicnumber.satellite.R;
import com.publicnumber.satellite.application.AppContext;
import com.publicnumber.satellite.bean.SoundInfo;
import com.publicnumber.satellite.bean.alarmInfo;
import com.publicnumber.satellite.db.DatabaseManager;
import com.publicnumber.satellite.util.PlayMedia;
import com.publicnumber.satellite.util.ToastCustom;

public class SoundActivity extends BaseActivity implements OnClickListener {

	private LinearLayout mLLRing;

	private Context mContext;

	private ImageView mIvBack;

	private DatabaseManager mDatabaseManager;

	private SoundInfo mSoundInfo;

	String[] mRingString = new String[10];

	int[] mRingResId = new int[10];

	private String mAddress;

	private SeekBar mSbVolume;

	private CheckBox mCbShock;

	private SeekBar mSbDuration;

	private AudioManager mAudioManager ;  
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device_sound);
		mAddress = AppContext.mDeviceAddress ;
		mContext = SoundActivity.this;
		mDatabaseManager = DatabaseManager.getInstance(mContext);
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);  
		setTitle(mContext.getString(R.string.sound));
	}
	

	private View mView;

	private TextView mTvTitleInfo;
	
	private void setTitle(String info) {
		mView = (View) findViewById(R.id.include_head);
		mTvTitleInfo = (TextView) mView.findViewById(R.id.tv_title_info);
		mTvTitleInfo.setText(info);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		initData();
		initView();
	}

	private void initData() {
		ArrayList<SoundInfo> list = mDatabaseManager.selectSoundInfo(AppContext.mDeviceAddress);
		if (list.size() > 0) {
			mSoundInfo = mDatabaseManager.selectSoundInfo(AppContext.mDeviceAddress).get(0);
		}
	}

	private TextView mTvRing;
	
	private void initView() {
		mTvRing = (TextView)findViewById(R.id.tv_ring);
		mLLRing = (LinearLayout) findViewById(R.id.ll_ring);
		mLLRing.setOnClickListener(this);
		mIvBack = (ImageView) findViewById(R.id.iv_back);
		mIvBack.setOnClickListener(this);

		mSbVolume = (SeekBar) findViewById(R.id.sb_volume_set);
		mCbShock = (CheckBox) findViewById(R.id.cb_shock);
		mSbDuration = (SeekBar) findViewById(R.id.sb_duration_time);
		mSbDuration.setProgress((int) mSoundInfo.getDurationTime());
		mCbShock.setChecked(mSoundInfo.isShock());
		mSbVolume.setMax(mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
		mSbVolume.setProgress(mSoundInfo.getRingVolume());
		mCbShock.setOnClickListener(this);
		mSbVolume.setOnSeekBarChangeListener(sbVolumeListener);
		mSbDuration.setOnSeekBarChangeListener(sbDurationListener);
		
		for(int i = 0 ;i < AppContext.mAlarmList.size();i++){
			alarmInfo info = AppContext.mAlarmList.get(i);
			if(mSoundInfo.getRingId() == info.getRes()){
				mTvRing.setText(info.getName());
			}
				
		}
	}

	OnSeekBarChangeListener sbDurationListener = new OnSeekBarChangeListener() // 调音监听器
	{
		public void onProgressChanged(SeekBar arg0, int progress,
				boolean fromUser) {
			mSoundInfo.setDurationTime(progress);
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {

		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			String argString = getResources().getString(R.string.sound_duration_time);    
			String string = String.format(argString,seekBar.getProgress());    
			ToastCustom.toastCustom(SoundActivity.this, mContext, string, 1);
			mDatabaseManager.updateSoundInfo(mAddress, mSoundInfo);
		}
	};
	OnSeekBarChangeListener sbVolumeListener = new OnSeekBarChangeListener() // 调音监听器
	{
		public void onProgressChanged(SeekBar arg0, int progress,
				boolean fromUser) {
			mSoundInfo.setRingVolume(progress);
			mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0); 
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			PlayMedia.getInstance(mContext).playMusicMedia(mSoundInfo.getRingId(),seekBar.getProgress());
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			String argString = getResources().getString(R.string.sound_valume);    
			String string = String.format(argString,seekBar.getProgress());    
			ToastCustom.toastCustom(SoundActivity.this, mContext, string, 1);
			mDatabaseManager.updateSoundInfo(mAddress, mSoundInfo);
		}
	};

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.ll_ring:
			Intent intent = new Intent(mContext, AlarmRingActvivity.class);
			startActivity(intent);
			break;
		case R.id.iv_back:
			finish();
			break;
		case R.id.cb_shock:
			mSoundInfo.setShock(mCbShock.isChecked());
			
			mDatabaseManager.updateSoundInfo(mAddress, mSoundInfo);
			break;
		default:
			break;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		PlayMedia.getInstance(mContext).release();
	}
}
