package thu.jiakai.wordbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.TreeMap;

import org.json.JSONArray;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
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
		final int offset, length, duration; // in milliseconds

		public FileChunk(int o, int l, int d) {
			offset = o;
			length = l;
			duration = d;
		}
	}

	ArrayList<Word> wordList;
	int curIndex;
	MediaPlayer mediaPlayer;
	TreeMap<Integer, FileChunk> wordChunkMap;
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
		FileChunk chunk = wordChunkMap.get(Integer.valueOf(w.id));
		mediaPlayer.reset();
		try {
			mediaPlayer.setDataSource(audioFile.getFD(), chunk.offset,
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

	private static class DBOpenHelper extends SQLiteOpenHelper {
		private static final int DATABASE_VERSION = 1;
		private static final String DATABASE_NAME = "cache";
		private final Loader loader;
		private final String mapFilePath;
		boolean initSuccessful = true;

		public DBOpenHelper(Context ctx, Loader loader, String mapFilePath) {
			super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
			this.loader = loader;
			this.mapFilePath = mapFilePath;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			initSuccessful = false;
			db.execSQL("CREATE TABLE `wordmap` "
					+ "(`id` INTEGER PRIMAY KEY, `offset` INTEGER, `length` INTEGER, `duration` INTEGER)");

			db.beginTransaction();
			loader.pubProg("init cache database");
			FileInputStream mapFile = null;
			BufferedReader rd = null;
			try {
				mapFile = new FileInputStream(mapFilePath);
				rd = new BufferedReader(new InputStreamReader(mapFile));
				JSONArray json = new JSONArray(rd.readLine());
				TreeMap<String, Integer> wordMap = new TreeMap<String, Integer>();
				{
					Cursor cursor = WordStorage.dbRead.rawQuery(
							"SELECT `id`, `spell` FROM `words`", null);
					while (cursor.moveToNext()) {
						wordMap.put(cursor.getString(1),
								Integer.valueOf(cursor.getInt(0)));
					}
				}
				long prevReportTime = System.currentTimeMillis();
				for (int num = 0; num < json.length(); num ++) {
					{
						long t = System.currentTimeMillis();
						if (t - prevReportTime > 1000) {
							prevReportTime = t;
							loader.pubProg("loaded " + num);
						}
					}
					JSONArray sub = json.getJSONArray(num);
					Integer id = wordMap.get(sub.getString(0));
					if (id != null) {
						db.execSQL(String
								.format("INSERT INTO `wordmap` VALUES (%d, %d, %d, %d)",
										id.intValue(), sub.getInt(1), sub.getInt(2), sub.getInt(3)));
					}
				}
				db.setTransactionSuccessful();
				initSuccessful = true;
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				db.endTransaction();
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
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		}
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

		void pubProg(String msg) {
			publishProgress(msg);
		}

		private void loadAudio() {
			loadSuccessful = false;
			mediaPlayer = null;
			File dataDir = new File(Environment.getExternalStorageDirectory()
					.getAbsoluteFile() + "/wordbar");
			if (!dataDir.isDirectory())
				return;
			DBOpenHelper dbHelper = null;
			SQLiteDatabase db = null;
			try {
				publishProgress("loading audio ...");
				audioFile = new FileInputStream(dataDir.getAbsolutePath()
						+ "/audio.ogg");
				mediaPlayer = new MediaPlayer();
				mediaPlayer.setDataSource(audioFile.getFD());
				mediaPlayer.prepare();

				publishProgress("loading word offset map ...");
				dbHelper = new DBOpenHelper(AutoPlayActivity.this, this,
						dataDir.getAbsolutePath() + "/map.json");
				if (dbHelper.initSuccessful) {
					db = dbHelper.getReadableDatabase();
					StringBuilder query = new StringBuilder();
					query.append("SELECT * FROM `wordmap` WHERE `id` IN (");
					{
						boolean first = true;
						for (Word w : wordList) {
							if (first)
								first = false;
							else
								query.append(",");

							query.append(w.id);
						}
					}
					query.append(")");
					Cursor cursor = db.rawQuery(query.toString(), null);
					wordChunkMap = new TreeMap<Integer, AutoPlayActivity.FileChunk>();
					while (cursor.moveToNext()) {
						wordChunkMap.put(
								Integer.valueOf(cursor.getInt(0)),
								new FileChunk(cursor.getInt(1), cursor
										.getInt(2), cursor.getInt(3)));
					}
					if (wordChunkMap.size() != wordList.size())
						failMsg = "no data for some words";
					else
						loadSuccessful = true;
				}
			} catch (Exception exc) {
				failMsg = "failed to load audio: caught exception: "
						+ exc.toString();
				exc.printStackTrace();
			} finally {
				if (!loadSuccessful) {
					try {
						audioFile.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					mediaPlayer = null;
					audioFile = null;
				}
				if (db != null)
					db.close();
				if (dbHelper != null)
					dbHelper.close();
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
