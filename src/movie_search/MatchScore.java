package movie_search;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MatchScore {
	public static double Calculate(String str1, String str2) {
		int maxLen = Math.max(str1.length(), str2.length()),
				minLen = Math.min(str1.length(), str2.length());
		if(maxLen == 0) return 0.0;
		if(minLen < 3 && maxLen - minLen >= 5) return 0.0;
		List<String> pairs1 = WordLetterPairs(str1.toUpperCase(Locale.US));
		List<String> pairs2 = WordLetterPairs(str2.toUpperCase(Locale.US));

		int intersection = 0;
		int union = pairs1.size() + pairs2.size();

		for (int i = 0; i < pairs1.size(); i++) {
			for (int j = 0; j < pairs2.size(); j++) {
				if (pairs1.get(i).equals(pairs2.get(j))) {
					intersection++;
					pairs2.remove(j);// Must remove the match to prevent "GGGG"
										// from appearing to match "GG" with
										// 100% success

					break;
				}
			}
		}

		return (2.0 * intersection) / union;
	}
	
	public static double CaclulatePart(String input, String pattern) {
		String[] inputWords = input.split("\\s"),
				patternWords = input.split("\\s");
		if(inputWords.length <= patternWords.length) return 0;
		String str1 = "";
		int i;
		for(i = 0; i < patternWords.length - 1; i++) {
			str1 += inputWords[i] + " ";
		}
		return Calculate(str1 + inputWords[i], pattern);
	}
	
	public static String GetRest(String input, String pattern) {
		String[] inputWords = input.split("\\s"),
				patternWords = input.split("\\s");
		String rest = "";
		if(patternWords.length >= inputWords.length) return rest;
		int i;
		for(i = patternWords.length; i < inputWords.length - 1; i++) {
			rest += inputWords[i] + " ";
		}
		return rest + inputWords[i];
	}

	// / Gets all letter pairs for each
	// / individual word in the String
	private static List<String> WordLetterPairs(String str) {
		List<String> AllPairs = new ArrayList<String>();

		// Tokenize the String and put the tokens/words into an array
		String[] Words = str.split("\\s");

		// For each word
		for (int w = 0; w < Words.length; w++) {
			if (Words[w].length() > 0) {
				// Find the pairs of characters
				String[] PairsInWord = LetterPairs(Words[w]);

				for (int p = 0; p < PairsInWord.length; p++) {
					AllPairs.add(PairsInWord[p]);
				}
			}
		}

		return AllPairs;
	}

	// / Generates an array containing every
	// / two consecutive letters in the input String
	private static String[] LetterPairs(String str) {
		int numPairs = str.length() - 1;

		String[] pairs = new String[numPairs];

		for (int i = 0; i < numPairs; i++) {
			pairs[i] = str.substring(i, i + 2);
		}

		return pairs;
	}
}
