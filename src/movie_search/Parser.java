package movie_search;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import movie_search.MovieData.Movie;


import com.example.speechrecognitionplayground.R;

import android.content.Context;
import android.content.res.Resources;

public class Parser {

	private String description;
	private List<String> matchedMovieGenres;
	private List<String> originalMovieGenreWords;

	public Parser(String input) {
		input = input.replaceAll("[^a-zA-Z ]", "").toLowerCase(Locale.US);
		Intention intention = new Intention(input);
		String text = intention.text;
		original = text;
		matchedMovieGenres = intention.matchedMovieGenres;
		originalMovieGenreWords = intention.originalMovieGenreWords;
		switch (intention.type) {
		case MOVIE:
			description = suggestMovies(text);
			break;
		case MOVIE_DIRECTOR:
			description = suggestMoviesByDirector(text);
			break;
		case MOVIE_ACTOR:
			description = suggestMoviesStarring(text);
			break;
		case MOVIE_PERSON:
			description = suggestMoviesWithPerson(text);
			break;
		default:
			String suggestion = suggestUnkown(text);
			if(suggestion != "") description = suggestion;
			else description = "Sorry, I can't quite understand...";
			break;
		}
	}
	
	private String suggestMovies(String input) {
		int n = 5;
		Tuple<Movie[], Double[]> t = closestMovieCandidatesGenre(input, n);
		return createMovieList(t, input);
	}
	
	private boolean useMovieGenreFilter = false;
	private String original;
	
	private Tuple<Movie[], Double[]> closestMovieCandidatesGenre(String input, int n) {
		if(originalMovieGenreWords.size() == 0)
			return closestMovieCandidates(input, n, false);
		original = input + " " + Utils.join(originalMovieGenreWords.toArray(), " ");
		if(input.trim().isEmpty()) return findMoviesByGenre();
		Tuple<Movie[], Double[]> t1 = closestMovieCandidates(input, n, true),
				t2 = closestMovieCandidates(original, n, false);
		if(t1.x.length == 0 && t1.y.length == 0) return findMoviesByGenre();
		useMovieGenreFilter = t1.y[0] > t2.y[0];
		return useMovieGenreFilter ? t1 : t2;
	}
	
	private Tuple<Movie[], Double[]> closestMovieCandidates(String input, int n, boolean matchGenres) {
		Movie[] topSuggestions = new Movie[n];
		Double[] topSuggestionScores = new Double[n];
		for(int i = 0; i < n; i++) topSuggestionScores[i] = 0.0;
		for (Movie movie : MovieData.Movies) {
			if(matchGenres && !movie.hasGenres(matchedMovieGenres)) continue;
			double score = MatchScore.Calculate(input, movie.title);
			trySuggestion(topSuggestions, topSuggestionScores, movie, score);
		}
		return new Tuple<>(topSuggestions, topSuggestionScores);
	}
	
	private Tuple<Movie[], Double[]> findMoviesByGenre() {
		useMovieGenreFilter = true;
		List<Movie> results = new ArrayList<>();
		List<Double> scores = new ArrayList<>();
		for(Movie movie : MovieData.Movies) {
			if(movie.hasGenres(matchedMovieGenres)) {
				results.add(movie);
				scores.add(1.0);
			}
		}
		return new Tuple<Movie[], Double[]>(results.toArray(new Movie[]{}), scores.toArray(new Double[]{}));
	}
	
	private String createMovieList(Tuple<Movie[], Double[]> t, String input) {
		String suggestions = (input.isEmpty() ? "" : "Movie search for '" + (useMovieGenreFilter ? input : original) + "':\n") + (useMovieGenreFilter ? movieGenreText() : "");
		for(int i = 0; i < t.x.length; i++)
		{
			suggestions += t.x[i].getTitle() + " - " + Math.round(t.y[i] * 100) + "%\n";
		}
		return suggestions;
	}
	
	private String suggestMoviesByDirector(String input) {
		Tuple<String, Double> t = closestDirectorCandidate(input);
		return createDirectorMovieList(t, input);
	}
	
	private Tuple<String, Double> closestDirectorCandidate(String input) {
		String topDirector = "";
		double topScore = 0;
		for(String director : MovieData.DirectorKeys) {
			double score = MatchScore.Calculate(input, director);
			if(score > topScore) {
				topScore = score;
				topDirector = director;
			}
		}
		return new Tuple<>(topDirector, topScore);
	}
	
	private String createDirectorMovieList(Tuple<String, Double> t, String input) {
		String suggestions = "Movies directed by " + t.x + ":\n" + movieGenreText();
		List<Movie> list = MovieData.Directors.get(t.x);
		int length = list.size();
		for(int i = 0; i < length; i++) {
			Movie movie = list.get(i);
			if(movie.hasGenres(matchedMovieGenres)) {
				suggestions += movie.getTitle() + "\n";
			}
		}
		return suggestions;
	}
	
	private String suggestMoviesStarring(String input) {
		Tuple<String, Double> t = closestActorCandidate(input);
		return createActorMovieList(t, input);
	}
	
	private Tuple<String, Double> closestActorCandidate(String input) {
		String topActor = "";
		double topScore = 0;
		for(String director : MovieData.ActorKeys) {
			double score = MatchScore.Calculate(input, director);
			if(score > topScore) {
				topScore = score;
				topActor = director;
			}
		}
		return new Tuple<>(topActor, topScore);
	}
	
	private String createActorMovieList(Tuple<String, Double> t, String input) {
		String suggestions = "Movies starring " + t.x + ":\n" + movieGenreText();
		List<Movie> list = MovieData.Actors.get(t.x);
		int length = list.size();
		for(int i = 0; i < length; i++) {
			Movie movie = list.get(i);
			if(movie.hasGenres(matchedMovieGenres)) {
				suggestions += movie.getTitle() + "\n";
			}
		}
		return suggestions;
	}
	
	private String suggestMoviesWithPerson(String input) {
		Tuple<String, Double> tActor = closestActorCandidate(input),
				tDirector = closestDirectorCandidate(input);
		double threshold = 0.5;
		if(Math.max(tActor.y, tDirector.y) < threshold) return suggestMovies(input);
		String actorMovieList = tActor.y >= threshold ? createActorMovieList(tActor, input) : "";
		String directorMovieList = tDirector.y >= threshold ? createDirectorMovieList(tDirector, input) : "";
		if(tActor.y > tDirector.y) {
			return actorMovieList + "\n" + directorMovieList;
		}
		return directorMovieList + "\n" + actorMovieList;
	}
	
	private String suggestUnkown(String input) {
		Tuple<Movie[], Double[]> tTitles = closestMovieCandidatesGenre(input, 5);
		Tuple<String, Double> topTitle = new Tuple<String, Double>(tTitles.x[0].title, tTitles.y[0]);
		Tuple<String, Double> tActor = closestActorCandidate(input);
		Tuple<String, Double> tDirector = closestDirectorCandidate(input);
		Tuple<String, Double>[] tuples = new Tuple[] {topTitle, tActor, tDirector};
		sortResults(tuples);
		double threshold = 0.5;
		StringBuilder suggestions = new StringBuilder();
		for(int i = 0; i < tuples.length; i++) {
			if(tuples[i].y < threshold) break;
			if(tuples[i] == topTitle) suggestions.append(createMovieList(tTitles, input));
			else if(tuples[i] == tActor) suggestions.append(createActorMovieList(tActor, input));
			else if(tuples[i] == tDirector) suggestions.append(createDirectorMovieList(tDirector, input));
			suggestions.append('\n');
		}
		return suggestions.toString();
	}
	
	// insertion sort sufficient on small inputs
	private void sortResults (Tuple<String, Double>[] tuples) {
		for(int i = 0; i < tuples.length; i++) {
			for(int j = i + 1; j < tuples.length; j++) {
				if(getScore(tuples[j]) > getScore(tuples[i])){
					Tuple<String, Double> temp = tuples[i];
					tuples[i] = tuples[j];
					tuples[j] = temp;
				}
			}
		}
	}
	
	private double getScore(Tuple<String, Double> t) {
		return t.y;
	}

	private void trySuggestion(Movie[] suggestions, Double[] scores, Movie suggestion, double score) {
		int i = scores.length - 1;
		if(score <= scores[i]) return;
		while(i >= 0 && score > scores[i]) i--;
		int j = scores.length - 2;
		while(j > i) {
			scores[j+1] = scores[j];
			suggestions[j+1] = suggestions[j];
			j--;
		}
		scores[i+1] = score;
		suggestions[i+1] = suggestion;
	}
	
	private String movieGenreText() {
		if(matchedMovieGenres.size() == 0) return "";
		return "Filtering by genres: " + Utils.join(matchedMovieGenres.toArray(), ", ") + "\n";
	}

	public String getDescription() {
		return description;
	}

}
