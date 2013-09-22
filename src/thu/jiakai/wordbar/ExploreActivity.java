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
		setCurWord(WordStorage.getFirtNewWord());
		wordIDET = (EditText) findViewById(R.id.wordIDEditText);
	}

	private void setCurWord(Word w) {
		curWord = w;
		TextView titleTV = (TextView) findViewById(R.id.wordTitleTextView), transTV = (TextView) findViewById(R.id.wordDefTextView);
		titleTV.setText(w.spell);
		transTV.setText(w.definition);
		wordIDET.setText(String.valueOf(w.id));
	}

	public void onGoClicked(View view) {
		setCurWord(WordStorage.getByID(Integer.parseInt(wordIDET.getText()
				.toString())));
	}

	public void onPrevClicked(View view) {
		setCurWord(WordStorage.getByID(curWord.id - 1));
	}
	
	public void onNextClicked(View view) {
		setCurWord(WordStorage.getByID(curWord.id + 1));
	}
	
	public void onAddToReviewClicked(View view) {
		WordStorage.setReviewFlag(curWord, 1);
	}
}
