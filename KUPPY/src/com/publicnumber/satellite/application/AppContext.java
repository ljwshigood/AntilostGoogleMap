package com.publicnumber.satellite.application;


import java.util.ArrayList;
import java.util.HashMap;

import android.app.Application;
import android.bluetooth.BluetoothGatt;

import com.loopj.android.http.AsyncHttpClient;
import com.publicnumber.satellite.R;
import com.publicnumber.satellite.bean.DeviceSetInfo;
import com.publicnumber.satellite.bean.NotificationBean;
import com.publicnumber.satellite.bean.alarmInfo;
import com.publicnumber.satellite.service.BluetoothLeService;
import com.publicnumber.satellite.util.CrashHandler;

public class AppContext extends Application {

	public static String mDeviceAddress;

	int[] mRingResId = new int[10];
	
	String[] mRingString = new String[10];
	
	public static  boolean isAlarm = true ;
	
	public static ArrayList<alarmInfo> mAlarmList = new ArrayList<alarmInfo>();
	
	public static boolean isShow = true ;
	
	public static  HashMap<String, BluetoothGatt> mHashMapConnectGatt = new HashMap<String, BluetoothGatt>();
	
	public static ArrayList<DeviceSetInfo> mDeviceList = new ArrayList<DeviceSetInfo>();
	
	public static int mCurrentTab = 0 ;
	
	public static BluetoothLeService mBluetoothLeService;
	
	public static boolean isEndMusic ;
	
	public int mBatteryValues = 90 ;
	
	public static NotificationBean mNotificationBean = new NotificationBean();
	
	public static int[] mDeviceStatus = {0,0};
	
	public boolean isExistDeviceConnected = false;
	
	public static AsyncHttpClient asyncHttpClient;
	
	public static double mLongitude = 0.0 ;
	
	public static double mLatitude = 0.0;
	
	public static boolean isStart = true ;
	
	public static boolean isFlash = true;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		/*CrashHandler crashHandler = CrashHandler.getInstance();
		crashHandler.init(getApplicationContext());
		*/
		asyncHttpClient = new AsyncHttpClient();
		asyncHttpClient.setTimeout(120000);
		
		initData();
	}

	private void initData() {
		mRingResId[0] = R.raw.crickets;
		mRingResId[1] = R.raw.ascending;
		mRingResId[2] = R.raw.bark;
		mRingResId[3] = R.raw.blues;
		mRingResId[4] = R.raw.alarm;
		mRingResId[5] = R.raw.marimba;
		mRingResId[6] = R.raw.motorcycle;
		mRingResId[7] = R.raw.oldphone;
		mRingResId[8] = R.raw.sirens;
		mRingResId[9] = R.raw.sonar;
		
	
		mRingString[0]= this.getString(R.string.ringset_qsmusic);
		mRingString[1]= this.getString(R.string.ringset_jqy);
		mRingString[2]= this.getString(R.string.ringset_gf);
		mRingString[3]= this.getString(R.string.ringset_ldmusic);
		mRingString[4]= this.getString(R.string.ringset_alarm);
		mRingString[5]= this.getString(R.string.ringset_mlmusic);
		mRingString[6]= this.getString(R.string.ringset_moto);
		mRingString[7]= this.getString(R.string.ringset_oldtele);
		mRingString[8]= this.getString(R.string.ringset_jdmusic);
		mRingString[9]= this.getString(R.string.ringset_snmusic);
		
		for(int i = 0 ;i < mRingResId.length;i++){
			alarmInfo info = new alarmInfo(mRingResId[i],mRingString[i],i,false);
			mAlarmList.add(info);
		}
	}

}
