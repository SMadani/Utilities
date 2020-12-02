import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 
 * @author Sina Madani
 */
public class CodeCompressorTests {
	
	protected void testMinimize(String input, String expected) {
		var minimizer = new CodeCompressor(input);
		var result = minimizer.minimize();
		assertEquals(expected, result);
	}
	
	@Test
	public void testSmallExample() {
		testMinimize(
			"you say yes, I say no you say stop and I say go go go",
			"you say yes, I $1 no $0 $1 stop and I $1 go go go"
		);
	}

	@Test
	public void testCorrectWordIndex() {
		testMinimize(
			"you say jah, I say nicht you say ACHTUNG & I say schnell schnell schnell!",
			"you say jah, I $1 nicht $0 $1 ACHTUNG & I $1 schnell $12 $12!"
		);
	}
	
}
