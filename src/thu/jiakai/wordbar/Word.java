package thu.jiakai.wordbar;

public class Word {
	public int id,
		curReviewInterval;	// in seconds, review interval after nextReviewTime
	public long prevReviewTime, nextReviewTime;	// java time stamp
	public String spell, definition;
	public boolean inReviewList;
	
	public Word(int id_, String spell_, String trans_, long pr, long nr, int cr) {
		id = id_;
		spell = spell_;
		definition = trans_;
		prevReviewTime = pr;
		nextReviewTime = nr;
		curReviewInterval = cr;
	}

	static Word makeNone(String msg) {
		return new Word(-1, "NONE", msg, 0, 0, 0);
	}

	public boolean isNone() {
		return id < 0;
	}
}
