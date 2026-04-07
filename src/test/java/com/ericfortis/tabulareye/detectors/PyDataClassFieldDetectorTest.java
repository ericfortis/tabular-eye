package com.ericfortis.tabulareye.detectors;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class PyDataClassFieldDetectorTest extends BasePlatformTestCase {
	private PyDataClassFieldDetector detector;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		detector = new PyDataClassFieldDetector();
	}

	private @NonNull List<AlignmentDetector.AlignmentBlock> getBlocks(String content) {
		var file = myFixture.configureByText("test.py", content);
		var doc = myFixture.getDocument(file);
		return detector.findBlocks(file, doc);
	}

	public void testBasicDataClass() {
		var blocks = getBlocks("""
			from dataclasses import dataclass
			
			@dataclass
			class User:
			    name: str
			    age: int
			    email: str = "default@example.com"
			""");
		assertEquals(1, blocks.size());

		var block = blocks.getFirst();
		assertEquals(3, block.size());

		assertEquals("name", block.get(0).key());
		assertEquals("age", block.get(1).key());
		assertEquals("email", block.get(2).key());
	}

	public void testMultipleDataClasses() {
		var blocks = getBlocks("""
			from dataclasses import dataclass
			
			@dataclass
			class Point:
			    x: float
			    y: float
			
			@dataclass
			class Size:
			    width: int
			    height: int
			""");
		assertEquals(2, blocks.size());

		var b0 = blocks.get(0);
		var b1 = blocks.get(1);

		assertEquals(2, b0.size());
		assertEquals("x", b0.get(0).key());
		assertEquals("y", b0.get(1).key());

		assertEquals(2, b1.size());
		assertEquals("width", b1.get(0).key());
		assertEquals("height", b1.get(1).key());
	}

	public void testIgnoresNonDataClass() {
		var blocks = getBlocks("""
			class Normal:
			    x: int
			    y: int
			""");
		assertTrue(blocks.isEmpty());
	}

	public void testIgnoresSingleLineDataClass() {
		var blocks = getBlocks("""
			@dataclass
			class Point: x: int; y: int
			""");
		assertTrue(blocks.isEmpty());
	}

	public void testIgnoresSingleFieldDataClass() {
		var blocks = getBlocks("""
			@dataclass
			class Single:
			    prop: str
			""");
		assertTrue(blocks.isEmpty());
	}

	public void testQualifiedDecorator() {
		var blocks = getBlocks("""
			import dataclasses
			
			@dataclasses.dataclass
			class User:
			    name: str
			    age: int
			""");
		assertEquals(1, blocks.size());
		assertEquals(2, blocks.getFirst().size());
	}
}
