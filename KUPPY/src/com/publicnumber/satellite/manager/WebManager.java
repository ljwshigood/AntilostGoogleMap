package com.publicnumber.satellite.manager;


import java.io.File;

import org.apache.http.Header;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.publicnumber.satellite.R;
import com.publicnumber.satellite.application.AppContext;
import com.publicnumber.satellite.bean.IPBean;
import com.publicnumber.satellite.bean.ResultBean;

public class WebManager {
		
	private Context mContext ;
	
	private static WebManager mInstance ;
	
	private String mUrls = "http://hanzhihong.cn/bbInfo/bbup.php";
	
	private ICommit2Web mICommit2Web ;
	
	public ICommit2Web getmICommit2Web() {
		return mICommit2Web;
	}

	public void setmICommit2Web(ICommit2Web mICommit2Web) {
		this.mICommit2Web = mICommit2Web;
	}

	private WebManager(Context context){
		this.mContext = context ;
	}
		
	public static WebManager getInstance(Context context){
		if(mInstance == null){
			mInstance = new WebManager(context);
		}
		return mInstance ;
	}
	
	private IPBean mIPBean ;
	
	public void getUploadFileIp(){
		String url = "http://hanzhihong.cn/myip/getip.i.php?title=hzh_home";
		 AppContext.asyncHttpClient.post(url, new AsyncHttpResponseHandler() {

			 @Override
			public void onStart() {
				super.onStart();
			}
			 
			@Override
			public void onSuccess(int statusCode, Header[] headers,byte[] responseBody) {
				String json = new String(responseBody);
				Gson gson = new Gson();
				mIPBean = gson.fromJson(json, IPBean.class);
				mICommit2Web.getIPSuccess();
			}

			@Override
			public void onFailure(int statusCode, Header[] headers,
					byte[] responseBody, Throwable error) {
				error.printStackTrace();
				Toast.makeText(mContext, mContext.getString(R.string.net_work_error), 1).show();
				mICommit2Web.updateSuccess("");
			}
			
		});
		
	}
	
	 public void uploadFile2Web(String picFile,String audioFile,double longitude ,double latitude) throws Exception {
		 
		 RequestParams params = new RequestParams();
		 params.put("picUrl", new File(picFile));
		 params.put("AudioUrl", new File(audioFile));
		 params.put("Longitude", longitude);
		 params.put("latitude", latitude);
		 
		 //http://14.217.245.253:81/bbinfo/bbup.php
		 
		 String url = "http://"+mIPBean.getServ().getIp()+":81/bbinfo/bbup.php";
		 
		 final String showUrl = "http://"+mIPBean.getServ().getIp()+":81/bbinfo/" ;
		 
		 AppContext.asyncHttpClient.post(url, params,new AsyncHttpResponseHandler() {

			 @Override
			public void onStart() {
				super.onStart();
				mICommit2Web.updateSuccess("");
			}
			 
			@Override
			public void onSuccess(int statusCode, Header[] headers,byte[] responseBody) {
				String json = new String(responseBody);
				Log.e("liujw","#######################json : "+json);
				
				Gson gson = new Gson();
				ResultBean bean = gson.fromJson(json, ResultBean.class);
				
				
				if(bean.getRet() == 0){
					mICommit2Web.updateSuccess(showUrl+"/"+bean.getUrl());
				}
			}

			@Override
			public void onFailure(int statusCode, Header[] headers,
					byte[] responseBody, Throwable error) {
				error.printStackTrace();
				mICommit2Web.updateSuccess("");
				//Toast.makeText(mContext, mContext.getString(R.string.net_work_error), 1).show();
			}
			
		});
	}
	 
	 public interface ICommit2Web{
		 
		 public void updateSuccess(String url);
		 
		 public void getIPSuccess();
		
	 } 

}
