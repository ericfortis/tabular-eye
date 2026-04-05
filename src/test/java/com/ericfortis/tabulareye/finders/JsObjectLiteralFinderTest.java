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

	public void testFindGroups_MultipleGroups() {
		var blocks = getBlocks("""
			 const first = {
			   a: 1,
			   b: 2
			 };
			 const second = {
			   c: 3,
			   d: 4
			 };
			 """);
		assertEquals(2, blocks.size());
	}

	public void testFindGroups_SimpleObject() {
		var blocks = getBlocks("""
			 const first = {
			   foo: "bar",
			   baz: 123
			 };
			 """);
		var b = blocks.getFirst();
		assertEquals(1, blocks.size());
		assertEquals(2, b.props().size());
		assertEquals("foo", b.props().get(0).keyText());
		assertEquals("baz", b.props().get(1).keyText());
	}

	public void testFindGroups_IgnoreShorthand() {
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
		assertEquals("baz", b.props().get(0).keyText());
		assertEquals("qux", b.props().get(1).keyText());
	}

	public void testFindGroups_IgnoresSingleLineObject() {
		var blocks = getBlocks("""
			 const first = { foo: "bar", baz: 123 };
			 """);
		assertTrue(blocks.isEmpty());
	}

	public void testFindGroups_DeeplyNested() {
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
		assertEquals("foo", blocks.getFirst().props().get(0).keyText());
		assertEquals("baz", blocks.getFirst().props().get(1).keyText());
	}


}
