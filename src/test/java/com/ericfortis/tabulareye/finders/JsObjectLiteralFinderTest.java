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

	private @NonNull List<AlignmentFinder.AlignmentGroup> getGroups(String content) {
		var file = myFixture.configureByText("test.js", content);
		var doc = myFixture.getDocument(file);
		return finder.findGroups(file, doc);
	}

	public void testFindGroups_SimpleObject() {
		var groups = getGroups("""
			 const obj = {
			   foo: "bar",
			   baz: 123
			 };
			 """);
		assertEquals(1, groups.size());
		var group = groups.getFirst();
		assertEquals(2, group.props().size());
		assertEquals("foo", group.props().get(0).keyText());
		assertEquals("baz", group.props().get(1).keyText());
	}

	public void testFindGroups_IgnoreShorthand() {
		var groups = getGroups("""
			 const foo = "bar";
			 const obj = {
			  foo,
			  baz: 123,
			  qux: "quux"
			 };
			 """);

		assertEquals(1, groups.size());
		var group = groups.getFirst();
		assertEquals(2, group.props().size()); // 'foo' is shorthanded, should be ignored
		assertEquals("baz", group.props().get(0).keyText());
		assertEquals("qux", group.props().get(1).keyText());
	}

	public void testFindGroups_IgnoresSingleLineObject() {
		var groups = getGroups("""
			 const obj = { foo: "bar", baz: 123 };
			 """);
		assertTrue(groups.isEmpty());
	}

	public void testFindGroups_DeeplyNested() {
		var groups = getGroups("""
			 const obj = {
			   foo: {
			     inner: "val"
			   },
			   baz: 123
			 };
			 """);

		// It should find groups for the outer object and potentially the inner one if it was multiline and has > 1 prop
		// Here inner has only 1 prop, so it shouldn't be a valid group (props.size() > 1)
		assertEquals(1, groups.size());
		assertEquals("foo", groups.getFirst().props().get(0).keyText());
		assertEquals("baz", groups.getFirst().props().get(1).keyText());
	}

	public void testFindGroups_MultipleGroups() {
		var groups = getGroups("""
			 const obj1 = {
			   a: 1,
			   b: 2
			 };
			 const obj2 = {
			   c: 3,
			   d: 4
			 };
			 """);
		assertEquals(2, groups.size());
	}
}
