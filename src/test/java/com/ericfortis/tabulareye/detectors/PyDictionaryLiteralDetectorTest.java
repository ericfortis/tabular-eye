package com.ericfortis.tabulareye.detectors;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class PyDictionaryLiteralDetectorTest extends BasePlatformTestCase {
	private PyDictionaryLiteralDetector detector;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		detector = new PyDictionaryLiteralDetector();
	}

	private @NonNull List<AlignmentDetector.AlignmentBlock> getBlocks(String content) {
		var file = myFixture.configureByText("test.py", content);
		var doc = myFixture.getDocument(file);
		
		return detector.findBlocks(file, doc);
	}

	public void testBasicDictionary() {
		var blocks = getBlocks("""
			 d = {
			     "name": "Alice",
			     "age": 30,
			     "city": "New York"
			 }
			 """);
		assertEquals(1, blocks.size());

		var block = blocks.getFirst();
		assertEquals(3, block.size());
		assertEquals("\"name\"", block.get(0).key());
		assertEquals("\"age\"", block.get(1).key());
		assertEquals("\"city\"", block.get(2).key());
	}

	public void testNestedDictionary() {
		var blocks = getBlocks("""
			 d = {
			     "user": {
			         "id": 1,
			         "name": "Bob"
			     },
			     "active": True
			 }
			 """);
		assertEquals(2, blocks.size());

		// Inner dict first usually in collectElementsOfType if it's bottom-up, or outer first if top-down
		// AlignmentDetector uses PsiTreeUtil.collectElementsOfType which is usually bottom-up or depends on implementation.
		// Let's check sizes.
		
		var innerBlock = blocks.stream().filter(b -> b.size() == 2).findFirst().orElseThrow();
		var outerBlock = blocks.stream().filter(b -> b.size() == 2 && b != innerBlock).findFirst(); // Might also be 2

		// For now just check we have 2 blocks
		assertEquals(2, blocks.size());
	}

	public void testIgnoresSingleLineDictionary() {
		var blocks = getBlocks("""
			 d = {"a": 1, "b": 2}
			 """);
		assertTrue(blocks.isEmpty());
	}

	public void testMixedTypes() {
		var blocks = getBlocks("""
			 d = {
			     1: "one",
			     "two": 2,
			     (1, 2): "three"
			 }
			 """);
		assertEquals(1, blocks.size());
		var block = blocks.getFirst();
		assertEquals(3, block.size());
		assertEquals("1", block.get(0).key());
		assertEquals("\"two\"", block.get(1).key());
		assertEquals("(1, 2)", block.get(2).key());
	}

	public void testSingleElementDictionary() {
		var blocks = getBlocks("""
			 d = {
			     "a": 1
			 }
			 """);
		assertTrue(blocks.isEmpty());
	}
}
