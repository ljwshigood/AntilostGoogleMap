package com.publicnumber.satellite.activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.publicnumber.satellite.R;
import com.publicnumber.satellite.adapter.DeviceAdapter;
import com.publicnumber.satellite.application.AppContext;
import com.publicnumber.satellite.bean.DeviceSetInfo;
import com.publicnumber.satellite.bean.DisturbInfo;
import com.publicnumber.satellite.db.DatabaseManager;
import com.publicnumber.satellite.service.BluetoothLeService;
import com.publicnumber.satellite.util.AlarmManager;
import com.publicnumber.satellite.util.BlueGattManager;
import com.publicnumber.satellite.util.ImageTools;
import com.publicnumber.satellite.util.LocationUtils;
import com.publicnumber.satellite.view.FollowEditDialog;
import com.publicnumber.satellite.view.FollowEditDialog.ICallbackUpdateView;
import com.publicnumber.satellite.view.FollowInfoDialog;
import com.publicnumber.satellite.view.SelectPicPopupWindow;

public class DeviceDisplayActivity extends BaseActivity implements OnClickListener, ICallbackUpdateView {

	private ImageView mIvAddDevice;

	private Context mContext;

	private DeviceAdapter mDeviceAdapter;

	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";

	public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

	public final static int RESULT_ADRESS = 1000;

	private static String TAG = "DeviceDisplayActivity";

	private CheckBox mCbOpenShock;

	private BluetoothAdapter mBluetoothAdapter;

	public ProgressDialog mDialogProgress = null;

	private RelativeLayout mRlTitle;

	private void showShakeDialog(String str) {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(str)
				.setCancelable(true)
				.setPositiveButton(mContext.getString(R.string.ok),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								if (mCbOpenShock.isChecked()) {
									mCbOpenShock.setChecked(true);
									SharedPreferences settings = mContext
											.getSharedPreferences("config", 0);
									SharedPreferences.Editor editor = settings
											.edit();
									editor.putBoolean("switch", true);
									editor.commit();
								} else {
									mCbOpenShock.setChecked(false);
									SharedPreferences settings = mContext
											.getSharedPreferences("config", 0);
									SharedPreferences.Editor editor = settings
											.edit();
									editor.putBoolean("switch", false);
									editor.commit();
								}
								DeviceDisplayActivity.this.setShakeConfig();
							}
						})
				.setNegativeButton(mContext.getString(R.string.alarm_cancel),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
								if (mCbOpenShock.isChecked()) {
									mCbOpenShock.setChecked(false);
								} else {
									mCbOpenShock.setChecked(true);
								}
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private ArrayList<DeviceSetInfo> mDeviceList = new ArrayList<DeviceSetInfo>();

	private DatabaseManager mDatabaseManager;

	public void updateDeviceAdapter(String address) {
		for (int i = 0; i < mDeviceList.size(); i++) {
			DeviceSetInfo info = mDeviceList.get(i);
			if (info.getmDeviceAddress().equals(address)) {
				info.setActive(true);
				info.setConnected(true);
				info.setVisible(false);
				mDatabaseManager.updateDeviceActiveStatus(info.getmDeviceAddress(), info);
			}
		}
	}

	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

				String address = intent.getStringExtra(BluetoothDevice.EXTRA_DEVICE);

				Log.e("liujw","#########################ACTION_GATT_SERVICES_DISCOVERED");

				if (AppContext.mBluetoothLeService != null) {
					displayGattServices(AppContext.mBluetoothLeService.getSupportedGattServices(),address);
				}
				updateDeviceAdapter(address);
				if(mPbbar != null){
					mPbbar.setVisibility(View.GONE);
				}

			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {

				Log.e("liujw","#########################ACTION_GATT_DISCONNECTED");
				String address = intent.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
				//AppContext.mHashMapConnectGatt.remove(address);
				mPbbar.setVisibility(View.VISIBLE);

			} else if (BluetoothLeService.ACTION_READ_DATA_AVAILABLE.equals(action)) {
				progressBatteryData(intent);
			}
		}
	};

	Runnable mRunable = new Runnable() {
		@Override
		public void run() {

			for (int i = 0; i < mDeviceList.size(); i++) {
				DeviceSetInfo info = mDeviceList.get(i);
				info.setVisible(false);
			}
			mDeviceAdapter.notifyDataSetChanged();
		}
	};

	private RelativeLayout mLlNotDisturb;

	private RelativeLayout mLLSound;

	private Intent mIntent;

	private DeviceSetInfo mDeviceSetInfo;

	private String mDeviceAddress;

	SelectPicPopupWindow menuWindow;

	private ImageView mIvDevicePhoto;

	private LinearLayout mLLDeviceDelete;

	private CheckBox mCbLocation;

	private EditText mEtName;

	public void editNameDialog() {
		FollowEditDialog dialog = new FollowEditDialog(mContext,
				R.style.MyDialog, mContext.getString(R.string.set_name),
				mDeviceSetInfo, mDeviceAddress, DeviceDisplayActivity.this);
		dialog.show();

	}

	private ImageView mIvDistanceInfo;

	private LinearLayout mLLDeviceName;

	private AlarmManager mAlarmManager;

	private TextView mTvDeviceInfo;

	private ImageView mIvAlarm;

	private ProgressBar mPbbar;

	private CheckBox mCbDisturb;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device);
		mDatabaseManager = DatabaseManager.getInstance(mContext);
		mAlarmManager = AlarmManager.getInstance(mContext);

		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
			finish();
		}
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		if (mBluetoothAdapter == null) {
			Toast.makeText(this, R.string.error_bluetooth_not_supported,Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		mContext = DeviceDisplayActivity.this;
		mDeviceSetInfo = mDatabaseManager.selectSingleDeviceInfo();

		initView();

		if (mDeviceSetInfo != null) {

			mDeviceAddress = mDeviceSetInfo.getmDeviceAddress();
			AppContext.mDeviceAddress = mDeviceAddress;
			initData();
			initDeviceDistance(mDeviceSetInfo.getDistanceType());
			mRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(RadioGroup radioGroup,
								int arg1) {
							int radioButtonId = radioGroup
									.getCheckedRadioButtonId();
							RadioButton rb = (RadioButton) findViewById(radioButtonId);
							if (rb == mRbFar) {
								mDeviceSetInfo.setDistanceType(2);
							} else if (rb == mRbMiddle) {
								mDeviceSetInfo.setDistanceType(1);
							} else if (rb == mRbNear) {
								mDeviceSetInfo.setDistanceType(0);
							}
							mDatabaseManager.updateDeviceInfo(mDeviceAddress,mDeviceSetInfo);
						}
			});
			initDeviceListInfo();
		}
		
	}

	private RadioGroup mRadioGroup;

	private RadioButton mRbNear;

	private RadioButton mRbFar;

	private RadioButton mRbMiddle;

	private void initDeviceDistance(int type) {
		switch (type) {
		case 0:
			mRbNear.setChecked(true);
			break;
		case 1:
			mRbMiddle.setChecked(true);
			break;
		case 2:
			mRbFar.setChecked(true);
			break;

		default:
			break;
		}
	}
	
	@SuppressLint("NewApi")
	private void displayGattServices(List<BluetoothGattService> gattServices,String address) {
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
						AppContext.mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
					}
				}
			}
		}
	}

	public void initDeviceListInfo() {

		mDeviceList = mDatabaseManager.selectDeviceInfo();
		
		if(AppContext.mBluetoothLeService != null && AppContext.mBluetoothLeService.isConnect()){
			if(mDeviceList != null && mDeviceList.size() > 0){
				mDeviceSetInfo =  mDeviceList.get(0);
				mDeviceSetInfo.setConnected(true);
				mDeviceSetInfo.setVisible(false);
			}
		}

		if (mDeviceSetInfo.isActive()) {
			if (mDeviceSetInfo.isConnected()) {
				mIvAlarm.setBackgroundResource(R.drawable.ic_alarm_enable);
			} else {
				mIvAlarm.setBackgroundResource(R.drawable.ic_alarm_enable);
			}
		} else if (!mDeviceSetInfo.isActive()) {
			mIvAlarm.setBackgroundResource(R.drawable.ic_alarm_enable);
		}

		if (mDeviceSetInfo.isConnected()) {
			mPbbar.setVisibility(View.GONE);

		} else {
			mPbbar.setVisibility(View.VISIBLE);
		}

		mTvDeviceInfo.setText(mDeviceSetInfo.getmDeviceName());
		mEtName.setText(mDeviceSetInfo.getmDeviceName());

		mIvAlarm.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mDeviceList.get(0).isActive()
						&& mDeviceList.get(0).isConnected()) {
					if (AppContext.mBluetoothLeService != null) {
						AppContext.mBluetoothLeService.writeCharacter(mDeviceSetInfo.getmDeviceAddress());
					}
				}

			}
		});

		mTvDeviceInfo.setText(mDeviceSetInfo.getmDeviceName());
		mIvDevicePhoto.setImageBitmap(mAlarmManager.getConnectDeviceBitmap(mDeviceSetInfo, mContext));
	}

	private void initData() {
		
		ArrayList<DeviceSetInfo> list = mDatabaseManager.selectDeviceInfo(mDeviceAddress);
		
		if (list.size() > 0) {
			mDeviceSetInfo = mDatabaseManager.selectDeviceInfo(mDeviceAddress).get(0);
		}
		
		ArrayList<DisturbInfo> disturbList = mDatabaseManager.selectDisturbInfo(mDeviceAddress);
		if (disturbList == null) {
			return;
		}

		for (int i = 0; i < disturbList.size(); i++) {
			mDisturbInfo = disturbList.get(i);
		}

		if (mDisturbInfo.isDisturb()) {
			mCbDisturb.setChecked(true);
		} else {
			mCbDisturb.setChecked(false);
		}
	}

	private DisturbInfo mDisturbInfo;

	private TextView mTvBattery;

	private void progressBatteryData(Intent intent) {
		byte[] msg = intent.getByteArrayExtra(BluetoothLeService.BATTERY_DATA);
		Log.e("DeviceSetActivity", "######################onReceive " + msg[0]);
		if (msg != null) {
			updateBattery(msg[0]);
		}
	}

	protected void updateBattery(int current) {
		mTvBattery.setText(String.valueOf(current) + "%");
	}

	private LinearLayout mLLContent;

	private void initView() {
		
		mRlTitle = (RelativeLayout)findViewById(R.id.rl_title);
		mCbDisturb = (CheckBox)findViewById(R.id.cb_disturb);
		mTvDeviceInfo = (TextView) findViewById(R.id.tv_device_info);
		mIvAlarm = (ImageView)findViewById(R.id.iv_alarm);
		mPbbar = (ProgressBar)findViewById(R.id.pb_connect_device);
		mLLContent = (LinearLayout) findViewById(R.id.ll_content);
		mRlTitle.setBackgroundResource(R.drawable.bg_device_list) ;

		mCbDisturb.setOnClickListener(this);
		
		if (mDatabaseManager.selectSingleDeviceInfo() != null) {
			mLLContent.setVisibility(View.VISIBLE);
		} else {
			mLLContent.setVisibility(View.GONE);
		}

		mIvAddDevice = (ImageView) findViewById(R.id.iv_add_device);
		mCbOpenShock = (CheckBox) findViewById(R.id.cb_open_scan);
		mIvAddDevice.setOnClickListener(this);
		mCbOpenShock.setOnClickListener(this);

		SharedPreferences settings = mContext.getSharedPreferences("config", 0);
		if (settings.getBoolean("switch", true)) {
			mCbOpenShock.setChecked(true);
		} else {
			mCbOpenShock.setChecked(false);
		}

		mTvBattery = (TextView) findViewById(R.id.tv_battery);
		mIvDevicePhoto = (ImageView) findViewById(R.id.iv_device);
		if (mDeviceSetInfo != null) {
			Bitmap circleBitmap = mAlarmManager.getConnectDeviceBitmap(mDeviceSetInfo, mContext);
			mIvDevicePhoto.setImageBitmap(circleBitmap);

		}

		mRbNear = (RadioButton) findViewById(R.id.rb_jin);
		mRbMiddle = (RadioButton) findViewById(R.id.rb_middle);
		mRbFar = (RadioButton) findViewById(R.id.rb_yuan);

		mLLDeviceName = (LinearLayout) findViewById(R.id.ll_device);
		mLLDeviceName.setOnClickListener(this);
		mEtName = (EditText) findViewById(R.id.et_device_name);
		mEtName.setOnClickListener(this);
		mLlNotDisturb = (RelativeLayout) findViewById(R.id.ll_not_disturb);
		mLLSound = (RelativeLayout) findViewById(R.id.ll_sound);
		mLLDeviceDelete = (LinearLayout) findViewById(R.id.ll_delete_device);
		mCbLocation = (CheckBox) findViewById(R.id.cb_location_switch);
		mIvDistanceInfo = (ImageView) findViewById(R.id.iv_distance_info);
		mIvDistanceInfo.setOnClickListener(this);
		mLLDeviceDelete.setOnClickListener(this);
		mLlNotDisturb.setOnClickListener(this);
		mLLSound.setOnClickListener(this);
		mCbLocation.setOnClickListener(this);

		if (mDeviceSetInfo != null) {
			mCbLocation.setChecked(mDeviceSetInfo.isLocation());
			mCbLocation.setChecked(LocationUtils.isOPen(mContext));
			mEtName.setText(mDeviceSetInfo.getmDeviceName());
		}

		mEtName.setFocusable(false);
		mEtName.setFocusableInTouchMode(false);
		mRadioGroup = (RadioGroup) this.findViewById(R.id.radioGroup);
	}

	private static final int REQUESTCODE = 0;

	@Override
	protected void onDestroy() {
		super.onDestroy();
		ArrayList<DeviceSetInfo> deviceList = mDatabaseManager.selectDeviceInfo(mDeviceAddress);
		if (deviceList.size() > 0) {
			mDatabaseManager.updateDeviceInfo(mDeviceAddress, mDeviceSetInfo);
		}
	}

	private static final int REQUEST_ENABLE_BT = 1;

	@Override
	protected void onResume() {
		super.onResume();
		if (!mBluetoothAdapter.isEnabled()) {
			if (!mBluetoothAdapter.isEnabled()) {
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		}

		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		setShakeConfig();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_NOTIFY_DATA_AVAILABLE);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_RSSI);
		intentFilter.addAction(BluetoothLeService.ACTION_READ_DATA_AVAILABLE);
		return intentFilter;
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mGattUpdateReceiver);
	}

	public void takePhoto() {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		name = DateFormat.format("yyyyMMdd_hhmmss",
				Calendar.getInstance(Locale.CHINA))
				+ ".jpg";
		Uri imageUri = Uri.fromFile(new File(PATH, name));
		intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
		startActivityForResult(intent, CAMERA_TAKE);
	}

	private Bitmap mBitmap;

	private String mFilePath;

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUESTCODE && resultCode == DeviceDisplayActivity.RESULT_ADRESS) {
			mDeviceSetInfo = mDatabaseManager.selectSingleDeviceInfo();
			initView();
			if (mDeviceSetInfo != null) {
				mDeviceAddress = mDeviceSetInfo.getmDeviceAddress();
				AppContext.mDeviceAddress = mDeviceAddress;
				initData();
				initDeviceDistance(mDeviceSetInfo.getDistanceType());
				mRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
							@Override
							public void onCheckedChanged(RadioGroup radioGroup,int arg1) {
								int radioButtonId = radioGroup.getCheckedRadioButtonId();
								RadioButton rb = (RadioButton)findViewById(radioButtonId);
								if (rb == mRbFar) {
									mDeviceSetInfo.setDistanceType(2);
								} else if (rb == mRbMiddle) {
									mDeviceSetInfo.setDistanceType(1);
								} else if (rb == mRbNear) {
									mDeviceSetInfo.setDistanceType(0);
								}
								mDatabaseManager.updateDeviceInfo(mDeviceAddress, mDeviceSetInfo);
							}
						});
				if (mDatabaseManager.selectSingleDeviceInfo() != null) {
					initDeviceListInfo();
				}
			}

		} else if (requestCode == CAMERA_TAKE) {
			mFilePath = PATH + "/" + name;
			mBitmap = ImageTools.getBitmapFromFile(mFilePath, 8);
			if (mBitmap == null) {
				return;
			}
			
			mIvDevicePhoto.setImageBitmap(ImageTools.toRoundBitmap(mBitmap, 80));
			mDeviceSetInfo.setFilePath(PATH + "/" + name);
			mDatabaseManager.updateDeviceInfo(mDeviceAddress, mDeviceSetInfo);

		} else if (requestCode == CAMERA_SELECT) {
			ContentResolver resolver = getContentResolver();
			if (data == null) {
				return;
			}
			Uri imgUri = data.getData();
			try {
				mBitmap = MediaStore.Images.Media.getBitmap(resolver, imgUri);
				mIvDevicePhoto.setImageBitmap(ImageTools.toRoundBitmap(mBitmap,80));
				mDeviceSetInfo.setFilePath(getGalleryPhotoPath(imgUri));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			mDatabaseManager.updateDeviceInfo(mDeviceAddress, mDeviceSetInfo);
		}

	};

	private String getGalleryPhotoPath(Uri originalUri) {
		String[] proj = { MediaStore.Images.Media.DATA };
		Cursor cursor = managedQuery(originalUri, proj, null, null, null);
		int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		String path = cursor.getString(column_index);
		return path;
	}

	private void showWindows(int type) {

		menuWindow = new SelectPicPopupWindow(DeviceDisplayActivity.this,
				itemsOnClick, type);
		menuWindow.showAtLocation(this.findViewById(R.id.main), Gravity.BOTTOM
				| Gravity.CENTER_HORIZONTAL, 0, 0);
	}

	// 为弹出窗口实现监听类
	private OnClickListener itemsOnClick = new OnClickListener() {

		public void onClick(View v) {
			menuWindow.dismiss();
			switch (v.getId()) {
			case R.id.btn_take_photo:
				takePhoto();
				break;
			case R.id.btn_pick_photo:
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("image/*");
				startActivityForResult(intent, CAMERA_SELECT);
				break;
			case R.id.btn_delete:
				if (AppContext.mBluetoothLeService == null) {
					return;
				}
				mDatabaseManager.deleteAllDeviceInfo(mDeviceAddress);
				AppContext.mBluetoothLeService.close();
				finish();
				break;
			default:
				break;
			}
		}
	};

	public String name;

	private static final String PATH = Environment
			.getExternalStorageDirectory() + "/DCIM";

	private static final int CAMERA_TAKE = 1;

	private static final int CAMERA_SELECT = 2;

	private void updateDatabase() {
		ArrayList<DisturbInfo> list = mDatabaseManager.selectDisturbInfo(mDeviceAddress);
		if (list.size() > 0) {
			mDatabaseManager.updateDisturbIsDisturbInfo(mDeviceAddress,mDisturbInfo);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.iv_add_device:
			Intent intent = new Intent(mContext, DeviceScanActivity.class);
			startActivityForResult(intent, REQUESTCODE);
			break;
		case R.id.cb_open_scan:
			if (mCbOpenShock.isChecked())
				showShakeDialog(mContext.getString(R.string.isOpenShake));
			else
				showShakeDialog(mContext.getString(R.string.isCloseShake));
			break;
		case R.id.ll_device:
			showWindows(0);
			break;
		case R.id.iv_distance_info:
			
			FollowInfoDialog dialog = new FollowInfoDialog(mContext,
					R.style.MyDialog, mContext.getString(R.string.notify),
					mContext.getString(R.string.distance_info), 0);
			dialog.show();
			break;
		case R.id.et_device_name:
			editNameDialog();
			break;
		case R.id.cb_location_switch:
			if (mCbLocation.isChecked()) {
				if (!LocationUtils.isOPen(mContext)) {
					FollowInfoDialog dialogLocation = new FollowInfoDialog(
							mContext, R.style.MyDialog, null,
							mContext.getString(R.string.open_gps), 0);
					dialogLocation.show();
				}

			} else {
				if (LocationUtils.isOPen(mContext)) {

					FollowInfoDialog dialogLocation = new FollowInfoDialog(
							mContext, R.style.MyDialog, null,
							mContext.getString(R.string.close_gps), 1);
					dialogLocation.show();
				}
			}
			mDeviceSetInfo.setLocation(mCbLocation.isChecked());
			mDatabaseManager.updateDeviceInfo(mDeviceAddress, mDeviceSetInfo);
			break;
		case R.id.ll_delete_device:
			showWindows(1);
			break;
		case R.id.cb_disturb:
			if (mCbDisturb.isChecked()) {
				mDisturbInfo.setDisturb(true);
			} else {
				mDisturbInfo.setDisturb(false);
			}
			updateDatabase();
			break;
		case R.id.ll_not_disturb:
			if (mCbDisturb.isChecked()) {
				mIntent = new Intent(mContext, DonotDistubActivity.class);
				startActivity(mIntent);
			} else {
				Toast.makeText(mContext,
						mContext.getString(R.string.disturb_switch), 1).show();
			}

			break;
		case R.id.ll_sound:
			mIntent = new Intent(mContext, SoundActivity.class);
			startActivity(mIntent);
			break;
		case R.id.iv_back:
			finish();
			break;
		case R.id.tv_change_device:
			showWindows(0);
			break;
		default:
			break;
		}
	}

	@Override
	public void updateView() {
		initData();
		initView();
	};
}
