package com.ericfortis.tabulareye.detectors;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class CssPropertyDetectorTest extends BasePlatformTestCase {
	private CssPropertyDetector detector;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		detector = new CssPropertyDetector();
	}

	private @NonNull List<AlignmentDetector.AlignmentBlock> getBlocks(String content) {
		var file = myFixture.configureByText("test.css", content);
		var doc = myFixture.getDocument(file);
		return detector.findBlocks(file, doc);
	}

	public void testBasicCssBlock() {
		var blocks = getBlocks("""
			 .container {
			  color: red;
			  background: blue;
			  font-size: 14px;
			 }
			 """);
		assertEquals(1, blocks.size());

		var block = blocks.getFirst();
		assertEquals(3, block.size());

		assertEquals("color", block.get(0).key());
		assertEquals("background", block.get(1).key());
		assertEquals("font-size", block.get(2).key());
	}

	public void testMultipleCssBlocks() {
		var blocks = getBlocks("""
			 .a {
			  top: 0;
			  left: 0;
			 }
			 .b {
			  width: 100px;
			  height: 100px;
			 }
			 """);
		assertEquals(2, blocks.size());

		var b0 = blocks.get(0);
		var b1 = blocks.get(1);

		assertEquals(2, b0.size());
		assertEquals("top", b0.get(0).key());
		assertEquals("left", b0.get(1).key());

		assertEquals(2, b1.size());
		assertEquals("width", b1.get(0).key());
		assertEquals("height", b1.get(1).key());
	}

	public void testIgnoresSingleLineBlock() {
		var blocks = getBlocks("""
			 .inline { color: red; background: blue; }
			 """);
		assertTrue(blocks.isEmpty());
	}

	public void testIgnoresSinglePropertyBlock() {
		var blocks = getBlocks("""
			 .single {
			  color: red;
			 }
			 """);
		assertTrue(blocks.isEmpty());
	}
}
