package com.ericfortis.tabulareye.detectors;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class YamlObjectDetectorTest extends BasePlatformTestCase {
	private YamlObjectDetector detector;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		detector = new YamlObjectDetector();
	}

	private @NonNull List<AlignmentDetector.AlignmentBlock> getBlocks(String content) {
		var file = myFixture.configureByText("test.yml", content);
		var doc = myFixture.getDocument(file);
		return detector.findBlocks(file, doc);
	}

	public void testBasicYamlMapping() {
		var blocks = getBlocks("""
			 person:
			  name: John
			  age: 30
			  city: New York
			 """);
		assertEquals(1, blocks.size());

		var block = blocks.getFirst();
		assertEquals(3, block.size());

		assertEquals("name", block.get(0).key());
		assertEquals("age", block.get(1).key());
		assertEquals("city", block.get(2).key());
	}

	public void testNestedYamlMappings() {
		var blocks = getBlocks("""
			 person:
			  details:
			    firstName: John
			    lastName: Doe
			  address:
			    street: 123 Main St
			    zip: 10001
			 """);
		assertEquals(3, blocks.size()); // main + 2 nested

		assertTrue(blocks.stream().anyMatch(b -> b.size() == 2 && b.get(0).key().equals("firstName")));
		assertTrue(blocks.stream().anyMatch(b -> b.size() == 2 && b.get(0).key().equals("street")));
		assertTrue(blocks.stream().anyMatch(b -> b.size() == 2 && b.get(0).key().equals("details")));
	}

	public void testIgnoresSingleLineMapping() {
		var blocks = getBlocks("""
			 person: { name: John, age: 30 }
			 """);
		assertTrue(blocks.isEmpty());
	}

	public void testIgnoresSinglePropertyMapping() {
		var blocks = getBlocks("""
			 person:
			  name: John
			 """);
		assertTrue(blocks.isEmpty());
	}
}
