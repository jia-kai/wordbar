package thu.jiakai.wordbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends WBActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		TextView logTV = (TextView) findViewById(R.id.logTextView);
		WordStorage.buildDB(this, (ScrollView) findViewById(R.id.scrollView1), logTV);
		logTV.append("\nversion: 2013-09-24\n");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		WordStorage.release();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void onExploreClick(View view) {
		startActivity(new Intent(this, ExploreActivity.class));
	}

	public void onReviewClicked(View view) {
		startActivity(new Intent(this, ReviewActivity.class));
	}

	public void onAutoPlayClicked(View view) {
		startActivity(new Intent(this, AutoPlayActivity.class));
	}
}
