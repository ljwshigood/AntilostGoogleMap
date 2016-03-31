package com.publicnumber.satellite.activity;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.publicnumber.satellite.R;
import com.publicnumber.satellite.application.AppContext;
import com.publicnumber.satellite.service.BluetoothLeService;
import com.publicnumber.satellite.util.RecordManager;

public class RecordActivity extends Activity implements OnClickListener {

	private Context mContext;

	private ImageView mIvRecordMenu;

	private ImageView mIvRecord;

	private TextView mTvTime;

	private RecordManager mRecordManger;

	private boolean isFirstRecord;

	private ImageView mRvRecord;
	
	private ImageView mIvBack ;

	private Handler mHandler = new Handler() {

		public void handleMessage(android.os.Message msg) {
			String strMinute = String.valueOf(minute);
			String strSecond = String.valueOf(second);
			if (minute < 10) {
				strMinute = "0" + strMinute;
			}
			if (second < 10) {
				strSecond = "0" + strSecond;
			}
			mTvTime.setText(strMinute + ":" + strSecond);
		};
	};
	
	private int mFlag = 0 ;
	
	private void getIntentData(){
		Intent intent = getIntent();
		mFlag = intent.getIntExtra("flag", 0);	
	}
	
	private PowerManager pm;

	private PowerManager.WakeLock wakeLock;
	
	public Handler handler = new Handler(){
		
		public void handleMessage(android.os.Message msg) {
    		mMediaStatusCode = mRecordManger.startRecord();
    		mIvRecord.setBackgroundResource(R.drawable.ic_record_pause);
			mIvRecordMenu.setBackgroundResource(R.drawable.ic_record_save);
			isFirstRecord = false;
			startTimeRecord();
		};
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		AppContext.isAlarm = false;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_record);
		
		getIntentData();
		mContext = RecordActivity.this;
		mRecordManger = RecordManager.getInstance(mContext);
		initView();
		makeGattUpdateIntentFilter();
		vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		
		if(mFlag == 1){
			vibrator.vibrate(200);
	    	if(mMediaStatusCode == -1){
				new Thread(){
					
					@Override
					public void run() {
						handler.sendEmptyMessage(0);
					};
					
				}.start();
				
	    	}else{
	    		saveRecord();
	    		mMediaStatusCode = -1 ;
	    		isSave = true ;
	    	}
		}
		
	}
	
	private Vibrator vibrator;

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		unregisterReceiver(mGattUpdateReceiver);
		
		if(vibrator != null){
    		vibrator.cancel();
    	}
		
		if(wakeLock != null){
			wakeLock.release();  
		}
		AppContext.isAlarm = true ;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		isFirstRecord = true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		saveRecord();
	}

	private ImageView mIvPause ;
	
	private void initView() {
		mIvPause = (ImageView)findViewById(R.id.iv_pause);
		mIvBack  =  (ImageView)findViewById(R.id.iv_back);
		mRvRecord = (ImageView) findViewById(R.id.rv_record);
		mIvRecordMenu = (ImageView) findViewById(R.id.iv_record_menu);
		mIvRecordMenu.setOnClickListener(this);
		mTvTime = (TextView) findViewById(R.id.tv_record_time);
		mIvRecord = (ImageView) findViewById(R.id.cb_record);
		mIvRecord.setOnClickListener(this);
		mIvBack.setOnClickListener(this);
		mIvPause.setOnClickListener(this);
	}

	private boolean isSave = false ;
	
	private int mMediaStatusCode = -1 ;

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.iv_record_menu:
			if(mMediaStatusCode == -1){
				Intent intent = new Intent(mContext, RecordMenuActivity.class);
				startActivity(intent);
				return ;
			}else if(mMediaStatusCode != 2 && !isSave){
				saveRecord();
				mMediaStatusCode = -1 ;
				isSave = true ;
				Log.e("liujw","#################saveRecord");
				return ;
			}else if(isSave){
				Intent intent = new Intent(mContext, RecordMenuActivity.class);
				startActivity(intent);
				Log.e("liujw","#################RecordMenuActivity");
				return ;
			}
			
			/*if (!isSave) {
				saveRecord();
			} else {
				Intent intent = new Intent(mContext, RecordMenuActivity.class);
				startActivity(intent);
			}*/
			break;
		case R.id.cb_record:
			if(mMediaStatusCode == -1){
				mIvRecord.setBackgroundResource(R.drawable.ic_record_pause);
				mMediaStatusCode = mRecordManger.startRecord();
				mIvRecordMenu.setBackgroundResource(R.drawable.ic_record_save);
				isFirstRecord = false;
				startTimeRecord();
			}else if(mMediaStatusCode == 0){
				mMediaStatusCode = mRecordManger.pauseRecord();
				if (mMediaStatusCode == 1) { // true 暂停录音
					mIvRecord.setBackgroundResource(R.drawable.ic_record);
					timer.cancel();
				} else { // false 开始恢复录音
					mIvRecordMenu.setBackgroundResource(R.drawable.ic_record_save);
					mIvRecord.setBackgroundResource(R.drawable.ic_record_pause);
					startTimeRecord();
				}
				
			}else if(mMediaStatusCode == 1){
				
				mMediaStatusCode = mRecordManger.pauseRecord();
				if (mMediaStatusCode == 1) { // true 暂停录音
					mIvRecord.setBackgroundResource(R.drawable.ic_record);
					timer.cancel();
				} else { // false 开始恢复录音
					mIvRecordMenu.setBackgroundResource(R.drawable.ic_record_save);
					mIvRecord.setBackgroundResource(R.drawable.ic_record_pause);
					startTimeRecord();
				}
			}
			//handlerRecord();
			break ;
		case R.id.iv_back:
			finish();
			break ;
		default:
			break;
		}
	}

	private int saveRecord() {
		int ret = 0 ;
		ret = mRecordManger.saveRecord();
		mIvRecordMenu.setBackgroundResource(R.drawable.ic_record_menu_nomal);
		mIvRecord.setBackgroundResource(R.drawable.ic_record);
		isSave = true;
		if (timer != null) {
			timer.cancel();
		}
		minute = 0;
		second = 0;
		//mTvTime.setText("00:00");
		return ret ;
	}

	/*private void handlerRecord() {
		if (isFirstRecord) { // 第一次录音
			mIvRecord.setBackgroundResource(R.drawable.ic_record_pause);
			mRecordManger.startRecord();
			mIvRecordMenu.setBackgroundResource(R.drawable.ic_record_save);
			isFirstRecord = false;
			startTimeRecord();
			
			Log.e("liujw","#####################isFirstRecord") ;
			
		} else {// 用户是否保存了录音
			if (mRecordManger.isSave()) {
				mIvRecord.setBackgroundResource(R.drawable.ic_record_pause);
				mRecordManger.startRecord();
				mIvRecordMenu.setBackgroundResource(R.drawable.ic_record_save);
				startTimeRecord();
				mRecordManger.setSave(false);
				Log.e("liujw","#####################isSave") ;
				
			} else {
				
				Log.e("liujw","#####################pauseRecord") ;
				int status = mRecordManger.pauseRecord();
				if (status) { // true 暂停录音
					mIvRecord.setBackgroundResource(R.drawable.ic_record);
					timer.cancel();
				} else { // false 开始恢复录音
					mIvRecordMenu.setBackgroundResource(R.drawable.ic_record_save);
					mIvRecord.setBackgroundResource(R.drawable.ic_record_pause);
					startTimeRecord();
				}
			}
		}
		isSave = false;
	}*/

	private int second = 0;

	private int minute = 0;

	private Timer timer;

	private void startTimeRecord() {

		TimerTask timerTask = new TimerTask() {

			@Override
			public void run() {
				second++;
				if (second >= 60) {
					second = 0;
					minute++;
				}
				mHandler.sendEmptyMessage(1);
			}
		};
		timer = new Timer();
		timer.schedule(timerTask, 1000, 1000);
	}

	private boolean isRecord = false;

	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED
					.equals(action)) {
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
					.equals(action)) {

			} else if (BluetoothLeService.ACTION_NOTIFY_DATA_AVAILABLE.equals(action)) {
		    	vibrator.vibrate(200);
		    	if(mMediaStatusCode == -1){
		    		mIvRecord.setBackgroundResource(R.drawable.ic_record_pause);
		    		mMediaStatusCode = mRecordManger.startRecord();
					mIvRecordMenu.setBackgroundResource(R.drawable.ic_record_save);
					isFirstRecord = false;
					startTimeRecord();
		    	}else{
		    		saveRecord();
		    		mMediaStatusCode = -1 ;
		    		isSave = true ;
		    	}
		    	
		    	
				/*if (!isSave) {
					saveRecord();
				} else {
					handlerRecord();
				}*/
			}
		}
	};

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_NOTIFY_DATA_AVAILABLE);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_RSSI);
		return intentFilter;
	}

}
