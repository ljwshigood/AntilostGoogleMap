package com.publicnumber.satellite.adapter;

import java.io.File;
import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.publicnumber.satellite.R;
import com.publicnumber.satellite.bean.RecordInfo;

public class RecordMenuAdapter extends BaseAdapter implements
		OnItemClickListener {

	private Context mContext;

	private LayoutInflater mInflator;

	private ArrayList<RecordInfo> mRecordMemuList;

	private IMediaListener mMeidaListener;

	public ITransferMediaItemList mTransferList;

	public RecordMenuAdapter(Context context,
			ArrayList<RecordInfo> mRecordMemuList,
			IMediaListener mediaListener, ITransferMediaItemList transferList) {
		this.mContext = context;
		this.mRecordMemuList = mRecordMemuList;
		mInflator = LayoutInflater.from(context);
		this.mMeidaListener = mediaListener;
		this.mTransferList = transferList;
	}

	@Override
	public int getCount() {

		return mRecordMemuList == null ? 0 : mRecordMemuList.size();
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final ViewHolder viewHolder;
		final int tempPosition = position;
		if (convertView == null) {
			convertView = mInflator.inflate(R.layout.item_record_menu, null);
			viewHolder = new ViewHolder();
			viewHolder.mIvRecordMenu = (ImageView) convertView
					.findViewById(R.id.iv_record_menu);
			viewHolder.mTvRecordMenuInfo = (TextView) convertView
					.findViewById(R.id.tv_record_menu);
			viewHolder.mCbDelete = (CheckBox) convertView
					.findViewById(R.id.cb_delete);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		if (mRecordMemuList.get(position).isVisible()) {
			viewHolder.mCbDelete.setVisibility(View.VISIBLE);
		} else {
			viewHolder.mCbDelete.setVisibility(View.GONE);
		}
		
		if(mRecordMemuList.get(position).isSelect()){
			viewHolder.mCbDelete.setChecked(true);
		}else{
			viewHolder.mCbDelete.setChecked(false);
		}
		
		File file = new File(mRecordMemuList.get(tempPosition).getFilePath());
		viewHolder.mTvRecordMenuInfo.setText(file.getName());
		return convertView;
	}

	class ViewHolder {
		CheckBox mCbDelete;
		ImageView mIvRecordMenu;
		TextView mTvRecordMenuInfo;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		String path = mRecordMemuList.get(position).getFilePath();
		//if (!mRecordMemuList.get(position)) {
			mMeidaListener.play(position, path);
		//}
		/*ViewHolder vHollder = (ViewHolder) view.getTag();
		vHollder.mCbDelete.toggle();
		String path = mRecordMemuList.get(position).getFilePath();
		if (!mRecordMemuList.get(position).isVisible()) {
			mMeidaListener.play(position, path);
		}
		mRecordMemuList.get(position).setSelect(vHollder.mCbDelete.isChecked());
		mTransferList.transferMediaItemList(mRecordMemuList);*/
	}

	public void notifyData(ArrayList<RecordInfo> list ) {
		this.mRecordMemuList = list ;
		notifyDataSetChanged();
	}

	public interface ITransferMediaItemList {
		public void transferMediaItemList(ArrayList<RecordInfo> mediaList);
	}

	public interface IMediaListener {
		public void play(int position, String path);
	}
}
