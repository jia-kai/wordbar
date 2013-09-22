package thu.jiakai.wordbar;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class ExploreActivity extends Activity {
	private Word curWord;
	private EditText wordIDET;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_explore);
		wordIDET = (EditText) findViewById(R.id.wordIDEditText);
		setCurWord(WordStorage.getFirtNewWord());
	}

	private void setCurWord(Word w) {
		curWord = w;
		TextView titleTV = (TextView) findViewById(R.id.wordTitleTextView), transTV = (TextView) findViewById(R.id.wordDefTextView);
		String spell = w.spell;
		if (w.inReviewList)
			spell += " [R]";
		titleTV.setText(spell);
		transTV.setText(w.definition);
		wordIDET.setText(String.valueOf(w.id));
	}

	public void onGoClicked(View view) {
		setCurWord(WordStorage.getByID(Integer.parseInt(wordIDET.getText()
				.toString())));
	}

	public void onPrevClicked(View view) {
		gotoID(curWord.id - 1);
	}

	public void onNextClicked(View view) {
		gotoID(curWord.id + 1);
	}

	public void onAddToReviewClicked(View view) {
		WordStorage.addToReviewSet(curWord);
		gotoID(curWord.id + 1);
	}
	
	private void gotoID(int id) {
		if (id < 1)
			id = WordStorage.getNrWord();
		else if (id > WordStorage.getNrWord())
			id = 1;
		setCurWord(WordStorage.getByID(id));
	}
}
