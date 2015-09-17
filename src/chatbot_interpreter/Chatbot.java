package chatbot_interpreter;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.Months;
import org.joda.time.Seconds;
import org.joda.time.Weeks;
import org.joda.time.Years;

import com.google.gson.Gson;


public class Chatbot {
	
	private Map<String, Object> bot;
	private String that, topic;
	private Map<String, Map<String, Double>> sets;
	private Set<String> setNames;
	private Map<String, String> properties;
	private Map<String, Map<String, String>> maps, substitutions;
	private Map<String, Object> patternGraph;
	private List<String> templates;
	private int size, vocabulary;
	private String undefined;
	private Integer templateIndex;
	
	private enum HardcodedSet {
		number {
			public boolean contains(String text) {
				try {
					Integer.parseInt(text);
					return true;
				} catch (NumberFormatException e) {
					return false;
				}
			}
		};
		abstract public boolean contains(String text);
	}
	
	private enum HardcodedMap {
		successor {
			public String mapTo(String text) {
				try {
					int n = Integer.parseInt(text);
					return (n + 1) + "";
				} catch (Exception e) {
					return "";
				}
			}
		};
		abstract public String mapTo(String text);
	}
	
	private Random randomGenerator = new Random();

	public Chatbot(String json) {
		bot = new Gson().fromJson(json, Map.class);
		sets = (Map<String, Map<String, Double>>) bot.get("sets");
		setNames = sets.keySet();
		properties = ((Map<String, String>)bot.get("properties"));
		maps = (Map<String, Map<String, String>>) bot.get("maps");
		substitutions = (Map<String, Map<String, String>>) bot.get("substitutions");
		templates = (List<String>) bot.get("templates");
		patternGraph = (Map<String, Object>)bot.get("patternGraph");
		globals = (Map<String, String>) bot.get("defaults");
		size = (int) Math.round((Double) bot.get("size"));
		vocabulary = (int) Math.round((Double) bot.get("vocabulary"));
		undefined = properties.containsKey("default-get") ? properties.get("default-get") : "undefined";
		that = topic = undefined;
	}
	
	private List<String> requestHistory = new ArrayList<>(),
		inputHistory = new ArrayList<>(),
		responseHistory = new ArrayList<>();
	private List<List<String>> thatHistory = new ArrayList<>();
	
	public String replyTo(String text) {
		if(text.trim().isEmpty()) return "";
		requestHistory.add(text);
		text = runSubstitutions(text.toLowerCase(), substitutions.get("normal"), new WildcardMatches(new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>()));
		String[] sentences = text.split("[\\.\\!\\?。？！]");
		String response = "";
		List<String> newThat = new ArrayList<>();
		for(String sentence : sentences) {
			if(sentence.trim().isEmpty()) continue;
			inputHistory.add(sentence);
			String responseSentence = getResponse(sentence);
			that = responseSentence.substring(0, Math.min(responseSentence.length(), 50));
			newThat.add(that);
			response += responseSentence + " ";
		}
		thatHistory.add(newThat);
		response = response.replaceAll("( )+", " ");
		responseHistory.add(response);
		return response;
	}
	
	private String getResponse(String text) {
		List<Double> templateIds = getTemplatesFromText(text);
		if(templateIds.size() == 0) return "I don't know what to say. :(";
		templateIndex = (int) Math.round(templateIds.get(randomGenerator.nextInt(templateIds.size())));
		return parseTemplate(templates.get(templateIndex), new WildcardMatches(wildcardMatches, thatWildcardMatches, topicWildcardMatches));
	}
	
	private class WildcardMatches {
		public List<String> text, that, topic;
		public WildcardMatches(List<String> text, List<String> that, List<String> topic){
			this.text = text;
			this.that = that;
			this.topic = topic;
		}
	}
	
	private ArrayList<String> wildcardMatches, thatWildcardMatches, topicWildcardMatches, matchedPatternWords;
	
	private List<Double> getTemplatesFromText(String text) {
		text += " <that> " + that + " <topic> " + topic;
		String[] words = separateChineseCharacters(text).toLowerCase().split(" ");
		wildcardMatches = new ArrayList<>();
		thatWildcardMatches = new ArrayList<>();
		topicWildcardMatches = new ArrayList<>();
		matchedPatternWords = new ArrayList<>();
		return traverse(patternGraph, words, wildcardMatches);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<Double> traverse(Map<String, Object> node, String[] words, List<String> currentMatches) {		
		if(words.length == 0) {
			if(node.containsKey("#") && ((Map<String, Object>) node.get("#")).containsKey(" ")) {
				currentMatches.add("");
				matchedPatternWords.add("#");
				return (List<Double>) ((Map)node.get("#")).get(" ");
			}
			if(node.containsKey(" ")) {
				return (List<Double>) node.get(" ");
			}
			if(node.containsKey("^") && ((Map<String, Object>) node.get("^")).containsKey(" ")) {
				currentMatches.add("");
				matchedPatternWords.add("^");
				return (List<Double>) ((Map)node.get("^")).get(" ");
			}
			return new ArrayList<>();
		}
		String word = words[0];
		String[] tail = Arrays.copyOfRange(words, 1, words.length);
		
		if(word.equals("<that>")) currentMatches = thatWildcardMatches;
		if(word.equals("<topic>")) currentMatches = topicWildcardMatches;
		
		List<Double> results;
		
		results = simpleMatch(node, "$" + word, tail, currentMatches);
		if(results.size() > 0) return results;
		
		results = wildcardMatch(node, "#", words, 0, currentMatches);
		if(results.size() > 0) return results;
		
		results = wildcardMatch(node, "_", words, 1, currentMatches);
		if(results.size() > 0) return results;
		
		results = simpleMatch(node, word, tail, currentMatches);
		if(results.size() > 0) return results;
		
		results = propertyMatch(node, word, tail, currentMatches);
		if(results.size() > 0) return results;
		
		results = setMatch(node, word, tail, currentMatches);
		if(results.size() > 0) return results;
		
		results = wildcardMatch(node, "^", words, 0, currentMatches);
		if(results.size() > 0) return results;
		
		results = wildcardMatch(node, "*", words, 1, currentMatches);
		if(results.size() > 0) return results;
		
		return new ArrayList<>();
	}
	
	private List<Double> simpleMatch(Map<String, Object> node, String word, String[] tail, List<String> currentMatches) {
		if(node.containsKey(word)) {
			int size = matchedPatternWords.size();
			List<Double> results = traverse((Map<String, Object>) node.get(word), tail, currentMatches);
			if(results.size() > 0) {
				matchedPatternWords.add(size, word);
				return results;
			}
			matchedPatternWords = new ArrayList<>(matchedPatternWords.subList(0, size));
		}
		return new ArrayList<>();
	}
	
	private List<Double> wildcardMatch(Map<String, Object> node, String wildcard, String[] words, int start, List<String> currentMatches) {
		if(node.containsKey(wildcard)) {
			int wildcardSize = currentMatches.size(),
				matchedSize = matchedPatternWords.size(),
				end = words.length;
			List<Double> results;
			for(int i = start; i <= end; i++) {
				String[] tail = Arrays.copyOfRange(words, i, end);
				results = traverse((Map<String, Object>) node.get(wildcard), tail, currentMatches);;
				if(results.size() > 0) {
					matchedPatternWords.add(matchedSize, wildcard);
					currentMatches.add(wildcardSize, StringUtils.join(Arrays.copyOfRange(words, 0, i), ' '));
					return results;
				}
				currentMatches = currentMatches.subList(0, wildcardSize);
				matchedPatternWords = new ArrayList<>(matchedPatternWords.subList(0, matchedSize));
			}
		}
		return new ArrayList<>();
	}
	
	private List<Double> setMatch(Map<String, Object> node, String word, String[] tail, List<String> currentMatches) {
		for (String set : setNames) {
			String name = "{{set:" + set.toLowerCase() + "}}";
			if(node.containsKey(name) && sets.get(set).containsKey(word)) {
				List<Double> results = populateSetMatch(name, node, word, tail, currentMatches);
				if(!results.isEmpty()) return results;
			}
		}
		for(HardcodedSet set : HardcodedSet.values()) {
			String name = "{{set:" + set.name().toLowerCase() + "}}";
			if(node.containsKey(name) && set.contains(word)) {
				List<Double> results = populateSetMatch(name, node, word, tail, currentMatches);
				if(!results.isEmpty()) return results;
			}
		}
		return new ArrayList<>();
	}
	
	private List<Double> populateSetMatch(String name, Map<String, Object> node, String word, String[] tail, List<String> currentMatches) {
		int matchedSize = matchedPatternWords.size(),
				wildcardSize = currentMatches.size();
			List<Double> results = traverse((Map<String, Object>) node.get(name), tail, currentMatches);
			if(results.size() > 0) {
				matchedPatternWords.add(matchedSize, name);
				currentMatches.add(wildcardSize, word);
				return results;
			}
			currentMatches = currentMatches.subList(0, wildcardSize);
			matchedPatternWords = new ArrayList<>(matchedPatternWords.subList(0, matchedSize));
			return new ArrayList<>();
	}
	
	private List<Double> propertyMatch(Map<String, Object> node, String word, String[] tail, List<String> currentMatches) {
		for (String property : properties.keySet()) {
			String name = "{{property:" + property.toLowerCase() + "}}";
			if(node.containsKey(name) && word == properties.get(property).toLowerCase()) {
				int matchedSize = matchedPatternWords.size(),
						wildcardSize = currentMatches.size();
					List<Double> results = traverse((Map<String, Object>) node.get(name), tail, currentMatches);
					if(results.size() > 0) {
						matchedPatternWords.add(matchedSize, name);
						currentMatches.add(wildcardSize, word);
						return results;
					}
					currentMatches = currentMatches.subList(0, wildcardSize);
					matchedPatternWords = new ArrayList<>(matchedPatternWords.subList(0, matchedSize));
			}
		}
		return new ArrayList<>();
	}
	
	private String parseTemplate(String text, WildcardMatches wildcardMatches) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if(c == '{' && text.charAt(i + 1) == '{') {
				int closeIndex = getCloseIndex(text, i);
				out.append(parseInterior(text, i, closeIndex, wildcardMatches));
				i = closeIndex;
				continue;
			}
			hasStar = false;
			i = parseStar(text, i, "$", wildcardMatches.text, out);
			if(hasStar) continue;
			i = parseStar(text, i, "that$", wildcardMatches.that, out);
			if(hasStar) continue;
			i = parseStar(text, i, "topic$", wildcardMatches.topic, out);
			if(hasStar) continue;
			out.append(c);
		}
		return out.toString();
	}
	
	private boolean hasStar = false;
	
	private int parseStar(String text, int i, String star, List<String> wildcardMatches, StringBuilder out) {
		int end = i + star.length();
		if(end > text.length()) end = text.length();
		if(text.substring(i, end).equals(star)) {
			hasStar = true;
			i += star.length();
			if(i >= text.length()) {
				if(!wildcardMatches.isEmpty()) {
					out.append(wildcardMatches.get(0));
				}
				return i;
			}
			if(text.charAt(i) == '$') {
				out.append(star + '$');
				return i;
			}
			String index = "";
			while(i < text.length() && Character.isDigit(text.charAt(i))) {
				index += text.charAt(i);
				i++;
			}
			if(index.isEmpty()) {
				if(!wildcardMatches.isEmpty()) {
					out.append(wildcardMatches.get(0));
				} else {
					out.append("");
				}
			}
			else {
				int x = Integer.parseInt(index) - 1;
				if(0 <= x && x < wildcardMatches.size()) {
					out.append(wildcardMatches.get(x));
				} else {
					out.append("");
				}
			}
			i--;
			return i;
		}
		return i;
	}
	
	private String parseInterior(String text, int start, int end, WildcardMatches matches) {
		String interior = text.substring(start + 2, end - 1);
		int colonIndex = interior.indexOf(':');
		return parseTag(interior.substring(0, colonIndex), interior.substring(colonIndex + 1), matches);
	}
	
	private Map<String, String> globals = new HashMap<>();
	private Map<Integer, Map<String, String>> locals = new HashMap<>();
	
	private String parseTag(String label, String text, WildcardMatches matches) {
		List<String> parts;
		String name, value;
		switch (label) {
		case "random":
			List<String> children = getChildren(text);
			for(int i = children.size() - 1; i >= 0; i--) {
				String child = children.get(i);
				if(child.length() < 2) children.remove(i);
			}
			if(children.isEmpty()) return "";
			String child = children.get(randomGenerator.nextInt(children.size()));
			return parseTemplate(child.substring(5, child.length() - 2), matches);
		case "srai":
			return getResponse(parseTemplate(text, matches)) + " ";
		case "set":
			parts = split(text, '=');
			name = parseTemplate(parts.get(0), matches);
			value = parseTemplate(parts.get(1), matches).trim();
			globals.put(name, value);
			if(name == "topic") topic = value;
			return value;
		case "setvar":	
			parts = split(text, '=');
			name = parseTemplate(parts.get(0), matches);
			value = parseTemplate(parts.get(1), matches).trim();
			if(!locals.containsKey(templateIndex)) {
				locals.put(templateIndex, new HashMap<String, String>());
			}
			locals.get(templateIndex).put(name, value);
			return value;
		case "get":
			name = parseTemplate(text, matches);
			return globals.containsKey(name) ? globals.get(name) : undefined;
		case "getvar":
			name = parseTemplate(text, matches);
			if(!locals.containsKey(templateIndex)) {
				locals.put(templateIndex, new HashMap<String, String>());
			}
			Map<String, String> scope = locals.get(templateIndex);
			return scope.containsKey(name) ? scope.get(name) : undefined;
		case "think":
			parseTemplate(text, matches);
			return "";
		case "condition":
			parts = split(text, '|');
			int arity = parts.size();
			String first = parts.get(0), second = "", third = "";
			if(arity > 1) second = parts.get(1);
			if(arity > 2) third = parts.get(2);
			StringBuilder out = new StringBuilder();
			int loopCount = 0, MAX_LOOPS = 102;
			while(loopCount++ < MAX_LOOPS) {
				if(arity == 3) {
					if(equals(getVar(first, matches), parseTemplate(second, matches))) out.append(parseTemplate(third, matches));
				} else if(arity == 2) {
					value = getVar(first, matches);
					parts = getChildren(second);
					for(String part : parts) {
						if(part.length() < 2) continue;
						List<String> inside = split(part.substring(5, part.length() - 2), '|');
						if(inside.size() == 1) {
							out.append(parseTemplate(inside.get(0), matches));
							break;
						}
						if(equals(value, parseTemplate(inside.get(0), matches))) {
							out.append(parseTemplate(inside.get(1), matches));
							break;
						}
					}
				} else {
					parts = getChildren(first);
					for(String part : parts) {
						if(part.length() < 2) continue;
						List<String> inside = split(part.substring(5, part.length() - 2), '|');
						if(inside.size() == 1) {
							out.append(parseTemplate(inside.get(0), matches));
							break;
						}
						if(inside.size() == 3) {
							if(equals(getVar(inside.get(0), matches), parseTemplate(inside.get(1), matches))) {
								out.append(parseTemplate(inside.get(2), matches));
								break;
							}
						}
					}
				}
				String outString = out.toString();
				int loopIndex = outString.indexOf("@@@@");
				if(loopIndex >= 0) {
					out = new StringBuilder();
					out.append(outString.substring(0, loopIndex));
					continue;
				}
				break;
			}
			if(loopCount >= MAX_LOOPS) return "Too many loops";
			return out.toString();
		case "li":
			return "{{li:" + text + "}}";
		case "loop":
			return "@@@@";
		case "map":
			parts = split(text, '|');
			String mapName = parseTemplate(parts.get(0), matches);
			name = parseTemplate(parts.get(1), matches).toLowerCase();
			if(maps.containsKey(mapName)) {
				Map<String, String> map = maps.get(mapName);
				return map.containsKey(name) ? map.get(name) : undefined;
			}
			for(HardcodedMap map : HardcodedMap.values()) {
				if(map.name().equalsIgnoreCase(mapName)) {
					return map.mapTo(name);
				}
			}
			return undefined;
		case "property":
			name = parseTemplate(text, matches);
			return properties.containsKey(name) ? properties.get(name) : undefined;
		case "formal": 
			return WordUtils.capitalize(parseTemplate(text, matches));
		case "lowercase":
			return parseTemplate(text, matches).toLowerCase();
		case "uppercase":
			return parseTemplate(text, matches).toUpperCase();
		case "sentence":
			text = parseTemplate(text, matches);
			return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
		case "denormal":
			return runSubstitutions(text, substitutions.get("denormal"), matches);
		case "normal":
			return runSubstitutions(text, substitutions.get("normal"), matches);
		case "gender":
			return runSubstitutions(text, substitutions.get("gender"), matches);
		case "person":
			return runSubstitutions(text, substitutions.get("person"), matches);
		case "person2":
			return runSubstitutions(text, substitutions.get("person2"), matches);
		case "explode":
			return StringUtils.join(parseTemplate(text, matches).split(""), ' ');
		case "system":
			// TODO
			return "Sorry, system call are not supported. :(";
		case "vocabulary":
			return Integer.toString(vocabulary);
		case "size":
			return Integer.toString(size);
		case "program":
			return "Java chatbot interpreter";
		case "that":
			if(!text.isEmpty()) {
				text = parseTemplate(text, matches);
				String[] index = text.split(",");
				if(index.length == 2) {
					int m = thatHistory.size() - Integer.parseInt(index[0]);
					if(0 <= m && m < thatHistory.size()) {
						int n = thatHistory.get(m).size() - Integer.parseInt(index[1]);
						if(0 <= n && n < thatHistory.get(m).size()) return thatHistory.get(m).get(n);
					}
				}
			}
			return that;
		case "input":
			return getHistoryItem(inputHistory, text, matches);
		case "request":
			return getHistoryItem(requestHistory, text, matches);
		case "response":
			return getHistoryItem(responseHistory, text, matches);
		case "date":
			text = parseTemplate(text, matches);
			return new SimpleDateFormat(text).format(new Date());
		case "interval":
			try {
				parts = split(text, '|');
				String jformat = parts.get(0), style = parts.get(1), from = parts.get(2), to = parts.get(3);
				jformat = parseTemplate(jformat, matches);
				style = parseTemplate(style, matches);
				from = parseTemplate(from, matches);
				to = parseTemplate(to, matches);
				SimpleDateFormat format = new SimpleDateFormat(jformat);
				DateTime fromDate = new DateTime(format.parse(from)), toDate = new DateTime(format.parse(to));
				if(style.equalsIgnoreCase("seconds")) {
					return Seconds.secondsBetween(fromDate, toDate).getSeconds() + "";
				}
				if(style.equalsIgnoreCase("minutes")) {
					return Minutes.minutesBetween(fromDate, toDate).getMinutes() + "";
				}
				if(style.equalsIgnoreCase("hours")) {
					return Hours.hoursBetween(fromDate, toDate).getHours() + "";
				}
				if(style.equalsIgnoreCase("days")) {
					return Days.daysBetween(fromDate, toDate).getDays() + "";
				}
				if(style.equalsIgnoreCase("weeks")) {
					return Weeks.weeksBetween(fromDate, toDate).getWeeks() + "";
				}
				if(style.equalsIgnoreCase("months")) {
					return Months.monthsBetween(fromDate, toDate).getMonths() + "";
				}
				if(style.equalsIgnoreCase("years")) {
					return Years.yearsBetween(fromDate, toDate).getYears() + "";
				}
				return "Invalid style";
			} catch (ParseException e) {
				return "Invalid date.";
			} catch (IndexOutOfBoundsException e) {
				return "Not enough arguments for interval tag";
			}
			
		case "addCategory":
			size++;
			parts = split(text, '|');
			String pattern = expandEvals(parts.get(0), matches),
				template = expandEvals(parts.get(1), matches);
			if(!pattern.contains("<that>")) pattern += " <that> *";
			if(!pattern.contains("<topic>")) pattern += " <topic> *";
			int templateIndex = templates.size();
			templates.add(template);
			addPattern(pattern, templateIndex);
			return "";
		case "eval":
			return parseTemplate(text, matches);
		case "hook":
			text = parseTemplate(text, matches);
			String[] split = text.split("\\|");
			name = split[0];
			for(ChatbotHooks.Hook hook : ChatbotHooks.Hook.values()) {
				if(!hook.name().equals(name)) continue;
				return hook.run(Arrays.copyOfRange(split, 1, split.length));
			}
			return "no hook with name " + name;
		default:
			return "unknown tag " + label;
		}
	}
	
	private void addPattern(String pattern, int templateIndex) {
		String[] path = separateChineseCharacters(pattern).toLowerCase().split(" ");
		List<String> words = new ArrayList<>();
		for(String word : path) {
			if(!word.isEmpty()) words.add(word);
		}
		vocabulary += words.size();
		Map<String, Object> node = patternGraph;
		for(String word : words) {
			if(!node.containsKey(word)) {
				node.put(word, new HashMap<String, Object>());
			}
			node = (Map<String, Object>) node.get(word);
		}
		if(!node.containsKey(" ")) {
			node.put(" ", new ArrayList<Double>());
		}
		((List<Double>)node.get(" ")).add((double) templateIndex);
	}
	
	private String expandEvals(String text, WildcardMatches matches) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			if(i + 6 < text.length() && text.substring(i, i + 6).equals("{{eval")) {
				int closeIndex = getCloseIndex(text, i);
				out.append(parseTemplate(text.substring(i + 7, closeIndex - 1), matches));
				i = closeIndex;
			} else {
				out.append(text.charAt(i));
			}
		}
		return out.toString();
	}
	
	private String getHistoryItem(List<String> list, String text, WildcardMatches matches) {
		int n = list.size() - Integer.parseInt(parseTemplate(text, matches));
		if(0 <= n && n < list.size()) return list.get(n);
		return list.isEmpty() ? undefined : list.get(list.size() - 1);
	}
	
	private String runSubstitutions(String text, Map<String, String> subs, WildcardMatches matches) {
		if(text.isEmpty()) text = "$";
		text = " " + parseTemplate(text, matches).toLowerCase() + " ";
		if(subs == null) return text.trim();
		Map<String, String> lowerToActual = new HashMap<>();
		int maxLength = 0;
		for(String key : subs.keySet()) {
			if(key.length() > maxLength) maxLength = key.length();
			lowerToActual.put(key.toLowerCase(), key);
		}
		StringBuilder replacement = new StringBuilder();
		outer: for(int i = 1; i < text.length(); i++) {
			for(int j = Math.min(maxLength, text.length() - i); j > 0; j--) {
				String slice = text.substring(i, i + j);
				String substitution = null;
				if(lowerToActual.containsKey(slice)) {
					substitution = subs.get(lowerToActual.get(slice));
				}
				else if (text.charAt(i - 1) == ' ' && lowerToActual.containsKey(" " + slice)) {
					substitution = subs.get(lowerToActual.get(" " + slice));
				}
				if(substitution != null) {
					replacement.append(substitution);
					i += j - 1;
					continue outer;
				}
			}
			replacement.append(text.charAt(i));
		}
		return replacement.toString().trim();
	}
	
	private String getVar(String text, WildcardMatches matches) {
		String[] parts = text.split("=");
		if(parts[0] == "var") return parseTag("getvar", parts[1], matches);
		return parseTag("get", parts[1], matches);
	}
	
	private boolean equals(String a, String b) {
		return (!a.equals(undefined) && b.equals("*")) || a.equalsIgnoreCase(b);
	}
	
	private List<String> split(String text, char separator) {
		List<String> children = getChildren(text);
		StringBuilder part = new StringBuilder();
		List<String> parts = new ArrayList<>();
		for(String child : children) {
			if(child.length() > 1) {
				part.append(child);
				continue;
			}
			char c = child.charAt(0);
			if(c == separator) {
				parts.add(part.toString());
				part = new StringBuilder();
			} else {
				part.append(c);
			}
		}
		parts.add(part.toString());
		return parts;
	}
	
	private List<String> getChildren(String text) {
		List<String> children = new ArrayList<>();
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if(c == '{' && text.charAt(i + 1) == '{') {
				int closeIndex = getCloseIndex(text, i);
				children.add(text.substring(i, closeIndex + 1));
				i = closeIndex;
			} else {
				children.add(String.valueOf(c));
			}
		}
		return children;
	}
	
	private int getCloseIndex(String text, int index) {
		int balance = 1;
		for (int i = index + 2; i < text.length() - 1; i++) {
			char thisChar = text.charAt(i),
				nextChar = text.charAt(i + 1);
			if(thisChar == '}' && nextChar == '}') {
				balance--;
				i++;
				if(balance == 0) return i;
			}
			if(thisChar == '{' && nextChar == '{'){
				balance++;
				i++;
			}
		}
		return -1;
	}
	
	private String separateChineseCharacters(String text) {
		Pattern p = Pattern.compile("[\u4E00-\u9FCC\u3400-\u4DB5\uFA0E\uFA0F\uFA11\uFA13\uFA14\uFA1F\uFA21\uFA23\uFA24\uFA27-\uFA29]|[\ud840-\ud868][\udc00-\udfff]|\ud869[\udc00-\uded6\udf00-\udfff]|[\ud86a-\ud86c][\udc00-\udfff]|\ud86d[\udc00-\udf34\udf40-\udfff]|\ud86e[\udc00-\udc1d]");
		StringBuilder out = new StringBuilder();
		for(int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if(p.matcher(String.valueOf(c)).matches()) {
				out.append(" " + c + " ");
			} else {
				out.append(c);
			}
		}
		return out.toString().replace("  ", " ").trim();
	}
}
