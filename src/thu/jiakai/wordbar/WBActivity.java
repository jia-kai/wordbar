package thu.jiakai.wordbar;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;

/*
 * base class for wordbar activities
 */
public class WBActivity extends Activity {
	static protected Typeface fontType = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (fontType == null)
			fontType = Typeface.createFromAsset(getAssets(), "segoeui.ttf");
		
		WordStorage.init(this);
	}
	
}
