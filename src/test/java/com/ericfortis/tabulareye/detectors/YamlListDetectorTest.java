package com.ericfortis.tabulareye.detectors;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class YamlListDetectorTest extends BasePlatformTestCase {
	private YamlListDetector detector;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		detector = new YamlListDetector();
	}

	private @NonNull List<AlignmentDetector.AlignmentBlock> getBlocks(String content) {
		var file = myFixture.configureByText("test.yml", content);
		var doc = myFixture.getDocument(file);
		return detector.findBlocks(file, doc);
	}

	public void testBasicYamlList() {
		var blocks = getBlocks("""
			 list:
			  - item1
			  - item2
			  - item3
			 """);
		assertEquals(3, blocks.size());

		for (var b : blocks) {
			assertEquals(2, b.size());
			assertEquals("-", b.get(0).key());
			assertEquals(" ", b.get(1).key());
		}
	}

	public void testIgnoresInlineList() {
		var blocks = getBlocks("""
			 list: [a, b, c]
			 """);
		assertTrue(blocks.isEmpty());
	}

	public void testIgnoresListWithMultiLineItems() {
		var blocks = getBlocks("""
			 list:
			  -
			    item1
			  - item2
			 """);
		// In this case, hyphen and value are on different lines for the first item
		// YamlListDetector checks doc.getLineNumber(hyphenStart) == doc.getLineNumber(contentStart)
		assertEquals(1, blocks.size());
	}
}
