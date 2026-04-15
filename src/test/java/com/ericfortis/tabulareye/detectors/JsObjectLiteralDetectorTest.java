package com.ericfortis.tabulareye.detectors;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class JsObjectLiteralDetectorTest extends BasePlatformTestCase {
	private JsObjectLiteralDetector detector;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		detector = new JsObjectLiteralDetector();
	}

	private @NonNull List<AlignmentDetector.AlignmentBlock> getBlocks(String content) {
		var file = myFixture.configureByText("test.js", content);
		var doc = myFixture.getDocument(file);
		return detector.findBlocks(file, doc);
	}

	public void testMultipleBlocks() {
		var blocks = getBlocks("""
			 const block0 = {
			  'a': 1,
			  b: 2
			 }
			 const block1 = {
			  c: 3,
			  /** JSDocComments are ignored */
			  d: 4
			 }
			 """);
		assertEquals(2, blocks.size());

		var b0 = blocks.getFirst();
		var b1 = blocks.getLast();
		assertEquals(2, b0.size());
		assertEquals(2, b1.size());

		assertEquals("'a'", b0.get(0).key());
		assertEquals("b", b0.get(1).key());
		assertEquals("c", b1.get(0).key());
		assertEquals("d", b1.get(1).key());

		// a, which is in quotes
		assertEquals(18, b0.get(0).keyOffset());
		assertEquals(18 + 2 + 1, b0.get(0).separatorOffset());

		// b
		assertEquals(27, b0.get(1).keyOffset());
		assertEquals(27 + 1, b0.get(1).separatorOffset());
	}


	public void testIgnoresShorthandProps() {
		var blocks = getBlocks("""
			 const foo = "bar";
			 const first = {
			  foo,
			  baz: 123,
			  qux: "quux"
			 };
			 """);
		var b = blocks.getFirst();
		assertEquals(1, blocks.size());
		assertEquals(2, b.size()); // 'foo' is shorthanded, should be ignored
		assertEquals("baz", b.get(0).key());
		assertEquals("qux", b.get(1).key());
	}

	public void testIgnoresInlineObjects() {
		var blocks = getBlocks("""
			 const first = { foo: "bar", baz: 123 };
			 """);
		assertTrue(blocks.isEmpty());
	}

	public void testFindsNestedObjects() {
		var blocks = getBlocks("""
			 const obj = {
			   foo: {
			     inner: "val"
			   },
			   baz: 123
			 };
			 """);
		assertEquals(1, blocks.size());
		var b = blocks.getFirst();
		assertEquals("foo", b.get(0).key());
		assertEquals("baz", b.get(1).key());
	}
}
