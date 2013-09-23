package thu.jiakai.wordbar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

public class WordStorage {
	public static final int DEFAULT_REVIEW_INTERVAL = MemoryModel.REVIEW_PREDEF_INTERVALS[0];
	static private int nrWordCache = -1;

	private static class DBOpenHelper extends SQLiteOpenHelper {
		private static final int DATABASE_VERSION = 1;
		private static final String DATABASE_NAME = "words";
		public boolean isNewlyCreated = false;

		public DBOpenHelper(Context ctx) {
			super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE `words` "
					+ "(`id` INTEGER PRIMARY KEY, `spell` TEXT, `def` TEXT,"
					+ "`prevReviewTime` INTEGER, `nextReviewTime` INTEGER,"
					+ "`curReviewInterval` INTEGER)");
			db.execSQL("CREATE INDEX `nextReviewTimeIdx` ON `words` (`nextReviewTime`)");
			isNewlyCreated = true;
		}

		@Override
		public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		}
	}

	static DBOpenHelper dbHelper;
	static SQLiteDatabase dbRead = null, dbWrite = null;

	public static void init(final Context ctx, final ScrollView logTVScroll,
			final TextView logTV) {
		dbHelper = new DBOpenHelper(ctx);
		start();

		new AsyncTask<Void, String, Void>() {

			protected void onProgressUpdate(String... msg) {
				logTV.append(msg[0] + "\n");
				logTVScroll.fullScroll(View.FOCUS_DOWN);
			}

			@Override
			protected Void doInBackground(Void... params) {
				if (dbHelper.isNewlyCreated) {
					publishProgress("init database");
					InputStream ins = ctx.getResources().openRawResource(
							R.raw.book);
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(ins));
					String line;
					dbWrite.beginTransaction();
					try {
						int num = 0;
						long prevReportTime = 0;
						while ((line = reader.readLine()) != null) {
							num++;
							JSONObject json = new JSONObject(line);
							ContentValues value = new ContentValues();
							value.put("spell", json.getString("spell"));
							value.put("def", json.getString("def"));
							dbWrite.insertOrThrow("words", null, value);
							long curTime = System.currentTimeMillis();
							if (curTime - prevReportTime > 1000) {
								prevReportTime = curTime;
								publishProgress(String.format(
										"building database: %d", num));
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						try {
							ins.close();
							reader.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						dbWrite.setTransactionSuccessful();
						dbWrite.endTransaction();
					}
				}
				publishProgress(String.format(
						"initialization finished\ntotal words: %d\n",
						getNrWord()));
				return null;
			}
		}.execute();
	}

	public static void start() {
		if (dbRead == null) {
			dbRead = dbHelper.getReadableDatabase();
			dbWrite = dbHelper.getWritableDatabase();
		}
	}

	public static void stop() {
		if (dbRead == null && dbWrite == null)
			return;
		dbRead.close();
		dbWrite.close();
		dbRead = null;
		dbWrite = null;
	}

	public static int getNrWord() {
		if (nrWordCache != -1)
			return nrWordCache;
		Cursor cursor = dbRead.rawQuery("SELECT COUNT(*) FROM `words`", null);
		cursor.moveToFirst();
		nrWordCache = cursor.getInt(0);
		cursor.close();
		return nrWordCache;
	}

	/**
	 * @return first word without review flag set
	 */
	public static Word getFirtNewWord() {
		Cursor cursor = dbRead
				.rawQuery(
						"SELECT * FROM `words` WHERE `curReviewInterval` IS NULL LIMIT 1",
						null);
		try {
			if (!cursor.moveToNext()) {
				return Word.makeNone("no more new words");
			}
			return getWordFromCursor(cursor);
		} finally {
			cursor.close();
		}
	}

	public static void addToReviewSet(Word w) {
		long time = System.currentTimeMillis();
		if (!w.inReviewList) {
			w.prevReviewTime = time;
			w.nextReviewTime = time;
			w.curReviewInterval = DEFAULT_REVIEW_INTERVAL;
			w.inReviewList = true;
			updateWordTime(w);
		}
	}

	public static Word getByID(int id) {
		Cursor cursor = dbRead.rawQuery(
				String.format("SELECT * FROM `words` WHERE `id`=%d", id), null);
		cursor.moveToFirst();
		Word w = getWordFromCursor(cursor);
		cursor.close();
		return w;
	}

	static Word getWordFromCursor(Cursor cursor) {
		if (cursor.isBeforeFirst() || cursor.isAfterLast()) {
			return Word.makeNone("no such word");
		}
		StringBuilder def = new StringBuilder();
		def.append(cursor.getString(2));
		Word w = new Word(cursor.getInt(0), cursor.getString(1), null,
				cursor.getLong(3), cursor.getLong(4), cursor.getInt(5));
		def.append("\n\n");
		if (cursor.isNull(5)) {
			def.append("not in review list");
			w.inReviewList = false;
		} else {
			def.append(String.format("Prev review time: %s\n",
					Utils.formatTime(w.prevReviewTime)));
			def.append(String.format("Next review time: %s\n",
					Utils.formatTime(w.nextReviewTime)));
			def.append(String.format("Review interval: %.2f hours",
					w.curReviewInterval / 3600.0));
			w.inReviewList = true;
		}
		w.definition = def.toString();
		return w;
	}

	static void updateWordTime(Word w) {
		dbWrite.execSQL(String
				.format("UPDATE `words` SET prevReviewTime=%d, nextReviewTime=%d, curReviewInterval=%d "
						+ "WHERE `id`=%d", w.prevReviewTime, w.nextReviewTime,
						w.curReviewInterval, w.id));
	}
}
