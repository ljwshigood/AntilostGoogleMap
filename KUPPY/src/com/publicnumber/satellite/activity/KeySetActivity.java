package com.publicnumber.satellite.activity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.google.android.gms.maps.model.LatLng;
import com.publicnumber.satellite.R;
import com.publicnumber.satellite.activity.BaseActivity.IUpdateConnectStatus;
import com.publicnumber.satellite.adapter.KeySetAdapter;
import com.publicnumber.satellite.adapter.KeySetAdapter.IKeyRemove;
import com.publicnumber.satellite.application.AppContext;
import com.publicnumber.satellite.bean.DeviceSetInfo;
import com.publicnumber.satellite.bean.KeySetBean;
import com.publicnumber.satellite.db.DatabaseManager;
import com.publicnumber.satellite.service.AlarmService;
import com.publicnumber.satellite.service.BgMusicControlService;
import com.publicnumber.satellite.view.FollowInfoDialog.IUpdateUI;

public class KeySetActivity extends BaseActivity  implements IUpdateUI,IKeyRemove,IUpdateConnectStatus{
	
	private TextView mTvNavigation ; 
	
	private Context mContext ;
	
	private SwipeMenuListView mLvKeySet ;
	
	private KeySetAdapter mKeySetAdapter ;
	
	private DatabaseManager mDataBaseManager ;

	private ArrayList<DeviceSetInfo> mDeviceList = new ArrayList<DeviceSetInfo>();
	
	private ImageView mIvBack ;
	
	private ImageView mIvSet ;
	
	private DatabaseManager mDatabaseManager ; 
	
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

	@Override
	protected void onPause() {
		super.onPause();
		//unregisterReceiver(mReceiver);
	}
	
	
	@Override
	protected void onStop() {
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		Log.e("KeySetActivity","#################onDestroy");
		Log.e("KeySetActivity","#################onDestroy");
		Log.e("KeySetActivity","#################onDestroy");
		
	}
	
	public void initDeviceListInfo() {
		mDeviceList = mDatabaseManager.selectDeviceInfo();
		Iterator iter = AppContext.mHashMapConnectGatt.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			Object key = entry.getKey();
			Object val = entry.getValue();
			for(int i = 0 ;i < mDeviceList.size();i++){
				DeviceSetInfo info = mDeviceList.get(i);
				if (key.toString().equals(info.getmDeviceAddress())) {
					info.setConnected(true);
					info.setVisible(false);
				}
			}
		}
		
	}
	
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.e("liujw","#################################BroadcastReceiver diconnect mReceiver");
			mKeySetAdapter.notifyKeySetStatusChange(mListKeySet,false);
		}
	};
	
	@Override
	protected void onResume() {
		super.onResume();
		sortKeySetList();
		
		Log.e("KeySetActivity","#################################onResume");
		
		if(AppContext.mBluetoothLeService != null && AppContext.mBluetoothLeService.isConnect()){
			mKeySetAdapter.notifyKeySetStatusChange(mListKeySet,true);
		}else{
			mKeySetAdapter.notifyKeySetStatusChange(mListKeySet,false);
		}
	};
		
	private ImageView mIvUrl ;
	
	public static String getFormatDate(long currentTime) {
		SimpleDateFormat formatter = new SimpleDateFormat("dd");
		Date date = new Date(currentTime);
		return formatter.format(date);
	}
	
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.key_set_activity);
		mContext = KeySetActivity.this;
		mDatabaseManager = DatabaseManager.getInstance(mContext);
		
		Intent intent = new Intent(mContext, BgMusicControlService.class);
		startService(intent);
		Intent intentAlarm = new Intent(mContext, AlarmService.class);
		startService(intentAlarm);
		  
		
	//	mDatabaseManager.testupdateDeviceLatLogIsConnect("-33.87365", "151.20689");
		
		SwipeMenuCreator creator = new SwipeMenuCreator() {

	            @Override
	            public void create(SwipeMenu menu) {
	                SwipeMenuItem deleteItem = new SwipeMenuItem(getApplicationContext());
	                deleteItem.setWidth(200);
	                deleteItem.setBackground(mContext.getResources().getDrawable(R.drawable.ic_delete_press));
	                menu.addMenuItem(deleteItem);
	            }
	        };
	        
		
		mDataBaseManager = DatabaseManager.getInstance(mContext);
		sortKeySetList();
		mKeySetAdapter = new KeySetAdapter(mContext,mListKeySet);
		mKeySetAdapter.setmIKeyRemove(this);
		initView();
		mLvKeySet.setMenuCreator(creator);
		mLvKeySet.setAdapter(mKeySetAdapter);
		mLvKeySet.setOnItemClickListener(mKeySetAdapter);
		
		mLvKeySet.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
	            @Override
	            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
	            	if(position == 0){
	            		Toast.makeText(mContext, mContext.getString(R.string.un_delete), 1).show();
	            	}else{
	            		DatabaseManager.getInstance(mContext).updateKeySet(position+1);
	            		mDataBaseManager = DatabaseManager.getInstance(mContext);
	            		sortKeySetList();
	            		mKeySetAdapter.notifyKeyDataSetChange(mListKeySet);
	            	}
	            	
	                return false;
	            }
	        });
		setmIUpdateConnectStatus(this);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == 1212){
			sortKeySetList();
			mKeySetAdapter.notifyKeyDataSetChange(mListKeySet);
		}
	}
	
	private List<KeySetBean> mListKeySet ;
	
	private void sortKeySetList(){
		mListKeySet = mDataBaseManager.selectKeySet();
		Collections.sort(mListKeySet, new ComparatorValues());
	}
	
	public static final class ComparatorValues implements Comparator<KeySetBean>{

        @Override
        public int compare(KeySetBean object1, KeySetBean object2) {
            int m1= object1.getCount();
            int m2= object2.getCount();
            int result=0;
            if(m1>m2) {
                result=1;
            }
            if(m1<m2){
                result=-1;
            }
            return result;
        }        
        
    }
	
	private void initView(){
		mIvUrl = (ImageView)findViewById(R.id.iv_url);
		mIvSet = (ImageView)findViewById(R.id.iv_set);
		mIvBack = (ImageView)findViewById(R.id.iv_back);
		mLvKeySet = (SwipeMenuListView)findViewById(R.id.lv_key_set);
		mTvNavigation = (TextView)findViewById(R.id.tv_navigate);
		mTvNavigation.setText(mContext.getString(R.string.shou_shi));
		mIvSet.setVisibility(View.INVISIBLE);
		mIvBack.setVisibility(View.GONE);
		mIvUrl.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Uri uri = Uri.parse("http://www.smart-key.co.kr/"); 
				Intent it = new Intent(Intent.ACTION_VIEW, uri); 
				startActivity(it);
			}
		});
	}
	
	
	@Override
	public void updateUI() {
		
	}

	@Override
	public void remove(int count) {
		
		DatabaseManager.getInstance(mContext).updateKeySet(count);
		mDataBaseManager = DatabaseManager.getInstance(mContext);
		sortKeySetList();
		mKeySetAdapter.notifyDataSetKey(mListKeySet);
	} 

	@Override
	public void updateConnectStatus(int status) {
		if(status == 0){
			if(AppContext.mBluetoothLeService != null && AppContext.mBluetoothLeService.isConnect()){
				mKeySetAdapter.notifyKeySetStatusChange(mListKeySet,true);
			}else{
				mKeySetAdapter.notifyKeySetStatusChange(mListKeySet,false);
			}
		}
	} 
	
	@Override
	protected void disconnectStatus() {
		super.disconnectStatus();
		Log.e("liujw","#################################BroadcastReceiver diconnect mReceiver");
		mKeySetAdapter.notifyKeySetStatusChange(mListKeySet,false);
	}
	
}
