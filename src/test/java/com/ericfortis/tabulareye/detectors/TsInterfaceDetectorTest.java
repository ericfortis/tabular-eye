package com.ericfortis.tabulareye.detectors;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class TsInterfaceDetectorTest extends BasePlatformTestCase {
	private TsInterfaceDetector detector;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		detector = new TsInterfaceDetector();
	}

	private @NonNull List<AlignmentDetector.AlignmentBlock> getBlocks(String content) {
		var file = myFixture.configureByText("test.ts", content);
		var doc = myFixture.getDocument(file);
		return detector.findBlocks(file, doc);
	}

	public void testBasicInterface() {
		var blocks = getBlocks("""
			 interface User {
			  age: number;
			  name: string;
			  /** JSDocComments are ignored */
			  email: string;
			 }
			 """);
		assertEquals(1, blocks.size());

		var block = blocks.getFirst();
		assertEquals(3, block.size());

		assertEquals("age", block.get(0).key());
		assertEquals("name", block.get(1).key());
		assertEquals("email", block.get(2).key());
	}

	public void testMultipleInterfaces() {
		var blocks = getBlocks("""
			 interface Point {
			  x: number;
			  y: number;
			 }
			 
			 interface Size {
			  width: number;
			  height: number;
			 }
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

	public void testIgnoresSingleLineInterface() {
		var blocks = getBlocks("""
			 interface Point { x: number; y: number; }
			 """);
		assertTrue(blocks.isEmpty());
	}

	public void testIgnoresSinglePropertyInterface() {
		var blocks = getBlocks("""
			 interface Single {
			  prop: string;
			 }
			 """);
		assertTrue(blocks.isEmpty());
	}

	public void testTypeAlias() {
		var blocks = getBlocks("""
			 type Config = {
			  enabled: boolean;
			  timeout: number;
			 };
			 """);
		assertEquals(1, blocks.size());
		var block = blocks.getFirst();
		assertEquals(2, block.size());
		assertEquals("enabled", block.get(0).key());
		assertEquals("timeout", block.get(1).key());
	}

	public void testNestedObjectType() {
		var blocks = getBlocks("""
			 interface Complex {
			  id: string;
			  metadata: {
			    created: string;
			    updated: string;
			  };
			 }
			 """);
		assertEquals(2, blocks.size());

		var mainBlock = blocks.stream().filter(b -> b.size() == 2 && b.get(0).key().equals("id")).findFirst();
		var nestedBlock = blocks.stream().filter(b -> b.size() == 2 && b.get(0).key().equals("created")).findFirst();
		assertTrue("Main interface block not found", mainBlock.isPresent());
		assertTrue("Nested object type block not found", nestedBlock.isPresent());
	}
}