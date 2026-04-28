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
			  d: 4,
			  [5]: 5,
			  ['computed_name']: 6,
			  inline_obj: { x: 1, y: 2 }
			 }
			 """);
		assertEquals(2, blocks.size());

		var b0 = blocks.getFirst();
		var b1 = blocks.getLast();
		assertEquals(2, b0.size());
		assertEquals(5, b1.size());

		assertEquals("'a'", b0.get(0).key());
		assertEquals("b", b0.get(1).key());

		assertEquals("c", b1.get(0).key());
		assertEquals("d", b1.get(1).key());
		assertEquals("[5]", b1.get(2).key());
		assertEquals("['computed_name']", b1.get(3).key());
		assertEquals("inline_obj", b1.get(4).key());

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

	public void testFunctionProps() {
		// com.intellij.lang.javascript.psi.JSFunctionProperty
		var content = """
			 const Obj = {
			 	isFooA(f) { return this.f === 1 },
			 	isFooBarA(f) { return this.f === 2 },
			 	/** @param {number} f */
			 	isBazA(f) { return this.f === 2 },
			 
			 	isFooB(f) { return this.f === 1 },
			 	isFooBarB(f) { return this.f === 2 },
			 }
			 """;
		var blocks = getBlocks(content);
		assertEquals(2, blocks.size());
		
		var b0 = blocks.getFirst();
		var b1 = blocks.getLast();
		assertEquals(3, b0.size());
		assertEquals(2, b1.size());
		
		assertEquals("isFooA", b0.get(0).key());
		assertEquals("isFooBarA", b0.get(1).key());
		assertEquals("isBazA", b0.get(2).key());

		assertEquals("isFooB", b1.get(0).key());
		assertEquals("isFooBarB", b1.get(1).key());

		assertEquals(')', content.charAt(b0.get(0).separatorOffset()));
		assertEquals(')', content.charAt(b1.get(0).separatorOffset()));
	}
}
