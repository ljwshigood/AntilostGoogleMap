package com.publicnumber.satellite.activity;


import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.publicnumber.satellite.R;
import com.publicnumber.satellite.adapter.RingAdapter;
import com.publicnumber.satellite.application.AppContext;
import com.publicnumber.satellite.bean.SoundInfo;
import com.publicnumber.satellite.db.DatabaseManager;
import com.publicnumber.satellite.util.PlayMedia;

public class AlarmRingActvivity extends BaseActivity implements OnClickListener {

	private ListView mLvRing;

	private RingAdapter mRingAdapter;

	private Context mContext;

	private ImageView mIvBack;

	private DatabaseManager mDatabaseManager ;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ring_set);
		this.mContext = AlarmRingActvivity.this;
		mDatabaseManager =  DatabaseManager.getInstance(mContext);
		initData();
		mRingAdapter = new RingAdapter(mContext,AppContext.mAlarmList);
		initView();
		setTitle(mContext.getString(R.string.ring));
	}

	private void initData(){
		ArrayList<SoundInfo> soundList = mDatabaseManager.selectSoundInfo(AppContext.mDeviceAddress);
    	if(soundList.size() > 0 ){
    		SoundInfo info  = soundList.get(0);
        	for(int i = 0 ;i < AppContext.mAlarmList.size();i++){
        		if(info.getRingId() ==  AppContext.mAlarmList.get(i).getRes()){
        			AppContext.mAlarmList.get(i).setSelect(true);
        		}else{
        			AppContext.mAlarmList.get(i).setSelect(false);
        		}
        	}
    	}
	}
	
	private void initView() {
		mLvRing = (ListView) findViewById(R.id.lv_ring);
		mLvRing.setAdapter(mRingAdapter);
		mLvRing.setOnItemClickListener(mRingAdapter);
		mIvBack = (ImageView) findViewById(R.id.iv_back);
		mIvBack.setOnClickListener(this);
	}
	
	private View  mView ;
	
	private TextView mTvTitleInfo ;
	
	private void setTitle(String info){
		mView = (View)findViewById(R.id.include_head);
		mTvTitleInfo = (TextView)mView.findViewById(R.id.tv_title_info);
		mTvTitleInfo.setText(info);
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		PlayMedia.getInstance(mContext).release();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.iv_back:
			PlayMedia.getInstance(mContext).release();
			finish();
			break;

		default:
			break;
		}
	}
	
}
