package com.publicnumber.satellite.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

import com.publicnumber.satellite.R;

public class MainFollowActivity extends BaseActivity implements OnClickListener {

	private LinearLayout mIvDevice;

	private LinearLayout mIvCamera;

	private LinearLayout mIvLocation;

	private LinearLayout mIvRecord;;

	private Context mContext;

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_follow);
		mContext = MainFollowActivity.this;
		initView();
	}

	
	private void initView() {
		mIvDevice = (LinearLayout) findViewById(R.id.ll_device);
		mIvCamera = (LinearLayout) findViewById(R.id.ll_camera);
		mIvLocation = (LinearLayout) findViewById(R.id.ll_location);
		mIvRecord = (LinearLayout) findViewById(R.id.ll_record);
		mIvDevice.setOnClickListener(this);
		mIvCamera.setOnClickListener(this);
		mIvLocation.setOnClickListener(this);
		mIvRecord.setOnClickListener(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	public void onClick(View v) {
		Intent intent;
		switch (v.getId()) {
		case R.id.ll_device:
			intent = new Intent(mContext, DeviceDisplayActivity.class);
			startActivity(intent);
			break;
		case R.id.ll_camera:
			intent = new Intent(mContext, CameraActivity.class);
			startActivity(intent);
			break;
		case R.id.ll_location:
			intent = new Intent(mContext, DeviceLocationActivity.class);
			startActivity(intent);
			break;
		case R.id.ll_record:
			intent = new Intent(mContext, RecordActivity.class);
			startActivity(intent);
			break;

		default:
			break;
		}
	}
}
