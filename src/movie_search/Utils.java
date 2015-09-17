package movie_search;

public class Utils {
	public static <T> String join (T[] x, String separator) {
		if(x.length == 0) return "";
		StringBuilder builder = new StringBuilder(x[0].toString());
		for(int i = 1; i < x.length; i++) {
			builder.append(separator + x[i].toString());
		}
		return builder.toString();
	}
}
