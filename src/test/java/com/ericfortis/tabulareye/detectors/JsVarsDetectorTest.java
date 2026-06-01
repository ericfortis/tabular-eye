package com.ericfortis.tabulareye.detectors;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class JsVarsDetectorTest extends BasePlatformTestCase {
	private JsVarsDetector detector;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		detector = new JsVarsDetector();
	}

	private @NonNull List<AlignmentDetector.AlignmentBlock> getBlocks(String content) {
		var file = myFixture.configureByText("test.js", content);
		var doc = myFixture.getDocument(file);
		return detector.findBlocks(file, doc);
	}

	public void testGroupsConsecutiveConsts() {
		var blocks = getBlocks("""
			 const PORT = 443
			 let TIMEOUT_SEC = 2
			 var FAMILY = 4
			 """);
		assertEquals(1, blocks.size());
		var b = blocks.get(0);
		assertEquals(3, b.size());
		assertEquals("const PORT ", b.get(0).key());
		assertEquals("let TIMEOUT_SEC ", b.get(1).key());
		assertEquals("var FAMILY ", b.get(2).key());
	}

	public void testGroupsTwoConsts() {
		var blocks = getBlocks("""
			 const a = 1
			 const b = 2
			 """);
		assertEquals(1, blocks.size());
		assertEquals(2, blocks.get(0).size());
		assertEquals("const a ", blocks.get(0).get(0).key());
		assertEquals("const b ", blocks.get(0).get(1).key());
	}

	public void testIgnoresSingleConst() {
		var blocks = getBlocks("""
			 const x = 1
			 """);
		assertTrue(blocks.isEmpty());
	}

	public void testSeparatorOffset() {
		var blocks = getBlocks("""
			 const a = 1
			 const bb = 2
			 """);
		assertEquals(1, blocks.size());
		var b = blocks.get(0);
		assertEquals(2, b.size());

		int sep0 = b.get(0).separatorOffset();
		int sep1 = b.get(1).separatorOffset();

		assertTrue(sep0 > 0);
		assertTrue(sep1 > sep0);
	}

	public void testWithIndentation() {
		var blocks = getBlocks("""
			   const a = 1
			   const b = 2
			 """);
		assertEquals(1, blocks.size());
		assertEquals("const a ", blocks.get(0).get(0).key());
		assertEquals("const b ", blocks.get(0).get(1).key());
	}

	public void testIgnoresGapBetweenGroups() {
		var blocks = getBlocks("""
			 const PORT = 443
			 const TIMEOUT_SEC = 2
			 
			 const HOST = 'localhost'
			 """);
		assertEquals(1, blocks.size());
		assertEquals(2, blocks.get(0).size());
	}

	public void testIgnoresSingleImportBefore() {
		var blocks = getBlocks("""
			 import { x } from 'y'
			 const z = 1;
			 """);
		assertTrue(blocks.isEmpty());
	}

	public void testSkipsObjectDestructuring() {
		var blocks = getBlocks("""
			 const PORT = 443
			 const { foo, bar } = obj
			 const TIMEOUT_SEC = 2
			 """);
		assertEquals(1, blocks.size());
		assertEquals(2, blocks.get(0).size());
		assertEquals("const PORT ", blocks.get(0).get(0).key());
		assertEquals("const TIMEOUT_SEC ", blocks.get(0).get(1).key());
	}

	public void testSkipsArrayDestructuring() {
		var blocks = getBlocks("""
			 const PORT = 443
			 const [a, b] = arr
			 const TIMEOUT_SEC = 2
			 """);
		assertEquals(1, blocks.size());
		assertEquals(2, blocks.get(0).size());
		assertEquals("const PORT ", blocks.get(0).get(0).key());
		assertEquals("const TIMEOUT_SEC ", blocks.get(0).get(1).key());
	}

	public void testSkipsDestructuringFollowedBySimple() {
		var blocks = getBlocks("""
			 const { foo, bar } = obj
			 const PORT = 443
			 const TIMEOUT_SEC = 2
			 """);
		assertEquals(1, blocks.size());
		assertEquals(2, blocks.get(0).size());
		assertEquals("const PORT ", blocks.get(0).get(0).key());
		assertEquals("const TIMEOUT_SEC ", blocks.get(0).get(1).key());
	}

	public void testDestructuringAloneDoesNotGroup() {
		var blocks = getBlocks("""
			 const { foo, bar } = obj
			 const PORT = 443
			 """);
		assertTrue(blocks.isEmpty());
	}

	public void testSkipsForLoopVar() {
		var blocks = getBlocks("""
			 const a = 1
			 const b = 2
			 for (let i = 0; i < 10; i++) {}
			 """);
		assertEquals(1, blocks.size());
		assertEquals(2, blocks.get(0).size());
	}

	public void testSkipsForInLoopVar() {
		var blocks = getBlocks("""
			 const a = 1
			 const b = 2
			 for (const key in obj) {}
			 """);
		assertEquals(1, blocks.size());
		assertEquals(2, blocks.get(0).size());
	}

	public void testForLoopVarDoesNotPolluteGroup() {
		var blocks = getBlocks("""
			 const clips = []
			 for (let i = 0; i < arr.length; i++) {
			   const item = arr[i]
			   const result = process(item)
			 }
			 """);
		assertEquals(1, blocks.size());
		assertEquals(2, blocks.get(0).size());
		assertEquals("const item ", blocks.get(0).get(0).key());
		assertEquals("const result ", blocks.get(0).get(1).key());
	}
}
