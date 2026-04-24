package com.ericfortis.tabulareye.detectors;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class JsonAlignmentDetectorTest extends BasePlatformTestCase {
	private JsonAlignmentDetector detector;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		detector = new JsonAlignmentDetector();
	}

	private @NonNull List<AlignmentDetector.AlignmentBlock> getBlocks(String content) {
		var file = myFixture.configureByText("test.json", content);
		var doc = myFixture.getDocument(file);
		return detector.findBlocks(file, doc);
	}

	public void testBasicJsonObject() {
		var blocks = getBlocks("""
			 {
			  "name": "John",
			  "age": 30,
			  "city": "New York"
			 }
			 """);
		assertEquals(1, blocks.size());

		var block = blocks.getFirst();
		assertEquals(3, block.size());

		assertEquals("name", block.get(0).key());
		assertEquals("age", block.get(1).key());
		assertEquals("city", block.get(2).key());
	}

	public void testMultipleJsonObjects() {
		var blocks = getBlocks("""
			 {
			  "person": {
			    "firstName": "John",
			    "lastName": "Doe"
			  },
			  "address": {
			    "street": "123 Main St",
			    "zip": "10001"
			  }
			 }
			 """);
		assertEquals(3, blocks.size()); // main object + 2 nested objects

		// Check if we can find the nested blocks
		assertTrue(blocks.stream().anyMatch(b -> b.size() == 2 && b.get(0).key().equals("firstName")));
		assertTrue(blocks.stream().anyMatch(b -> b.size() == 2 && b.get(0).key().equals("street")));
		assertTrue(blocks.stream().anyMatch(b -> b.size() == 2 && b.get(0).key().equals("person")));
	}

	public void testIgnoresSingleLineJson() {
		var blocks = getBlocks("""
			 {"a": 1, "b": 2}
			 """);
		assertTrue(blocks.isEmpty());
	}

	public void testIgnoresSinglePropertyJson() {
		var blocks = getBlocks("""
			 {
			  "only": "one"
			 }
			 """);
		assertTrue(blocks.isEmpty());
	}
}
