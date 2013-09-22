package thu.jiakai.wordbar;

import java.util.ArrayList;

import android.database.Cursor;

public class MemoryModel {

	private static final double REVIEW_INTERVAL_PENALTY = 0.94,
			TOLERANCE = REVIEW_INTERVAL_PENALTY * REVIEW_INTERVAL_PENALTY;

	// interval: 5min, 2hrs, 1day, 2day, 4day, 8day ...
	static final int[] REVIEW_PREDEF_INTERVALS = new int[] { 300, 2 * 3600,
			24 * 3600 };

	static int getNextReviewInterval(int curReviewInterval) {
		for (int i : REVIEW_PREDEF_INTERVALS) {
			if (curReviewInterval < i * TOLERANCE)
				return i;
		}
		return curReviewInterval * 2;
	}

	public static ArrayList<Word> getWordsToReview() {
		WordStorage.start();
		ArrayList<Word> rst = new ArrayList<Word>();
		Cursor cursor = WordStorage.dbRead.rawQuery(String.format(
				"SELECT * FROM `words` WHERE nextReviewTime <= %d",
				System.currentTimeMillis()), null);
		for (;;) {
			if (!cursor.moveToNext()) {
				cursor.close();
				return rst;
			}
			rst.add(WordStorage.getWordFromCursor(cursor));
		}
	}

	/**
	 * @param resp
	 *            whether the user responses that the word has been memorized
	 */
	public static void setUserResponse(Word w, int resp) {
		if (resp == 0) {
			w.curReviewInterval = Math.max(WordStorage.DEFAULT_REVIEW_INTERVAL,
					(int) (w.curReviewInterval * REVIEW_INTERVAL_PENALTY));
		} else {
			long t = System.currentTimeMillis();
			w.prevReviewTime = t;
			w.nextReviewTime = t + w.curReviewInterval * 1000;
			w.curReviewInterval = getNextReviewInterval(w.curReviewInterval);
		}
		WordStorage.updateWordTime(w);
	}

}
