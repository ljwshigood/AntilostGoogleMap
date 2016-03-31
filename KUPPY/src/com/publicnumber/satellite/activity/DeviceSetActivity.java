package com.publicnumber.satellite.activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.publicnumber.satellite.R;
import com.publicnumber.satellite.application.AppContext;
import com.publicnumber.satellite.bean.DeviceSetInfo;
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

public class DeviceSetActivity extends BaseActivity implements OnClickListener,
		ICallbackUpdateView {

	private Context mContext;

	private LinearLayout mLlNotDisturb;

	private LinearLayout mLLSound;

	private Intent mIntent;

	private ImageView mIvBack;

	private DatabaseManager mDatabaseManager;

	private DeviceSetInfo mDeviceSetInfo;

	private String mDeviceAddress;

	SelectPicPopupWindow menuWindow;

	private TextView mTvChangePicView;

	private ImageView mIvDevicePhoto;

	private LinearLayout mLLDeviceDelete;

	private CheckBox mCbLocation;

	// private SeekBar mSbDistance;

	private EditText mEtName;

	public void editNameDialog() {
		FollowEditDialog dialog = new FollowEditDialog(mContext,
				R.style.MyDialog, mContext.getString(R.string.set_name),
				mDeviceSetInfo, mDeviceAddress, DeviceSetActivity.this);
		dialog.show();

	}

	private ImageView mIvDistanceInfo;

	private LinearLayout mLLDeviceName;

	private AlarmManager mAlarmManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device_set);
		this.mContext = DeviceSetActivity.this;
		mDatabaseManager = DatabaseManager.getInstance(mContext);
		mAlarmManager = AlarmManager.getInstance(mContext);
		getIntentExtra();
		initData();
		initView();
		setTitle(mContext.getString(R.string.device_setting));
		
		initDeviceDistance(mDeviceSetInfo.getDistanceType());
		mRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup radioGroup, int arg1) {
				int radioButtonId = radioGroup.getCheckedRadioButtonId();
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
	}

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

	private View mView;

	private TextView mTvTitleInfo;

	private void setTitle(String info) {
		mView = (View) findViewById(R.id.include_head);
		mTvTitleInfo = (TextView) mView.findViewById(R.id.tv_title_info);
		mTvTitleInfo.setText(info);
	}

	@Override
	protected void onResume() {
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		super.onResume();
	}
	
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			String address = intent
					.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_READ_DATA_AVAILABLE.equals(action)) {
				if (address.equals(mDeviceAddress)) {
					Log.e("DeviceSetActivity",
							"######################onReceive " + address);
					progressBatteryData(intent);
				}
			}

		}
	};

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter
				.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_NOTIFY_DATA_AVAILABLE);
		intentFilter.addAction(BluetoothLeService.ACTION_READ_DATA_AVAILABLE);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_RSSI);
		return intentFilter;
	}

	private void progressBatteryData(Intent intent) {
		byte[] msg = intent.getByteArrayExtra(BluetoothLeService.BATTERY_DATA);
		Log.e("DeviceSetActivity", "######################onReceive " + msg[0]);
		if (msg != null) {
			updateBattery(msg[0]);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	private void initData() {
		ArrayList<DeviceSetInfo> list = mDatabaseManager
				.selectDeviceInfo(mDeviceAddress);
		if (list.size() > 0) {
			mDeviceSetInfo = mDatabaseManager.selectDeviceInfo(mDeviceAddress).get(0);
		}
	}

	private void getIntentExtra() {
		Intent intent = getIntent();
		mDeviceAddress = intent.getStringExtra("address");
		AppContext.mDeviceAddress = mDeviceAddress;
	}

	public Bitmap getRes(String name) {
		ApplicationInfo appInfo = getApplicationInfo();
		int resID = getResources().getIdentifier(name, "drawable",
				appInfo.packageName);
		return BitmapFactory.decodeResource(getResources(), resID);
	}

	private TextView mTvBattery;

	protected void updateBattery(int current) {
		mTvBattery.setText(String.valueOf(current) + "%");
	}

	private RadioGroup mRadioGroup;

	private RadioButton mRbNear;

	private RadioButton mRbFar;

	private RadioButton mRbMiddle;

	private void initView() {
		mTvBattery = (TextView) findViewById(R.id.tv_battery);
		mIvDevicePhoto = (ImageView) findViewById(R.id.iv_device);
		Bitmap circleBitmap = mAlarmManager.getConnectDeviceBitmap(
				mDeviceSetInfo, mContext);
		mIvDevicePhoto.setImageBitmap(circleBitmap);

		mRbNear = (RadioButton) findViewById(R.id.rb_jin);
		mRbMiddle = (RadioButton) findViewById(R.id.rb_middle);
		mRbFar = (RadioButton) findViewById(R.id.rb_yuan);

		mLLDeviceName = (LinearLayout) findViewById(R.id.ll_device);
		mLLDeviceName.setOnClickListener(this);
		mEtName = (EditText) findViewById(R.id.et_device_name);
		mEtName.setOnClickListener(this);
		mLlNotDisturb = (LinearLayout) findViewById(R.id.ll_not_disturb);
		mLLSound = (LinearLayout) findViewById(R.id.ll_sound);
		mIvBack = (ImageView) findViewById(R.id.iv_back);
		mTvChangePicView = (TextView) findViewById(R.id.tv_change_device);
		mLLDeviceDelete = (LinearLayout) findViewById(R.id.ll_delete_device);
		mCbLocation = (CheckBox) findViewById(R.id.cb_location_switch);
		mIvDistanceInfo = (ImageView) findViewById(R.id.iv_distance_info);
		mIvDistanceInfo.setOnClickListener(this);
		mLLDeviceDelete.setOnClickListener(this);
		mTvChangePicView.setOnClickListener(this);
		mIvBack.setOnClickListener(this);
		mLlNotDisturb.setOnClickListener(this);
		mLLSound.setOnClickListener(this);
		mCbLocation.setOnClickListener(this);
		mCbLocation.setChecked(mDeviceSetInfo.isLocation());
		mCbLocation.setChecked(LocationUtils.isOPen(mContext));
		mEtName.setText(mDeviceSetInfo.getmDeviceName());

		mEtName.setFocusable(false);
		mEtName.setFocusableInTouchMode(false);
		mRadioGroup = (RadioGroup) this.findViewById(R.id.radioGroup);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.ll_device:
			showWindows(0);
			break;
		case R.id.iv_distance_info:
			Log.e("liujw", "#####################iv_distance_info");
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
		case R.id.ll_not_disturb:
			mIntent = new Intent(mContext, DonotDistubActivity.class);
			startActivity(mIntent);
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

	private void showWindows(int type) {
		
		menuWindow = new SelectPicPopupWindow(DeviceSetActivity.this,itemsOnClick, type);
		// 显示窗口
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

	// 图片名
	public String name;

	private static final String PATH = Environment
			.getExternalStorageDirectory() + "/DCIM";
	private static final int CAMERA_TAKE = 1;
	private static final int CAMERA_SELECT = 2;

	public void takePhoto() {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);// 调用系统相机
		new DateFormat();
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
		if (requestCode == CAMERA_TAKE) {
			mFilePath = PATH + "/" + name;
			mBitmap = ImageTools.getBitmapFromFile(mFilePath, 8);
			if (mBitmap == null) {
				return;
			}
			mIvDevicePhoto
					.setImageBitmap(ImageTools.toRoundBitmap(mBitmap, 80));
			mDeviceSetInfo.setFilePath(PATH + "/" + name);

		} else {
			ContentResolver resolver = getContentResolver();
			if (data == null) {
				return;
			}
			Uri imgUri = data.getData();
			try {
				mBitmap = MediaStore.Images.Media.getBitmap(resolver, imgUri);
				mIvDevicePhoto.setImageBitmap(ImageTools.toRoundBitmap(mBitmap,
						80));
				mDeviceSetInfo.setFilePath(getGalleryPhotoPath(imgUri));

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		mDatabaseManager.updateDeviceInfo(mDeviceAddress, mDeviceSetInfo);
	};

	private String getGalleryPhotoPath(Uri originalUri) {
		String[] proj = { MediaStore.Images.Media.DATA };
		// 好像是android多媒体数据库的封装接口，具体的看Android文档
		Cursor cursor = managedQuery(originalUri, proj, null, null, null);
		// 按我个人理解 这个是获得用户选择的图片的索引值
		int column_index = cursor
				.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		// 将光标移至开头 ，这个很重要，不小心很容易引起越界
		cursor.moveToFirst();
		// 最后根据索引值获取图片路径
		String path = cursor.getString(column_index);
		return path;
	}

	private void saveBitmap(String filePath, Bitmap bm) {
		if (filePath == null || bm == null) {
			return;
		}
		File file = new File(filePath);
		if (file.exists()) {
			file.delete();
		}
		try {
			file.createNewFile();

		} catch (IOException e) {
			e.printStackTrace();
		}
		FileOutputStream fout = null;
		try {
			fout = new FileOutputStream(file);
			bm.compress(Bitmap.CompressFormat.PNG, 90, fout);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		try {
			fout.flush();
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onDestroy() {
		ArrayList<DeviceSetInfo> deviceList = mDatabaseManager
				.selectDeviceInfo(mDeviceAddress);
		if (deviceList.size() > 0) {
			mDatabaseManager.updateDeviceInfo(mDeviceAddress, mDeviceSetInfo);
		}
		
		unregisterReceiver(mGattUpdateReceiver);
		super.onDestroy();
	}

	@Override
	public void updateView() {
		initData();
		initView();
	}
}
