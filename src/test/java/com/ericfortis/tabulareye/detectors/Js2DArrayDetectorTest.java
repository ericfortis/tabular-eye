package com.ericfortis.tabulareye.detectors;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class Js2DArrayDetectorTest extends BasePlatformTestCase {
	private Js2DArrayDetector detector;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		detector = new Js2DArrayDetector();
	}

	private @NonNull List<AlignmentDetector.AlignmentBlock> getBlocks(String content) {
		var file = myFixture.configureByText("test.js", content);
		var doc = myFixture.getDocument(file);
		return detector.findBlocks(file, doc);
	}

	public void testBasic2DArray() {
		var blocks = getBlocks("""
			 const data = [
			  ["a", 1],
			  ["bc", 2],
			  ["def", 3]
			 ];
			 """);
		assertEquals(1, blocks.size());

		var block = blocks.getFirst();
		assertEquals(3, block.size());

		assertEquals("\"a\"", block.get(0).key());
		assertEquals("\"bc\"", block.get(1).key());
		assertEquals("\"def\"", block.get(2).key());
	}

	public void testMultiple2DArrays() {
		var blocks = getBlocks("""
			 const a = [
			  [1, 2],
			  [3, 4]
			 ];
			 const b = [
			  ["x", "y"],
			  ["z", "w"]
			 ];
			 """);
		assertEquals(2, blocks.size());

		var b0 = blocks.get(0);
		var b1 = blocks.get(1);

		assertEquals(2, b0.size());
		assertEquals("1", b0.get(0).key());
		assertEquals("3", b0.get(1).key());

		assertEquals(2, b1.size());
		assertEquals("\"x\"", b1.get(0).key());
		assertEquals("\"z\"", b1.get(1).key());
	}

	public void testIgnoresSingleLineArray() {
		var blocks = getBlocks("""
			 const data = [[1, 2], [3, 4]];
			 """);
		assertTrue(blocks.isEmpty());
	}

	public void testIgnores1DArray() {
		var blocks = getBlocks("""
			 const data = [
			  1,
			  2,
			  3
			 ];
			 """);
		assertTrue(blocks.isEmpty());
	}

	public void testIgnoresTooSmallInnerArray() {
		var blocks = getBlocks("""
			 const data = [
			  [1],
			  [2]
			 ];
			 """);
		assertTrue(blocks.isEmpty());
	}
}
