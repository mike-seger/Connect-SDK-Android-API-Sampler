//
//  Connect SDK Sample App by LG Electronics
//
//  To the extent possible under law, the person who associated CC0 with
//  this sample app has waived all copyright and related or neighboring rights
//  to the sample app.
//
//  You should have received a copy of the CC0 legalcode along with this
//  work. If not, see http://creativecommons.org/publicdomain/zero/1.0/.
//

package com.connectsdk.sampler.fragments;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.sampler.R;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaControl.DurationListener;
import com.connectsdk.service.capability.MediaControl.PlayStateListener;
import com.connectsdk.service.capability.MediaControl.PlayStateStatus;
import com.connectsdk.service.capability.MediaControl.PositionListener;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.MediaPlayer.MediaLaunchObject;
import com.connectsdk.service.capability.VolumeControl;
import com.connectsdk.service.capability.VolumeControl.VolumeListener;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.sessions.LaunchSession;

public class MediaPlaylistFragment extends BaseFragment {
	private Button playButton;
    private Button pauseButton;
    private Button stopButton;
    private Button rewindButton;
    private Button fastForwardButton;
    private Button closeButton;
    
    private LaunchSession launchSession;
    
    private TextView positionTextView;
    private TextView durationTextView;
    private SeekBar mSeekBar;
    private SeekBar mVolumeBar;
    private ListView listView;

    private static final int REFRESH_INTERVAL_MS = (int) TimeUnit.SECONDS.toMillis(1);
    private long totalTimeDuration;
    
    private MediaControl mMediaControl = null;
    
    private Timer refreshTimer;
    
    public MediaPlaylistFragment(Context context)
    {
        super(context);
    }
	
	private List<String> readUrl(final String urlString) {
		final List<String> lines=new ArrayList<>();
		Thread t=new Thread() {
			public void run() {
				try {
				    URL url = new URL(urlString);
				    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
				    String line;
				    while ((line = in.readLine()) != null) {
				    	lines.add(line);
				    }
				    in.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return lines;
	}
	
	private String emptyTokenAsNull(String token) {
		if(token==null) {
			return null;
		}
		token=token.trim();
		if(token.length()==0) {
			return null;
		}
		return token;
	}
	
	private MediaInfo parseMediaInfo(String mediaInfoString) {
		String mediaURL=null;
		String iconURL=null;
		String title=null;
		String description="";
		String mimeType="video/mp4";
		
		String [] tokens=mediaInfoString.split("\t");
		mediaURL=tokens[0];
		if(tokens.length>1) {
			iconURL=emptyTokenAsNull(tokens[1]);
		}
		
		if(tokens.length>2) {
			title=emptyTokenAsNull(tokens[2]);
		}
		
		if(tokens.length>3) {
			description=emptyTokenAsNull(tokens[3]);
		}
		
		if(tokens.length>4) {
			mimeType=emptyTokenAsNull(tokens[4]);
		}
		
		if(mediaURL==null) {
			return null;
		}
		if(title==null) {
			title=mediaURL;
		}
		return new MediaInfo(mediaURL, iconURL, title, description, mimeType);
	}
	
	private List<MediaInfo> parseMediaInfo(List<String> mediaInfoStrings) {
		List<MediaInfo> mediaInfoList=new ArrayList<>();
		for(String mediaInfoString : mediaInfoStrings) {
			MediaInfo mediaInfo=parseMediaInfo(mediaInfoString);
			if(mediaInfo!=null) {
				mediaInfoList.add(mediaInfo);
			}
		}
		return mediaInfoList;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(
				R.layout.fragment_media_playlist, container, false);

        playButton = (Button) rootView.findViewById(R.id.playButton);
        pauseButton = (Button) rootView.findViewById(R.id.pauseButton);
        stopButton = (Button) rootView.findViewById(R.id.stopButton);
        rewindButton = (Button) rootView.findViewById(R.id.rewindButton);
        fastForwardButton = (Button) rootView.findViewById(R.id.fastForwardButton);
        closeButton = (Button) rootView.findViewById(R.id.closeButton);
        
        positionTextView = (TextView) rootView.findViewById(R.id.stream_position);
        durationTextView = (TextView) rootView.findViewById(R.id.stream_duration);
        mSeekBar = (SeekBar) rootView.findViewById(R.id.stream_seek_bar);
        mVolumeBar = (SeekBar) rootView.findViewById(R.id.volume_seek_bar);
        
        buttons = new Button[] {
         	playButton, 
        	pauseButton, 
        	stopButton, 
        	rewindButton, 
        	fastForwardButton, 
        	closeButton
        };

        listView = (ListView) rootView.findViewById(R.id.listview);
        
        String baseUrl="http://188.40.60.38/mediatest/";
        List<MediaInfo> list=new ArrayList<>();
        list.add(new MediaInfo(baseUrl+"airplay/bipbop/bipbopall.m3u8",  null, "BipBop Test Video (HLS)", "bip bop", "video/mp4"));
        list.add(new MediaInfo(baseUrl+"sintel/sintel-1024-stereo.mp4", null, "Sintel", "Full Mocie", "video/mp4"));
        list.add(new MediaInfo(baseUrl+"airplay/bass_demanding_tracks_for_your_system.mp4", null, "bass demanding tracks for your system", "", "video/mp4"));
        list.add(new MediaInfo(baseUrl+"airplay/stereo_amp_tv_audio_video_sync_test_av_system_test.mp4", null, "stereo amp tv audio video sync test av system test", "", "video/mp4"));
        list.add(new MediaInfo(baseUrl+"airplay/sheep_in_the_island_hd.mp4", null, "sheep in the island hd", "", "video/mp4"));
        list.add(new MediaInfo(baseUrl+"airplay/test_tv_full_hd_1920_x_1080p_2d_3d_sbs.mp4", null, "test tv full hd 1920 x 1080p 2d 3d sbs", "", "video/mp4"));
        list.add(new MediaInfo(baseUrl+"airplay/big_buck_bunny_animation_1080p.mp4", null, "big buck bunny animation 1080p", "", "video/mp4"));
        list.add(new MediaInfo(baseUrl+"airplay/five_minute_sync_test.mp4", null, "five minute sync test", "", "video/mp4"));
        list.add(new MediaInfo(baseUrl+"airplay/planet_earth_seen_from_space_full_hd_1080p.mp4", null, "planet earth seen from space full hd 1080p", "", "video/mp4"));
        list.add(new MediaInfo(baseUrl+"airplay/audio_visual_system_test.mp4", null, "audio visual system test", "", "video/mp4"));
        list.add(new MediaInfo(baseUrl+"airplay/hearing_test_hd.mp4", null, "hearing test hd", "", "video/mp4"));
        list.add(new MediaInfo("http://www-itec.uni-klu.ac.at/dash/js/content/bunny_ibmff_720.mpd", null, "big buck bunny @ 720p MPD", "", "video/mp4"));
        list.add(new MediaInfo(baseUrl+"airplay/nostrum_-_brilliant.mp4", null, "nostrum - brilliant (video)", "", "video/mp4"));
        list.add(new MediaInfo(baseUrl+"airplay/nostrum_-_excess.mp4", null, "nostrum - excess (video)", "", "video/mp4"));
        list.add(new MediaInfo(baseUrl+"airplay/nostrum_-_monastery.mp4", null, "nostrum - monastery (video)", "", "video/mp4"));
        list.add(new MediaInfo(baseUrl+"airplay/nostrum_-_polaris.mp4", null, "nostrum - polaris (video)", "", "video/mp4"));
        list.add(new MediaInfo(baseUrl+"airplay/nostrum_-_brilliant.m4a", null, "nostrum - brilliant (audio)", "", "audio/m4a"));
        list.add(new MediaInfo(baseUrl+"airplay/nostrum_-_excess.m4a", null, "nostrum - excess (audio)", "", "audio/m4a"));
        list.add(new MediaInfo(baseUrl+"airplay/nostrum_-_monastery.m4a", null, "nostrum - monastery (audio)", "", "audio/m4a"));
        list.add(new MediaInfo(baseUrl+"airplay/nostrum_-_polaris.m4a", null, "nostrum - polaris (audio)", "", "audio/m4a"));
        list.add(new MediaInfo(baseUrl+"airplay/segmenter/mixes/goa_saga_chapter_120_125/stream_multi.m3u8", null, "goa saga - chapter 120 - 125 bpm (audio)", "", "audio/m4a"));
      
        list.addAll(parseMediaInfo(readUrl(baseUrl+"moremedia.txt")));
        
        final StableArrayAdapter adapter = new StableArrayAdapter(container.getContext(), android.R.layout.simple_list_item_1, list);
        listView.setAdapter(adapter);

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, final View view,
					int position, long id) {
				MediaInfo item = (MediaInfo) parent.getItemAtPosition(position);
				if (launchSession != null) {
					launchSession.close(null);
					launchSession = null;
					stopUpdating();
					disableMedia();
				}

				MediaPlayer mediaPlayer=getMediaPlayer();
				if(mediaPlayer==null && getTv()!=null) {
					mediaPlayer=getTv().getMediaPlayer();
				}
				if(mediaPlayer!=null) {
					mediaPlayer.playMedia(item.mediaURL, item.mimeType,
							item.title, item.description, item.iconURL, false,
							new MediaPlayer.LaunchListener() {
								public void onSuccess(MediaLaunchObject object) {
									launchSession = object.launchSession;
									mMediaControl = object.mediaControl;
									stopUpdating();
									enableMedia();
								}
	
								@Override
								public void onError(ServiceCommandError error) {
								}
							});
				}
			}
		});
		
		disableList();
        
        return rootView;
	}
    
    @Override
	public void setTv(ConnectableDevice tv) {
		super.setTv(tv);
		
		if (tv == null) {
			disableList();
			stopUpdating();
			mMediaControl = null;
		} else {
			enableList();
		}
	}
    
    @Override
    public void onPause() {
    	stopUpdating();
    	super.onPause();
    }

	@Override
    public void enableButtons()
    {    	 
    	totalTimeDuration = -1;
    	    	
    	mVolumeBar.setEnabled(getTv().hasCapability(VolumeControl.Volume_Set));
    	mVolumeBar.setOnSeekBarChangeListener(volumeListener);
    	
    	if (getTv().hasCapability(VolumeControl.Volume_Get)) {
    		getVolumeControl().getVolume(getVolumeListener);
    	}
    	
    	if (getTv().hasCapability(VolumeControl.Volume_Subscribe)) {
    		getVolumeControl().subscribeVolume(getVolumeListener);
    	}

    	disableMedia();
    }

	@Override
	public void disableButtons() {
		mSeekBar.setEnabled(false);
		mVolumeBar.setEnabled(false);
		mVolumeBar.setOnSeekBarChangeListener(null);
		positionTextView.setEnabled(false);
		durationTextView.setEnabled(false);
		
		super.disableButtons();
	}
	
	protected void onSeekBarMoved(long position) {
		if (mMediaControl != null && getTv().hasCapability(MediaControl.Seek)) {
    		mMediaControl.seek(position, new ResponseListener<Object>() {
				
				@Override
				public void onSuccess(Object response) {
					Log.d("LG", "Success on Seeking");
					startUpdating();
				}
				
				@Override
				public void onError(ServiceCommandError error) {
					Log.w("Connect SDK", "Unable to seek: " + error.getCode());
					startUpdating();
				}
			});
		}
	}
	
	public void enableMedia() {
    	playButton.setEnabled(getTv().hasCapability(MediaControl.Play));
    	pauseButton.setEnabled(getTv().hasCapability(MediaControl.Pause));
    	stopButton.setEnabled(getTv().hasCapability(MediaControl.Stop));
    	rewindButton.setEnabled(getTv().hasCapability(MediaControl.Rewind));
    	fastForwardButton.setEnabled(getTv().hasCapability(MediaControl.FastForward));
       	mSeekBar.setEnabled(getTv().hasCapability(MediaControl.Seek));
       	closeButton.setEnabled(getTv().hasCapability(MediaPlayer.Close));

        fastForwardButton.setOnClickListener(fastForwardListener);
    	mSeekBar.setOnSeekBarChangeListener(seekListener);
        rewindButton.setOnClickListener(rewindListener);
        stopButton.setOnClickListener(stopListener);
        playButton.setOnClickListener(playListener);
        pauseButton.setOnClickListener(pauseListener);
        closeButton.setOnClickListener(closeListener);
        
        if (getTv().hasCapability(MediaControl.PlayState_Subscribe)) {
        	mMediaControl.subscribePlayState(playStateListener);
        } else {
        	mMediaControl.getDuration(durationListener);	
        	startUpdating();
        }
       	
	}
	
	public void disableMedia() {
		playButton.setEnabled(false);
    	playButton.setOnClickListener(null);
    	pauseButton.setEnabled(false);
    	pauseButton.setOnClickListener(null);
    	stopButton.setEnabled(false);
    	stopButton.setOnClickListener(null);
    	rewindButton.setEnabled(false);
    	rewindButton.setOnClickListener(null);
    	fastForwardButton.setEnabled(false);
    	fastForwardButton.setOnClickListener(null);
       	mSeekBar.setEnabled(false);
       	mSeekBar.setOnSeekBarChangeListener(null);
       	mSeekBar.setProgress(0);
       	closeButton.setEnabled(false);
       	closeButton.setOnClickListener(null);
      	
       	positionTextView.setText("--:--:--");
       	durationTextView.setText("--:--:--");
       	
       	totalTimeDuration = -1;       	
	}
	
	private void enableList() {
		listView.setEnabled(true);
	}
	
	private void disableList() {
		listView.setEnabled(false);
       	listView.clearChoices();
       	listView.requestLayout();
	}
	
	public View.OnClickListener playListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
        	if (mMediaControl != null)
        		mMediaControl.play(null);
        }
    };
    
    public View.OnClickListener pauseListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
        	if (mMediaControl != null)
        		mMediaControl.pause(null);
        }
    };
    
    public View.OnClickListener closeListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View view) {
			if (getMediaPlayer() != null) {
				if (launchSession != null)
					launchSession.close(null);
				
				launchSession = null;

				disableMedia();
				stopUpdating();
			}
		}
	};
    
    public View.OnClickListener stopListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
        	if (mMediaControl != null)
        		mMediaControl.stop(new ResponseListener<Object>() {
				
				@Override
				public void onSuccess(Object response) {
					stopUpdating();
					
					playButton.setEnabled(false);
			    	playButton.setOnClickListener(null);
			    	pauseButton.setEnabled(false);
			    	pauseButton.setOnClickListener(null);
			    	stopButton.setEnabled(false);
			    	stopButton.setOnClickListener(null);
			    	rewindButton.setEnabled(false);
			    	rewindButton.setOnClickListener(null);
			    	fastForwardButton.setEnabled(false);
			    	fastForwardButton.setOnClickListener(null);
			       	mSeekBar.setEnabled(false);
			       	mSeekBar.setOnSeekBarChangeListener(null);
			       	mSeekBar.setProgress(0);
			       	
			       	positionTextView.setText("--:--:--");
			       	durationTextView.setText("--:--:--");
			       	
			       	totalTimeDuration = -1;
				}
				
				@Override
				public void onError(ServiceCommandError error) {
				}
			});
        }
    };
    
    public View.OnClickListener rewindListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
        	if (mMediaControl != null)
        		mMediaControl.rewind(null);
        }
    };
    
    public View.OnClickListener fastForwardListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
        	if (mMediaControl != null)
        		mMediaControl.fastForward(null);
        }
    };
    
    public OnSeekBarChangeListener seekListener = new SeekBar.OnSeekBarChangeListener() {
		
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
            mSeekBar.setSecondaryProgress(0);
            onSeekBarMoved(seekBar.getProgress());					
		}
		
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
            mSeekBar.setSecondaryProgress(seekBar.getProgress());					
            stopUpdating();
		}
		
		@Override
		public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
			
		}
	};
	
	public OnSeekBarChangeListener volumeListener = new OnSeekBarChangeListener() {
		
		@Override public void onStopTrackingTouch(SeekBar arg0) { }
		@Override public void onStartTrackingTouch(SeekBar arg0) { }

		@Override
		public void onProgressChanged(SeekBar seekBar, int position, boolean fromUser) {
			if (fromUser)
				getVolumeControl().setVolume((float) mVolumeBar.getProgress() / 100.0f, null);
		}
	};
	
	public VolumeListener getVolumeListener = new VolumeListener() {
		
		@Override
		public void onError(ServiceCommandError error) {
			Log.d("LG", "Error getting Volume: " + error);
		}
		
		@Override
		public void onSuccess(Float object) {
			mVolumeBar.setProgress((int) (object * 100.0f));
		}
	};
	
	public PlayStateListener playStateListener = new PlayStateListener() {
		
		@Override
		public void onError(ServiceCommandError error) {
			Log.d("LG", "Playstate Listener error = " + error);
		}
		
		@Override
		public void onSuccess(PlayStateStatus playState) {
			Log.d("LG", "Playstate changed | playState = " + playState);
			
			switch (playState) {
			case Playing:
					startUpdating();

				if (mMediaControl != null && getTv().hasCapability(MediaControl.Duration)) {
					mMediaControl.getDuration(durationListener);
				}
				break;
			case Finished:
				positionTextView.setText("--:--");
				durationTextView.setText("--:--");
				mSeekBar.setProgress(0);
				
			case Paused:
				stopUpdating();
				break;
			default:
				break;
			}
		}
	};
	
	private void startUpdating() {
		if (refreshTimer != null) {
			refreshTimer.cancel();
			refreshTimer = null;
		}
		refreshTimer = new Timer();
		refreshTimer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				Log.d("LG", "Updating information");
				if (mMediaControl != null && getTv() != null && getTv().hasCapability(MediaControl.Position)) {
					mMediaControl.getPosition(positionListener);
				}
				
				if (mMediaControl != null
						&& getTv() != null
						&& getTv().hasCapability(MediaControl.Duration)
						&& !getTv().hasCapability(MediaControl.PlayState_Subscribe)
						&& totalTimeDuration <= 0) {
					mMediaControl.getDuration(durationListener);
				}
			}
		}, 0, REFRESH_INTERVAL_MS);
	}
	
	private void stopUpdating() {
		if (refreshTimer == null)
			return;
		
		refreshTimer.cancel();
		refreshTimer = null;
	}
	
	private PositionListener positionListener = new PositionListener() {
		
		@Override public void onError(ServiceCommandError error) { }
		
		@Override
		public void onSuccess(Long position) {
			positionTextView.setText(formatTime(position.intValue()));
			mSeekBar.setProgress(position.intValue());
		}
	};
	
	private DurationListener durationListener = new DurationListener() {
		
		@Override public void onError(ServiceCommandError error) { }
		
		@Override
		public void onSuccess(Long duration) {
			totalTimeDuration = duration;
			mSeekBar.setMax(duration.intValue());
			durationTextView.setText(formatTime(duration.intValue()));
		}
	};
	
	private String formatTime(long millisec) {
		int seconds = (int) (millisec / 1000);
		int hours = seconds / (60 * 60);
		seconds %= (60 * 60);
		int minutes = seconds / 60;
		seconds %= 60;

		String time;
		if (hours > 0) {
			time = String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
		}
		else {
			time = String.format(Locale.US, "%d:%02d", minutes, seconds);
		}

		return time;
	}
	
	
    private class MediaInfo {
        private final String mediaURL;
        private final String iconURL;
        private final String title ;
        private final String description;
        private final String mimeType;

        private MediaInfo(String mediaURL, String iconURL, String title, String description, String mimeType) {
            this.mimeType = mimeType;
            this.mediaURL = mediaURL;
            this.iconURL = iconURL;
            this.title = title;
            this.description = description;
        }

        @Override
        public String toString() {
            return title;
        }
    }
    
    private class StableArrayAdapter extends ArrayAdapter<MediaInfo> {
        HashMap<MediaInfo, Integer> mIdMap = new HashMap<MediaInfo, Integer>();
        public StableArrayAdapter(Context context, int textViewResourceId,
            List<MediaInfo> objects) {
          super(context, textViewResourceId, objects);
          for (int i = 0; i < objects.size(); ++i) {
            mIdMap.put(objects.get(i), i);
          }
        }

        @Override
        public long getItemId(int position) {
        	MediaInfo item = getItem(position);
          return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
          return true;
        }
      }
}
