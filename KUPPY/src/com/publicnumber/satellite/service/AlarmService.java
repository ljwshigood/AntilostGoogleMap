package com.publicnumber.satellite.service;


import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.publicnumber.satellite.R;
import com.publicnumber.satellite.activity.KeySetActivity;
import com.publicnumber.satellite.activity.MainFollowActivity;
import com.publicnumber.satellite.application.AppContext;
import com.publicnumber.satellite.bean.DeviceSetInfo;
import com.publicnumber.satellite.bean.DisturbInfo;
import com.publicnumber.satellite.bean.KeySetBean;
import com.publicnumber.satellite.db.DatabaseManager;
import com.publicnumber.satellite.util.AlarmManager;
import com.publicnumber.satellite.util.Constant;
import com.publicnumber.satellite.util.KeyFunctionUtil;
import com.publicnumber.satellite.util.ScreenObserver;
import com.publicnumber.satellite.view.FollowAlarmActivity;

public class AlarmService extends Service implements ConnectionCallbacks,
													OnConnectionFailedListener,
													OnMyLocationButtonClickListener, 
													com.google.android.gms.location.LocationListener{

	
	
	private AlarmManager mAlarmManager;

	private DatabaseManager mDatabaseManager;

	private Context mContext;

	private BluetoothAdapter mBluetoothAdapter;

	private Timer mTotalTimer = null;
	
	private static boolean isClick = false;
	
	private int mClickCount = 0;
	
	private static ClickOneTimer mClickOneTimer = null;
	
	private static ClickOthersTimer mClickOthersTimer = null ;
	
	private void createClickTimer(){
		if(mClickOneTimer == null){
			mClickOneTimer = new ClickOneTimer();
			mTotalTimer.schedule(mClickOneTimer,800);
		}
	}
	
	private void createClickStaticTimer(){
		if(mClickOthersTimer == null){
			mClickOthersTimer = new ClickOthersTimer();
			mTotalTimer.schedule(mClickOthersTimer, 2000);
		}
	}
	
	private void cancelClickTimer(){
		if(mClickOneTimer != null){
			mClickOneTimer.cancel();
			mClickOneTimer = null ;
		}
	}
	
	private void cancelStaticClickTimer(){
		if(mClickOthersTimer != null){
			mClickOthersTimer.cancel();
			mClickOthersTimer = null ;
		}
	}
	
	
	class ClickOthersTimer extends TimerTask {

		@Override
		public void run() {
			mHandlerOneClick.sendEmptyMessage(1);
		}

	}
	
	private void progressClickStatic(){
		try {
			
			KeySetBean bean = DatabaseManager.getInstance(mContext).selectKeySetByCount(mClickCount);
			KeyFunctionUtil.getInstance(mContext).actionKeyFunction(mContext,bean.getAction());	
			isClick = false;
			mClickCount = 0;
			Log.e("HeadSetRecevier","########################mClickCount : "+mClickCount);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	class ClickOneTimer extends TimerTask {

		@Override
		public void run() {
			mHandlerOneClick.sendEmptyMessage(0);
		}
	};
	
	private void dismissBleActivity() {
		Intent intent = new Intent(Constant.DIALOG_FINISH);
		sendBroadcast(intent);
	}
	
	public void alarmDialog(Context context ,DeviceSetInfo info,String alarmInfo, int type) {
		Intent intent = null ;
		
		Log.v("alarmService","###################alarmDialog");
		switch (type) {
		case Constant.DISCONNECT:
			dismissBleActivity();
			intent = new Intent(context,FollowAlarmActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra("type", type);
			intent.putExtra("alarm_info", alarmInfo);
			intent.putExtra("deviceinfo", info);
			startActivity(intent);
			break;
		case Constant.DISTANCE:
			
			dismissBleActivity();
			
			intent = new Intent(context,FollowAlarmActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra("type", type);
			intent.putExtra("alarm_info", alarmInfo);
			intent.putExtra("deviceinfo", info);
			startActivity(intent);
			break;
		case Constant.SENDDATA:
			
			dismissBleActivity();
			intent = new Intent(context,FollowAlarmActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra("type", type);
			intent.putExtra("alarm_info", alarmInfo);
			intent.putExtra("deviceinfo", info);
			startActivity(intent);
			
			break;
		case Constant.READBATTERY:
			
			dismissBleActivity();
			
			intent = new Intent(context,FollowAlarmActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra("type", type);
			intent.putExtra("alarm_info", alarmInfo);
			intent.putExtra("deviceinfo", info);
			startActivity(intent);
			break;
		default:
			break;
		}
	}
	
	private void progressTopTaskDeviceSendData(Intent intent) {
		String address = mIntent.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
		ArrayList<DeviceSetInfo>  list = mDatabaseManager.selectDeviceInfo(address);
		DeviceSetInfo info = null ;
		if(list.size() > 0){
			info  = list.get(0);
		}
		if(info != null){
			alarmDialog(mContext,info,mContext.getString(R.string.device_found_mobile),Constant.SENDDATA);
			Intent intentDistance = new Intent(BgMusicControlService.CTL_ACTION);
			intentDistance.putExtra("control", 1);
			intentDistance.putExtra("address", address);
			sendBroadcast(intentDistance);
		}
		
	}
	
	private boolean isShow = true ;
	
	Handler mHandlerOneClick = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case 0:
				if (!mAlarmManager.isApplicationBroughtToBackground(mContext)) { // app 在前台
					
					if(AppContext.isShow){
						progressTopTaskDeviceSendData(mIntent);
						AppContext.isShow = false ;
					}else{
						String address = mIntent.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
						dismissBleActivity();
						Intent intentDistance = new Intent(BgMusicControlService.CTL_ACTION);
						intentDistance.putExtra("control", 2);
						intentDistance.putExtra("address", address);
						sendBroadcast(intentDistance);
						AppContext.isShow = true ;
					}
					
				}else if((mAlarmManager.isApplicationBroughtToBackground(mContext))) {
					if(isShow){
						progressDeviceSendData(mIntent);
						isShow = false ;
					}else{
						String address = mIntent.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
						Intent intentDistance = new Intent(BgMusicControlService.CTL_ACTION);
						intentDistance.putExtra("control", 2);
						intentDistance.putExtra("address", address);
						sendBroadcast(intentDistance);
						isShow = true ;
					}
				}
				
				cancelClickTimer();
				isClick = false;
				mClickCount = 0 ;
				break;
			case 1:
				cancelStaticClickTimer();
				progressClickStatic();
				break ;
			default:
				break;
			}
			
		}
		
	};
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

			@Override
			public void onServiceConnected(ComponentName componentName,IBinder service) {
				AppContext.mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
				
				Log.e("AlarmService","####################onServiceConnected");

				if (!AppContext.mBluetoothLeService.initialize()) {
					stopSelf();
				}
			}

			@Override
			public void onServiceDisconnected(ComponentName componentName) {
				
				Log.e("liujw","####################onServiceDisconnected");
				
				AppContext.mBluetoothLeService = null;
			}
	};
		
	private boolean isBind ;
			
	@Override
	public void onCreate() {
		
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		isBind = this.getApplicationContext().bindService(gattServiceIntent,mServiceConnection, BIND_AUTO_CREATE);
		
		mContext = AlarmService.this;
		mAlarmManager = AlarmManager.getInstance(mContext);
		mDatabaseManager = DatabaseManager.getInstance(mContext);
		BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_SCREEN_ON);
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

		Log.e("liujw", "######################alarmServcie : oncreate");
		
		mTotalTimer = new Timer(true);
		
		setUpLocationClientIfNeeded();
		mLocationClient.connect();
		pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
		
	
	};
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		scanLeDevice(true);
		return super.onStartCommand(intent, flags, startId);
	}
	
	private KeyguardLock mKeyguardLock = null;    

	private ScreenObserver mScreenObserver;
	
	private PowerManager pm;  
	
	private PowerManager.WakeLock wakeLock;  
	
	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_NOTIFY_DATA_AVAILABLE);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_RSSI);
		return intentFilter;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if(isBind){
			getApplicationContext().unbindService(mServiceConnection);	
		}
		
		KeyFunctionUtil.getInstance(mContext).releaseWake();
		
		unregisterReceiver(mGattUpdateReceiver);
		
		if (manager != null) {
			manager.cancel(NOTICE_ID);
		}
		scanLeDevice(false);
		
		mHandlerAudioBattery.removeCallbacks(autoReadBatteryRunable);
		Log.e("liujw", "####################alarmServie onDestroy");
	}

	private ArrayList<DeviceSetInfo> mDeviceList = new ArrayList<DeviceSetInfo>();
	
	private final static String TAG = "AlarmService" ;

	private Handler mHandler = new Handler();
	
	private Handler mHandlerAudioBattery = new Handler();
	
	private int readBatteryCount = 0 ;
	
	Runnable autoReadBatteryRunable = new Runnable() {
		
		@Override
		public void run() {
			readBatteryCount++ ;
			if(readBatteryCount > 5){
				return ;
			}
			if(AppContext.mBluetoothLeService != null){
				AppContext.mBluetoothLeService.readBatteryCharacteristic();
			}
			mHandlerAudioBattery.postDelayed(autoReadBatteryRunable, 3000);
		}
	};
	

	private static final long SCAN_PERIOD = 30000;

	private void scanLeDevice(final boolean enable) {
		if (enable) {
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		} else {
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}
	}
	
	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, int rssi,
				byte[] scanRecord) {
			new Thread() {
				public void run() {
					mDeviceList = mDatabaseManager.selectDeviceInfo();
					for (int i = 0; i < mDeviceList.size(); i++) {
						DeviceSetInfo info = mDeviceList.get(i);
						String address = info.getmDeviceAddress();
						if (info.isActive() && device.getAddress().equals(address)) {
							if (AppContext.mBluetoothLeService != null) {
								
								Log.v("liujw","##############################address onLeScan "+address);
								
								AppContext.mBluetoothLeService.connect(device.getAddress());
							}
							break;
						}
					}
				};
			}.start();
		}
	};
	
	private void progressTopTaskDeviceDisconnect(Intent intent) {
		Log.v("AlarmService","########################progressTopTaskDeviceDisconnect");
		DeviceSetInfo info = null ;
		String address = intent.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
		DatabaseManager manager = DatabaseManager.getInstance(mContext);
		manager.updateDeviceLatLogDisconnect(String.valueOf(AppContext.mLatitude), String.valueOf(AppContext.mLongitude), address);
		ArrayList<DeviceSetInfo>  list = manager.selectDeviceInfo(address);
		if(list.size() > 0){
			info  = list.get(0);
		}
		AppContext.mBluetoothLeService.close();
		if(info != null && info.isActive()){
			boolean isDisconnect = mAlarmManager.DeviceDisconnectAlarm(info, address,mContext.getString(R.string.device_disconnect));
			if(isDisconnect){
				alarmDialog(mContext,info, mContext.getString(R.string.device_disconnect),Constant.DISCONNECT);	
			}
		}
	}
	
	
	private Intent mIntent ;
	
	Runnable mDisconnectRunnable = new Runnable() {
		
		@Override
		public void run() {
			// 处理后台运行的情况
			AppContext.mBluetoothLeService.close();
			if(mIntent == null){
				return ;
			}
			if (!mAlarmManager.isApplicationBroughtToBackground(mContext)) {
				progressTopTaskDeviceDisconnect(mIntent);
			}else if (mAlarmManager.isApplicationBroughtToBackground(mContext)) {
				progressDeviceDisconnect(mIntent);
			}
		}
	};
	
	
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			
			final String action = intent.getAction();
			mIntent = intent ;
			String address = intent.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
			if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
				
				mClickCount = 0 ;
				cancelClickTimer();
				cancelStaticClickTimer();
				
				mHandler.postDelayed(mDisconnectRunnable, 3000);
				mHandlerAudioBattery.removeCallbacks(autoReadBatteryRunable);
				
			} else if (BluetoothLeService.ACTION_GATT_RSSI.equals(action)) { // 超距离报警
				if (!mAlarmManager.isApplicationBroughtToBackground(mContext)) {
					progressTopTaskRssi(intent);
				}else if (mAlarmManager.isApplicationBroughtToBackground(mContext)) {
					progressRssi(intent);
				}
			} else if (BluetoothLeService.ACTION_NOTIFY_DATA_AVAILABLE.equals(action)) { //设备寻找手机报警
				if(AppContext.isAlarm == false){
					return ;
				}
				mClickCount++ ;
				Log.e("liujw","###########################ACTION_NOTIFY_DATA_AVAILABLE: click count : "+mClickCount); 
				if(isClick){
					cancelClickTimer();
					createClickStaticTimer();
				}else{
					cancelStaticClickTimer();
					createClickTimer();
					isClick = true;
				}
				
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				
				mHandler.removeCallbacks(mDisconnectRunnable);
				
				if (AppContext.mBluetoothLeService != null) {
					displayGattServices(AppContext.mBluetoothLeService.getSupportedGattServices(),address);
				}
				
				DatabaseManager.getInstance(mContext).updateDeviceConnect(address);
				
				dismissBleActivity();
				Intent intentDistance = new Intent(BgMusicControlService.CTL_ACTION);
				intentDistance.putExtra("control", 2);
				intentDistance.putExtra("address", address);
				sendBroadcast(intentDistance);
				
				AppContext.mNotificationBean.setShowNotificationDialog(false);
				
				Log.e("AlarmService","##########################BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED");
			}else if(BluetoothLeService.ACTION_READ_DATA_AVAILABLE.equals(action)){
				byte[] msg = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
				if (msg != null) {
					String message = msg.toString();
					if(Integer.parseInt(message) < 30){
						notifycationAlarm(mContext, address,
								mContext.getString(R.string.battery),
								Constant.READBATTERY);
					}
				}
			}
		}
	};

	@SuppressLint("NewApi")
	private void displayGattServices(List<BluetoothGattService> gattServices,
			String address) {
		if (gattServices == null) {
			return;
		}
		for (BluetoothGattService gattService : gattServices) {
			if (gattService.getUuid().toString().startsWith("00001802")) {
				List<BluetoothGattCharacteristic> gattCharacteristics = gattService
						.getCharacteristics();
				for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
					if (gattCharacteristic.getUuid().toString()
							.startsWith("00002a06")) {

					}
				}
			} else if (gattService.getUuid().toString().startsWith("0000ffe0")) {
				List<BluetoothGattCharacteristic> gattCharacteristics = gattService
						.getCharacteristics();
				for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
					if (gattCharacteristic.getUuid().toString().startsWith("0000ffe1")) {
						mHandlerAudioBattery.removeCallbacks(autoReadBatteryRunable);
						mHandlerAudioBattery.post(autoReadBatteryRunable);
						AppContext.mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
					}
				}
			}
			
			
		}
	}

	private void progressDeviceDisconnect(Intent intent) {
		DeviceSetInfo info = null;
		String address = intent.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
		DatabaseManager manager = DatabaseManager.getInstance(mContext);
		manager.updateDeviceLatLogDisconnect(String.valueOf(AppContext.mLatitude), String.valueOf(AppContext.mLongitude), address);
		ArrayList<DeviceSetInfo> list = manager.selectDeviceInfo(address);
		if (list.size() > 0) {
			info = list.get(0);
		}
		if (info != null && info.isActive()) {
			boolean isAlarm = mAlarmManager.DeviceDisconnectAlarm(info,
					address, mContext.getString(R.string.device_disconnect));
			if (isAlarm) {
				notifycationAlarm(mContext, address,
						mContext.getString(R.string.device_disconnect),
						Constant.DISCONNECT);
			}
			AppContext.mHashMapConnectGatt.remove(address);
		}
	}

	private void progressTopTaskRssi(Intent intent) {
		int  rssi = intent.getIntExtra(BluetoothLeService.EXTRA_DATA, 0);
		String address = intent.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
		ArrayList<DeviceSetInfo> deviceList = mDatabaseManager.selectDeviceInfo(address);
		ArrayList<DisturbInfo> disturbList = mDatabaseManager.selectDisturbInfo(address);
		if(deviceList.size() > 0){
			boolean isMoreDistance = mAlarmManager.isDeviceMoreDistance(rssi, address,deviceList.get(0),
																		disturbList.get(0));	
			if(isMoreDistance){
				AppContext.mDeviceStatus[0] = 1;
				mAlarmManager.isMoreDistanceAlarm(address,deviceList.get(0),disturbList.get(0));
				alarmDialog(mContext,deviceList.get(0), mContext.getString(R.string.device_more_distance),Constant.DISTANCE);
			}else{
				
				AppContext.mDeviceStatus[1] = 1;
				
				if(AppContext.mDeviceStatus[0] == 1 && AppContext.mDeviceStatus[1] == 1){
					Intent intentDistance = new Intent(BgMusicControlService.CTL_ACTION);
					intentDistance.putExtra("control", 2);
					intentDistance.putExtra("address", address);
					sendBroadcast(intentDistance);
					dismissBleActivity();
					AppContext.mDeviceStatus[0] = 0;
					AppContext.mDeviceStatus[1] = 0;
				}
			}
		}
	}
	
	private void progressRssi(Intent intent) {
		int rssi = intent.getIntExtra(BluetoothLeService.EXTRA_DATA, 0);
		String address = intent.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
		ArrayList<DeviceSetInfo> deviceList = mDatabaseManager.selectDeviceInfo(address);
		ArrayList<DisturbInfo> disturbList = mDatabaseManager.selectDisturbInfo(address);
		if (deviceList.size() > 0) {
			boolean isMoreDistance = mAlarmManager.isDeviceMoreDistance(rssi,
					address, deviceList.get(0), disturbList.get(0));
			if (isMoreDistance) {
				boolean flag = mAlarmManager.isMoreDistanceAlarm(address,
						deviceList.get(0), disturbList.get(0));
				if (flag) {
					AppContext.mDeviceStatus[0] = 1;
					notifycationAlarm(mContext, address,
							mContext.getString(R.string.device_more_distance),
							Constant.DISTANCE);
				}
			} else {

				AppContext.mDeviceStatus[1] = 1;

				if (AppContext.mDeviceStatus[0] == 1
						&& AppContext.mDeviceStatus[1] == 1) {
					Intent intentDistance = new Intent(
							BgMusicControlService.CTL_ACTION);
					intentDistance.putExtra("control", 2);
					intentDistance.putExtra("address", address);
					sendBroadcast(intentDistance);
					AppContext.mNotificationBean.setShowNotificationDialog(false);
					AppContext.mDeviceStatus[0] = 0;
					AppContext.mDeviceStatus[1] = 0;
				}
			}
		}
	}

	private static final int NOTICE_ID = 1222;

	private NotificationManager manager;

	public void notifycationAlarm(Context context, String address,String string, int type) {

		if (address == null) {
			Intent intent = new Intent(context, MainFollowActivity.class);
			manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			Notification notification = new Notification(R.drawable.ic_launcher,
					mContext.getString(R.string.notify_alarm),System.currentTimeMillis());
			PendingIntent pendIntent = PendingIntent.getActivity(context, 0,intent, 0);
			notification.setLatestEventInfo(context, "KUPPY", string,pendIntent);
			manager.notify(NOTICE_ID, notification);
			AppContext.mNotificationBean.setShowNotificationDialog(false);
			AppContext.mNotificationBean.setNotificationID(NOTICE_ID);
		} else {
			Intent intent = new Intent(context, KeySetActivity.class);
			manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			Notification notification = new Notification(R.drawable.ic_launcher,mContext.getString(R.string.notify_alarm),
					System.currentTimeMillis());
			PendingIntent pendIntent = PendingIntent.getActivity(context, 0,intent, 0);
			notification.setLatestEventInfo(context, "KUPPY", string,pendIntent);
			manager.notify(NOTICE_ID, notification);
			AppContext.mNotificationBean.setAddress(address);
			AppContext.mNotificationBean.setShowNotificationDialog(true);
			AppContext.mNotificationBean.setAlarmInfo(string);
			AppContext.mNotificationBean.setNotificationID(NOTICE_ID);
			AppContext.mNotificationBean.setAlarmType(type);
		}
	}

	private void progressDeviceSendData(Intent intent) {
		
		if(mIntent == null){
			return ;
		}
		
		String deviceAddress = intent.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
		ArrayList<DeviceSetInfo> list = mDatabaseManager
				.selectDeviceInfo(deviceAddress);
		DeviceSetInfo info = null;
		if (list.size() > 0) {
			info = list.get(0);
		}
		if (info != null) {
			notifycationAlarm(mContext, deviceAddress,
					mContext.getString(R.string.device_found_mobile),
					Constant.SENDDATA);
			Intent intentDistance = new Intent(BgMusicControlService.CTL_ACTION);
			intentDistance.putExtra("control", 1);
			intentDistance.putExtra("address", deviceAddress);
			sendBroadcast(intentDistance);
		}
	}
	
	private LocationClient mLocationClient;

    // These settings are the same as the settings for the map. They will in fact give you updates
    // at the maximal rates currently possible.
	private static final LocationRequest REQUEST = LocationRequest.create()
        .setInterval(5000)         // 5 seconds
        .setFastestInterval(16)    // 16ms = 60fps
        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

	 private void setUpLocationClientIfNeeded() {
	        if (mLocationClient == null) {
	            mLocationClient = new LocationClient(
	                    getApplicationContext(),
	                    this,  // ConnectionCallbacks
	                    this); // OnConnectionFailedListener
	        }
	    }
	
	@Override
	public void onLocationChanged(Location location) {
		AppContext.mLatitude = location.getLatitude();
		AppContext.mLongitude = location.getLongitude();
		
		DatabaseManager.getInstance(mContext).updateDeviceLatLogIsConnect(String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()));
		
		Log.e("AlarmService","###################################location.getLatitude() "+location.getLatitude());
		Log.e("AlarmService","###################################location.getLatitude() "+location.getLatitude());
		Log.e("AlarmService","###################################location.getLatitude() "+location.getLatitude());
		Log.e("AlarmService","###################################location.getLatitude() "+location.getLatitude());
		
		
		Log.e("AlarmService","###################################location.getLatitude() "+location.getLongitude());
		Log.e("AlarmService","###################################location.getLatitude() "+location.getLongitude());
		Log.e("AlarmService","###################################location.getLatitude() "+location.getLongitude());
		Log.e("AlarmService","###################################location.getLatitude() "+location.getLongitude());
	}

	@Override
	public boolean onMyLocationButtonClick() {
		return false;
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		
	}

	@Override
	public void onConnected(Bundle arg0) {
		  mLocationClient.requestLocationUpdates(
	                REQUEST,
	                this);  // LocationListener
	}

	@Override
	public void onDisconnected() {
		
	}
}
