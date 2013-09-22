package thu.jiakai.wordbar;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class WordStorage {
	private static class DBOpenHelper extends SQLiteOpenHelper {
		private static final int DATABASE_VERSION = 1;
		private static final String DATABASE_NAME = "words";

		/*
		 * db schema: id: integer data: text, Record.toString uploaded: integer,
		 * whether this record has been uploaded to server
		 */

		public DBOpenHelper(Context ctx) {
			super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE `words` (`id` INTEGER PRIMARY KEY, `spell` TEXT, `def` INTEGER)");
		}

		@Override
		public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		}
	}
	static DBOpenHelper dbHelper;
	static SQLiteDatabase dbRead = null, dbWrite = null;
	
	public static void init(Context ctx) {
		dbHelper = new DBOpenHelper(ctx);
	}
	
	public static void start() {
		stop();
		dbRead = dbHelper.getReadableDatabase();
		dbWrite = dbHelper.getWritableDatabase();
	}
	
	public static void stop() {
		if (dbRead == null && dbWrite == null)
			return;
		dbRead.close();
		dbWrite.close();
		dbRead = null;
		dbWrite = null;
	}
	
	/**
	 * @return first word without review flag set
	 */
	public static Word getFirtNewWord() {
		return null;
	}
	
	public static void setReviewFlag(Word w, int flag) {
		
	}
	
	public static Word getByID(int id) {
		return null;
	}
}
