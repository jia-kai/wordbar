package thu.jiakai.wordbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class Utils {
	public static String formatTime(long timestamp) {
		return (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US))
				.format(new Date(timestamp));
	}

	public static <K> void randomShuffle(ArrayList<K> list) {
		 Random rnd = new Random();
		 for (int i = 0; i < list.size(); i ++) {
			 int j = rnd.nextInt(list.size() - i) + i;
			 K t = list.get(i);
			 list.set(i, list.get(j));
			 list.set(j, t);
		 }
	}

}
