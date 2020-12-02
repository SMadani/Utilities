import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 
 * @author Sina Madani
 */
public class CodeCompressor {

	public static void main(String[] args) throws Throwable {
		var clazz = CodeCompressor.class;
		var filePath = Paths.get(clazz.getResource("").toURI())
			.getParent().resolve("src")
			.resolve(clazz.getSimpleName()+".java");
		
		var input = Files.readString(filePath);
		var comp = new CodeCompressor(input);
		var result = comp.minimize();
		System.out.println(result);
		
		//System.out.println(comp.wordCount());
	}
	
	/**
	 * Class for grouping information about each encountered word in the code.
	 */
	protected class Occurences {
		public final int identifier;
		public int count = 1, firstStartIndex = -1;
		Occurences(int identifier, int startIndex) {
			this(identifier);
			this.firstStartIndex = startIndex;
		}
		Occurences(int identifier) {
			this.identifier = identifier;
		}
	}
	
	protected final String replacementPrefix;
	protected final Pattern wordPattern;
	protected final String code;
	protected Map<String, Occurences> wordsCache;
	
	public CodeCompressor(String code) {
		this(code, "([A-Za-z]+)((?:(?![A-Za-z]).)*?)", "$");
	}
	
	public CodeCompressor(String code, String wordRegex, String replacementPrefix) {
		this.code = code;
		this.wordPattern = Pattern.compile(wordRegex);
		this.replacementPrefix = replacementPrefix;
	}
	
	public String minimize() {
		// We can use the length of the input as a sizing hint
		wordsCache = new HashMap<>(code.length() / 10);
		var minified = new StringBuilder(code);
		var matcher = wordPattern.matcher(minified);
		// We need to keep track of replacements because each time we replace,
		// we're reducing the number of words. However, we need to count replaced instances
		// as words (as if we hadn't replaced them with $).
		for (int replacements = 0, wordNumber = 0; matcher.find(); wordNumber++) {
			int group = 1;
			var word = matcher.group(group);
			var occ = wordsCache.get(word);
			// First encounter
			if (occ == null) {
				wordsCache.put(word, occ = new Occurences(wordNumber, matcher.start(group)));
			}
			else {
				++occ.count;
				// Skip the first time the word occurs so that we know what it is
				if (occ.firstStartIndex == matcher.start(group)) {
					continue;
				}
				var replacement = replacementPrefix + occ.identifier;
				// Don't replace when it would make things longer
				if (replacement.length() < word.length()) {
					minified.replace(matcher.start(group), matcher.end(group), replacement);
					// Since we've changed the string, we need to reset to avoid IndexOutOfBounds
					matcher.reset();
					wordNumber = ++replacements;
				}
			}
		}
		return minified.toString();
	}
	
	/**
	 * Computes the number of times each word was encountered and presents them as
	 * a convenient human-readable string for ease of analysis.
	 * 
	 * @return Each word and the number of occurrences on a separate line, in descending order.
	 * @throws IllegalStateException If {@link #minimize()} has not been called yet.
	 */
	public String wordCount() throws IllegalStateException {
		if (wordsCache == null) {
			throw new IllegalStateException("Should call minimize() first");
		}
		return wordsCache.entrySet().stream()
			.collect(
				Collectors.toMap(
					java.util.Map.Entry::getKey,
					e -> e.getValue().count
				)
			)
			.entrySet().stream()
				.sorted((e1, e2) -> e2.getValue() - e1.getValue())
				.map(Object::toString)
				.collect(Collectors.joining("\n"));
	}
}
