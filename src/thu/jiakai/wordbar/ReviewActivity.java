package thu.jiakai.wordbar;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class ReviewActivity extends Activity {
	Word curWord;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_review);
		getNextWord();
	}

	private void onUserResponse(int rst) {
		MemoryModel.setUserResponse(curWord, rst);
	}
	
	private void getNextWord() {
		curWord = MemoryModel.getNextToReview();
		((TextView)findViewById(R.id.wordTitleTextView)).setText(curWord.spell);
	}

	public void onShowDefClicked(View view) {
		((TextView)findViewById(R.id.wordDefTextView)).setText(curWord.definition);
	}
	
	public void onYesClicked(View view) {
		onUserResponse(1);
	}

	public void onNoClicked(View view) {
		onUserResponse(0);
	}
	

}
