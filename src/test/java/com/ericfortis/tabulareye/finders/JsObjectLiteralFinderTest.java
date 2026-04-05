package com.ericfortis.tabulareye.finders;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class JsObjectLiteralFinderTest extends BasePlatformTestCase {
	private JsObjectLiteralFinder finder;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		finder = new JsObjectLiteralFinder();
	}

	private @NonNull List<AlignmentFinder.AlignmentBlock> getBlocks(String content) {
		var file = myFixture.configureByText("test.js", content);
		var doc = myFixture.getDocument(file);
		return finder.findBlocks(file, doc);
	}

	public void testMultipleBlocks() {
		var blocks = getBlocks("""
			 const block0 = {
			  a: 1,
			  b: 2
			 }
			 const block1 = {
			  c: 3,
			  d: 4
			 }
			 """);
		assertEquals(2, blocks.size());
		
		var block0 = blocks.getFirst();
		assertEquals("a", block0.props().getFirst().key());
		assertEquals("b", block0.props().getLast().key());
		
		var block1 = blocks.getLast();
		assertEquals("c", block1.props().getFirst().key());
		assertEquals("d", block1.props().getLast().key());
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
		assertEquals(2, b.props().size()); // 'foo' is shorthanded, should be ignored
		assertEquals("baz", b.props().get(0).key());
		assertEquals("qux", b.props().get(1).key());
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
		// It should find groups for the outer object and potentially the inner one if it was multiline and has > 1 prop
		// Here inner has only 1 prop, so it shouldn't be a valid group (props.size() > 1)
		assertEquals(1, blocks.size());
		assertEquals("foo", blocks.getFirst().props().get(0).key());
		assertEquals("baz", blocks.getFirst().props().get(1).key());
	}


}
