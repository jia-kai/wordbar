package thu.jiakai.wordbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Menu;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class AutoPlayActivity extends Activity {
	private static final class TimeInterval {
		final int start, end; // in milliseconds

		public TimeInterval(int s, int e) {
			start = s;
			end = e;
		}
	}

	ArrayList<Word> wordList;
	int curIndex;
	MediaPlayer mediaPlayer;
	TreeMap<String, TimeInterval> timeMap;
	FileInputStream audioFile = null;
	Handler handler;
	int curEndTime;
	final Runnable stopAudioAndMoveToNext = new Runnable() {
		@Override
		public void run() {
			if (mediaPlayer.isPlaying()) {
				while (mediaPlayer.getCurrentPosition() < curEndTime)
					continue;
				mediaPlayer.pause();
			}
			moveToNext();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_auto_play);
		wordList = MemoryModel.getWordsToReview();
		if (wordList.isEmpty()) {
			Toast.makeText(this, "no words to review", Toast.LENGTH_LONG)
					.show();
			this.finish();
			return;
		}
		(new Loader()).execute();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		handler = null;
	}

	private void moveToNext() {
		if (handler == null)
			return;
		curIndex++;
		if (curIndex >= wordList.size())
			curIndex = 0;
		Word w = wordList.get(curIndex);
		((TextView) findViewById(R.id.wordTitleTextView)).setText(w.spell);
		((TextView) findViewById(R.id.wordDefTextView)).setText(w.definition);
		TimeInterval time = timeMap.get(w.spell);
		mediaPlayer.seekTo(time.start);
		mediaPlayer.start();
		curEndTime = time.end;
		handler.postDelayed(stopAudioAndMoveToNext, time.end - mediaPlayer.getCurrentPosition());
	}

	private class Loader extends AsyncTask<Void, String, Void> {
		final TextView logTV = (TextView) findViewById(R.id.wordDefTextView);

		final ScrollView logTVScroll = (ScrollView) findViewById(R.id.scrollView1);

		protected void onProgressUpdate(String... msg) {
			if (msg[0] == null) {
				handler = new Handler();
				curIndex = -1;
				moveToNext();
			} else {
				logTV.append(msg[0] + "\n");
				logTVScroll.fullScroll(View.FOCUS_DOWN);
			}
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			if (!loadAudio()) {
				Toast.makeText(AutoPlayActivity.this,
						"no audio data available", Toast.LENGTH_LONG).show();
				finish();
				return null;
			}
			publishProgress((String) null);
			return null;
		}

		private boolean loadAudio() {
			mediaPlayer = null;
			File dataDir = new File(Environment.getExternalStorageDirectory()
					.getAbsoluteFile() + "/wordbar");
			if (!dataDir.isDirectory())
				return false;
			FileInputStream mapFile = null;
			BufferedReader rd = null;
			boolean successful = false;
			try {
				publishProgress("loading audio ...");
				audioFile = new FileInputStream(dataDir.getAbsolutePath()
						+ "/audio.ogg");
				mediaPlayer = new MediaPlayer();
				mediaPlayer.setDataSource(audioFile.getFD());
				mediaPlayer.prepare();

				publishProgress("loading word offset map ...");
				mapFile = new FileInputStream(dataDir.getAbsolutePath()
						+ "/map.json");
				rd = new BufferedReader(new InputStreamReader(mapFile));
				timeMap = new TreeMap<String, AutoPlayActivity.TimeInterval>();
				JSONObject json = new JSONObject(rd.readLine());
				for (Word w : wordList) {
					JSONArray array = json.getJSONArray(w.spell);
					if (array == null) {
						Toast.makeText(AutoPlayActivity.this,
								"no audio for word " + w.spell,
								Toast.LENGTH_LONG).show();
						return false;
					}
					timeMap.put(w.spell,
							new TimeInterval((int) (array.getDouble(0) * 1000),
									(int) array.getDouble(1) * 1000));
				}
				successful = true;
				return true;
			} catch (Exception exc) {
				exc.printStackTrace();
				return false;
			} finally {
				if (rd != null) {
					try {
						rd.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (mapFile != null) {
					try {
						mapFile.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (!successful) {
					try {
						audioFile.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					mediaPlayer = null;
					audioFile = null;
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.auto_play, menu);
		return true;
	}

}
