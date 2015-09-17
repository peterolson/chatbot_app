package movie_search;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Intention {
	public static enum Type {
		MOVIE, MOVIE_DIRECTOR, MOVIE_ACTOR, MOVIE_PERSON, UNKNOWN
	}

	public Type type;
	public String text;
	public List<String> matchedMovieGenres = new ArrayList<>();
	public List<String> originalMovieGenreWords = new ArrayList<>();

	public Intention(String input) {
		input = matchMovieGenres(input, matchedMovieGenres, originalMovieGenreWords);
		Data[] possibleIntentions = new Data[] {
				matchMovieDirector(input),
				matchMovieActor(input),
				matchMoviePerson(input),
				matchMovie(input),
				matchMoviePerson2(input)
				};
		Data bestIntention = possibleIntentions[0];
		for (Data intention : possibleIntentions) {
			if (intention.score > bestIntention.score) {
				bestIntention = intention;
			}
		}
		if (bestIntention.score < 0.4) {
			bestIntention = new Data(Type.UNKNOWN, 0, input);
		}
		type = bestIntention.type;
		text = bestIntention.text.trim();
	}

	private static String[] moviesVariations = new String[] { "movie", "the movie", "a movie", "films", "film", "the film", "a film" },
			watchVariations = new String[] { "to watch", "see", "to see", "will watch", "will see" }, 
			wantVariations = new String[] {"wanna", "would like", "like" , "can i", "can" },
			iVariations = new String[] { "(?: )?", "id" },
			showMeVariations = new String[] { "show" },
			somethingVariations = new String[] { "some thing", "some things", "thing", "things", "a thing", "the thing", "the things" },
			directorVariations = new String[] { "by director", "by the director", "directed", "directed by" },
			starringVariations = new String[] {"with actor", "with the actor", "with actress", "with the actress", "actor", "the actor", "actress", "the actress", "by actor", "by the actor", "by actress", "by the actress", "acted", "acted by", "acting", "featuring", "starred by"},
			byVariations = new String[] {"with"};
	
	private static Pattern moviePattern = createMoviePattern(),
			movieDirectorPattern = createMovieDirectorPattern(),
			movieActorPattern = createMovieActorPattern(),
			moviePersonPattern = createMoviePersonPattern(),
			moviePersonPattern2 = createMoviePersonPattern2();
	
	private static Pattern createPattern(List<String> patterns) {
		String expression = "(?:" + patterns.get(0) + ")";
		for(int i = 1; i < patterns.size(); i++){ 
			expression += "|(?:" + patterns.get(i) + ")";
		}
		return Pattern.compile(expression);
	}
	
	private static Pattern createMoviePattern() {
		List<String> p = new ArrayList<>();
		p.add("i want watch movies");
		p.add("i want watch");
		p.add("show me movies");
		p.add("watch movies");
		p.add("^watch");
		p.add("^movies");
		p.add("^show me");
		addVariations(p, "movies", moviesVariations);
		addVariations(p, "watch", watchVariations);
		addVariations(p, "want", wantVariations);
		addVariations(p, "i ", iVariations);
		addVariations(p, "show me", showMeVariations);
		return createPattern(p);
	}
	
	private static Pattern createMovieDirectorPattern() {
		List<String> p = new ArrayList<>();
		p.add("i want watch movies director");
		p.add("i want watch something director");
		p.add("i want watch director");
		p.add("show me movies director");
		p.add("show me something director");
		p.add("watch something director");
		p.add("watch director");
		p.add("movies director");
		p.add("show me director");
		p.add("something director");
		p.add("^director");
		addVariations(p, "director", directorVariations);
		addVariations(p, "movies", moviesVariations);
		addVariations(p, "watch", watchVariations);
		addVariations(p, "want", wantVariations);
		addVariations(p, "i ", iVariations);
		addVariations(p, "show me", showMeVariations);
		addVariations(p, "something", somethingVariations);
		return createPattern(p);
	}
	
	private static Pattern createMovieActorPattern() {
		List<String> p = new ArrayList<>();
		p.add("i want watch movies starring");
		p.add("i want watch something starring");
		p.add("i want watch starring");
		p.add("show me movies starring");
		p.add("show me something starring");
		p.add("watch something starring");
		p.add("watch starring");
		p.add("movies starring");
		p.add("show me starring");
		p.add("something starring");
		p.add("^starring");
		addVariations(p, "starring", starringVariations);
		addVariations(p, "movies", moviesVariations);
		addVariations(p, "watch", watchVariations);
		addVariations(p, "want", wantVariations);
		addVariations(p, "i ", iVariations);
		addVariations(p, "show me", showMeVariations);
		addVariations(p, "something", somethingVariations);
		return createPattern(p);
	}
	
	private static Pattern createMoviePersonPattern() {
		List<String> p = new ArrayList<>();
		p.add("i want watch movies by");
		p.add("i want watch (.{4,}) movies");
		p.add("i want watch something by");
		p.add("i want watch by");
		p.add("show me movies by");
		p.add("show me something by");
		p.add("watch something by");
		p.add("show me (.{4,}) movies");
		p.add("watch (.{4,}) movies");
		p.add("watch by");
		p.add("movies by");
		p.add("show me by");
		p.add("^something by");
		p.add("^by");
		addVariations(p, "by", byVariations);
		addVariations(p, "movies", moviesVariations);
		addVariations(p, "watch", watchVariations);
		addVariations(p, "want", wantVariations);
		addVariations(p, "i ", iVariations);
		addVariations(p, "show me", showMeVariations);
		addVariations(p, "something", somethingVariations);
		return createPattern(p);
	}
	
	public static Pattern createMoviePersonPattern2() { // patterns to check after Movie Patterns to avoid conflicts
		List<String> p = new ArrayList<>();
		p.add("(.{4,}) movies");
		addVariations(p, "movies", moviesVariations);
		addVariations(p, "watch", watchVariations);
		addVariations(p, "want", wantVariations);
		addVariations(p, "i ", iVariations);
		addVariations(p, "show me", showMeVariations);
		return createPattern(p);
	}

	private Data matchMovie(String input) {
		return matchAny(Type.MOVIE, moviePattern, input);
	}

	private Data matchMovieDirector(String input) {
		return matchAny(Type.MOVIE_DIRECTOR, movieDirectorPattern, input);
	}
	
	private Data matchMovieActor(String input) {
		return matchAny(Type.MOVIE_ACTOR, movieActorPattern, input);
	}
	
	private Data matchMoviePerson(String input) {
		return matchAny(Type.MOVIE_PERSON, moviePersonPattern, input);
	}
	
	private Data matchMoviePerson2(String input) {
		return matchAny(Type.MOVIE_PERSON, moviePersonPattern2, input);
	}
	
	private String matchMovieGenres(String input, List<String> matches, List<String> originals) {
		String[] words = input.split("\\s");
		String[] genres = MovieData.GenreKeys;
		String out = "";
		for(int i = 0; i < words.length; i++) {
			for(String genre : genres) {
				if(MatchScore.Calculate(words[i], genre) > 0.8) {
					matches.add(genre);
					originals.add(words[i]);
					words[i] = "";
					break;
				}
			}
			if(!words[i].isEmpty()) {
				out += (out.isEmpty()) ? words[i] : " " + words[i];
			}
		}
		return out;
	}

	private Data matchAny(Type type, Pattern pattern, String input) {
		Matcher matcher = pattern.matcher(input);
		if(matcher.find()) {
			int start = matcher.start(),
					end = matcher.end();
			String subPattern = "";
			int count = matcher.groupCount();
			int subStart = -1, subEnd = -1;
			for(int i = 1; i <= count; i++) {
				subStart = matcher.start(i);
				subEnd = matcher.end(i);
				if(subStart >= 0) break;
			}
			if(subStart >= 0) {
				subPattern = (start > 0 ? " " : "") + input.substring(subStart, subEnd) + (end < input.length() ? " " : "");
			}
			return new Data(type, 1, input.substring(0, start) + subPattern + input.substring(end));
		}
		return new Data(type, 0, input);
	}

	private static void addVariations(List<String> list, String word,
			String[] variations) {
		int length = list.size();
		for (int i = 0; i < length; i++) {
			String item = list.get(i);
			if (item.contains(word)) {
				String pattern = "(?:" + word;
				for (String variation : variations) {
					pattern += "|" + variation;
				}
				pattern += ")";
				list.set(i, item.replace(word, pattern));
			}
		}
	}

	private class Data {
		public Type type;
		public double score;
		public String text;

		public Data(Type type, double score, String text) {
			this.type = type;
			this.score = score;
			this.text = text;
		}
	}
}