package thu.jiakai.wordbar;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class ReviewActivity extends Activity {
	Word curWord;
	ArrayList<Word> wordList;
	int wordIndex = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_review);
		wordList = MemoryModel.getWordsToReview();
		Utils.randomShuffle(wordList);
		wordIndex = -1;
		moveToNextWord();
	}

	private void onUserResponse(int rst) {
		if (!curWord.isNone())
			MemoryModel.setUserResponse(curWord, rst);
		moveToNextWord();
	}

	private void moveToNextWord() {
		wordIndex += 1;
		if (wordIndex >= wordList.size()) {
			wordIndex = 0;
			Utils.randomShuffle(wordList);
		}
		if (wordList.isEmpty()) {
			curWord = Word.makeNone("no word to review");
		} else
			curWord = wordList.get(wordIndex);
		((TextView) findViewById(R.id.statusTextView)).setText(String.format(
				"%d/%d", wordIndex, wordList.size()));
		((TextView) findViewById(R.id.wordTitleTextView))
				.setText(curWord.spell);
		((TextView) findViewById(R.id.wordDefTextView))
				.setText("<-- definition -->");
	}

	public void onShowDefClicked(View view) {
		((TextView) findViewById(R.id.wordDefTextView))
				.setText(curWord.definition);
	}

	public void onYesClicked(View view) {
		if (!curWord.isNone()) {
			wordList.remove(wordIndex);
			wordIndex--;
		}
		onUserResponse(1);
	}

	public void onNoClicked(View view) {
		onUserResponse(0);
	}

}
