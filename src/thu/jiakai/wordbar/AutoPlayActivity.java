package thu.jiakai.wordbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.TreeMap;

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
	private static final class FileChunk {
		final int start, length, duration; // in milliseconds

		public FileChunk(int s, int l, int d) {
			start = s;
			length = l;
			duration = d;
		}
	}

	ArrayList<Word> wordList;
	int curIndex;
	MediaPlayer mediaPlayer;
	TreeMap<String, FileChunk> wordChunkMap;
	FileInputStream audioFile = null;
	Handler handler;
	int curEndTime;
	final Runnable stopAudioAndMoveToNext = new Runnable() {
		@Override
		public void run() {
			if (mediaPlayer == null)
				return;
			try {
				mediaPlayer.stop();
			} catch (Exception ex) {
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
		if (mediaPlayer != null) {
			mediaPlayer.release();
			mediaPlayer = null;
		}
	}

	private void moveToNext() {
		if (mediaPlayer == null)
			return;
		curIndex++;
		if (curIndex >= wordList.size())
			curIndex = 0;
		Word w = wordList.get(curIndex);
		((TextView) findViewById(R.id.wordTitleTextView)).setText(w.spell);
		((TextView) findViewById(R.id.wordDefTextView)).setText(w.definition);
		FileChunk chunk = wordChunkMap.get(w.spell);
		mediaPlayer.reset();
		try {
			mediaPlayer.setDataSource(audioFile.getFD(), chunk.start,
					chunk.length);
			mediaPlayer.prepare();
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, "error", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		mediaPlayer.start();
		handler.postDelayed(stopAudioAndMoveToNext, chunk.duration);
	}

	private class Loader extends AsyncTask<Void, String, Void> {
		final TextView logTV = (TextView) findViewById(R.id.wordDefTextView);

		final ScrollView logTVScroll = (ScrollView) findViewById(R.id.scrollView1);
		boolean loadSuccessful = false;
		String failMsg = null;

		protected void onProgressUpdate(String... msg) {
			if (msg[0] == null) {
				if (!loadSuccessful) {
					Toast.makeText(AutoPlayActivity.this, failMsg,
							Toast.LENGTH_LONG).show();
					finish();
					return;
				}
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
			loadAudio();
			if (!loadSuccessful && failMsg == null)
				failMsg = "no audio data available";
			publishProgress((String) null);
			return null;
		}

		private void loadAudio() {
			loadSuccessful = false;
			mediaPlayer = null;
			File dataDir = new File(Environment.getExternalStorageDirectory()
					.getAbsoluteFile() + "/wordbar");
			if (!dataDir.isDirectory())
				return;
			FileInputStream mapFile = null;
			BufferedReader rd = null;
			try {
				publishProgress("loading audio ...");
				audioFile = new FileInputStream(dataDir.getAbsolutePath()
						+ "/audio.ogg");
				mediaPlayer = new MediaPlayer();
				mediaPlayer.setDataSource(audioFile.getFD());
				mediaPlayer.prepare();

				publishProgress("loading word offset map ...");
				String[] word = new String[wordList.size()];
				{
					int p = 0;
					for (Word w : wordList)
						word[p ++] = w.spell;
				}
				Arrays.sort(word);
				mapFile = new FileInputStream(dataDir.getAbsolutePath()
						+ "/map.txt");
				rd = new BufferedReader(new InputStreamReader(mapFile));
				wordChunkMap = new TreeMap<String, AutoPlayActivity.FileChunk>();

				for (String w : word) {
					for (;;) {
						String line = rd.readLine();
						if (line == null) {
							failMsg = "no word" + w;
							return;
						}

						Scanner sc = new Scanner(line);
						String cur_word = sc.next();
						if (cur_word.equals(w)) {
							int s, l, d;
							s = sc.nextInt();
							l = sc.nextInt();
							d = sc.nextInt();
							wordChunkMap.put(w, new FileChunk(s, l, d));
							break;
						}
					}
				}

				loadSuccessful = true;
				return;
			} catch (Exception exc) {
				failMsg = "failed to load audio: caught exception: " + exc.toString();
				exc.printStackTrace();
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
				if (!loadSuccessful) {
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
