package com.ericfortis.tabulareye.detectors;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class PyKeywordArgsDetectorTest extends BasePlatformTestCase {
	private PyKeywordArgsDetector detector;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		detector = new PyKeywordArgsDetector();
	}

	private @NonNull List<AlignmentDetector.AlignmentBlock> getBlocks(String content) {
		var file = myFixture.configureByText("test.py", content);
		var doc = myFixture.getDocument(file);
		return detector.findBlocks(file, doc);
	}

	public void testBasicKeywordArgs() {
		var blocks = getBlocks("""
			 do_something(
			     name="Alice",
			     age=30,
			     city="New York"
			 )
			 """);
		assertEquals(1, blocks.size());

		var block = blocks.getFirst();
		assertEquals(3, block.size());
		assertEquals("name", block.get(0).key());
		assertEquals("age", block.get(1).key());
		assertEquals("city", block.get(2).key());
	}

	public void testMixedArgs() {
		var blocks = getBlocks("""
			 do_something(
			     "positional",
			     name="Alice",
			     age=30
			 )
			 """);
		assertEquals(1, blocks.size());

		var block = blocks.getFirst();
		assertEquals(2, block.size());
		assertEquals("name", block.get(0).key());
		assertEquals("age", block.get(1).key());
	}

	public void testIgnoresSingleLineCall() {
		var blocks = getBlocks("""
			 do_something(name="Alice", age=30)
			 """);
		assertTrue(blocks.isEmpty());
	}

	public void testSingleKeywordArg() {
		var blocks = getBlocks("""
			 do_something(
			     name="Alice"
			 )
			 """);
		assertTrue(blocks.isEmpty());
	}

	public void testNestedCall() {
		var blocks = getBlocks("""
			 outer(
			     arg1=inner(
			         nested_arg1=1,
			         nested_arg2=2
			     ),
			     arg2=3
			 )
			 """);
		assertEquals(2, blocks.size());
	}
}
