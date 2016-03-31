package com.publicnumber.satellite.service;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;

import com.publicnumber.satellite.bean.DisturbInfo;
import com.publicnumber.satellite.bean.MediaPlayerBean;
import com.publicnumber.satellite.bean.SoundInfo;
import com.publicnumber.satellite.db.DatabaseManager;
import com.publicnumber.satellite.util.FomatTimeUtil;

public class BgMusicControlService extends Service {
	// 帮助声音开发状态文件
	public static final String HELP_SOUND_FILE = "help_sound_setting";
	public static final String CTL_ACTION = "com.android.iwit.IWITARTIS.CTL_ACTION";
	MyReceiver serviceReceiver;
	private DatabaseManager mDatabaseManger;

	AudioManager mAudioManager;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private HashMap<String, MediaPlayerBean> mHashMapMediaPlayer = new HashMap<String, MediaPlayerBean>();

	private MediaPlayerBean iteratorMediaPlayer(
			HashMap<String, MediaPlayerBean> hashMapMediaPlayer, String address) {
		Iterator iter = hashMapMediaPlayer.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			Object key = entry.getKey();
			Object val = entry.getValue();
			if (key.toString().equals(address)) {
				return (MediaPlayerBean) val;
			}
		}
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mDatabaseManger = DatabaseManager
				.getInstance(BgMusicControlService.this);
		serviceReceiver = new MyReceiver();
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		// 创建IntentFilter
		IntentFilter filter = new IntentFilter();
		filter.addAction(CTL_ACTION);
		registerReceiver(serviceReceiver, filter);
	}

	private MediaPlayerBean createMediaPlayer(int id, float volume,
			double duration, final String address) {
		// 创建MediaPlayer
		MediaPlayer mediaPlayer = null;

		mediaPlayer = MediaPlayer.create(getBaseContext(), id);
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		// 准备、并播放音乐
		
		mediaPlayer.setVolume(volume, volume);
		mediaPlayer.start();
		
		long formatDuration = (long) duration * 1000;
		final int playCount = (int) formatDuration / mediaPlayer.getDuration();
		MediaPlayerBean bean = new MediaPlayerBean();
		bean.setMediaPlayer(mediaPlayer);

		mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				MediaPlayerBean bean = iteratorMediaPlayer(mHashMapMediaPlayer,
						address);
				if (bean != null) {
					bean.increase();
					if (playCount <= bean.getCount()) {
						if (mp != null) {
							mp.release();
							mp = null;
							if(vibrator != null){
								vibrator.cancel();
							}
						}
						mHashMapMediaPlayer.remove(address);
					} else {
						mp.seekTo(0);
						mp.start();
					}
				}
			}
		});
		return bean;
	}

	private Vibrator vibrator;
	public static boolean isPause = true;// 当前是否处于暂停状态

	public class MyReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(final Context context, Intent intent) {
			int control = intent.getIntExtra("control", -1);
			String address = intent.getStringExtra("address");
			switch (control) {
			case 1:
				ArrayList<SoundInfo> soundList = mDatabaseManger
						.selectSoundInfo(address);
				ArrayList<DisturbInfo> disturbList = mDatabaseManger
						.selectDisturbInfo(address);
				if(disturbList == null || disturbList.size() == 0){
					return ;
				}
				boolean isDisturb = disturbList.get(0).isDisturb();
				if (isDisturb && disturbList.size() > 0) {
					isDisturb = FomatTimeUtil.isInDisturb(disturbList.get(0)
							.getStartTime(), disturbList.get(0).getEndTime());
				}
				if (soundList.size() > 0) {
					SoundInfo info = soundList.get(0);
					if(isDisturb){
						mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);    
					}else{
						mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, info.getRingVolume(), 0);
					}
					if (iteratorMediaPlayer(mHashMapMediaPlayer, address) == null) {
						if (mHashMapMediaPlayer.get(address) == null) {
							MediaPlayerBean bean = createMediaPlayer(
									info.getRingId(), info.getRingVolume(),
									info.getDurationTime(), address);
							mHashMapMediaPlayer.put(address, bean);
						}
					} else {
						MediaPlayerBean bean = mHashMapMediaPlayer.get(address);
						if (!bean.getMediaPlayer().isPlaying()) {
							bean.getMediaPlayer().start();
						}
					}
					
					if (info.isShock()) {
						vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
						long[] pattern = { 100, 400, 100, 400 }; // 停止 开启 停止 开启
						vibrator.vibrate(pattern, 2);
						
					}
				}

				break;
			case 2:
				releaseMusic(address);
				break;
			case 3:
				releaseMusic();
				break;
			}
		}
	}

	private void releaseMusic(){
		Iterator iter = mHashMapMediaPlayer.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			Object key = entry.getKey();
			Object val = entry.getValue();
			MediaPlayerBean mediaBean = (MediaPlayerBean)val;
			if (mediaBean == null) {
				continue;
			}
			MediaPlayer mediaPlayer = mediaBean.getMediaPlayer();
			if (mediaPlayer != null) {
				mediaPlayer.release();
				mediaPlayer = null;
				if (vibrator != null) {
					vibrator.cancel();
				}
			}
		}
		mHashMapMediaPlayer.clear();
	}
	
	private void playMusic(String address) {
		MediaPlayerBean mediaPlayer = iteratorMediaPlayer(mHashMapMediaPlayer,
				address);
		mediaPlayer.getMediaPlayer().start();
		mediaPlayer.getMediaPlayer().setLooping(true);
	}

	private void pauseMusic(String address) {
		MediaPlayerBean mediaPlayer = iteratorMediaPlayer(mHashMapMediaPlayer,
				address);
		mediaPlayer.getMediaPlayer().pause();
	}

	private void releaseMusic(String address) {

		MediaPlayerBean mediaBean = iteratorMediaPlayer(mHashMapMediaPlayer,address);
		if (mediaBean == null) {
			return;
		}
		MediaPlayer mediaPlayer = mediaBean.getMediaPlayer();
		if (mediaPlayer != null) {
			mediaPlayer.release();
			mediaPlayer = null;
			if (vibrator != null) {
				vibrator.cancel();
			}
		}
		mHashMapMediaPlayer.remove(address);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(serviceReceiver);
	}

}
