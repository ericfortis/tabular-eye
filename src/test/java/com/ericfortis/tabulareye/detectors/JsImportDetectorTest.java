package com.ericfortis.tabulareye.detectors;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class JsImportDetectorTest extends BasePlatformTestCase {
	private JsImportDetector detector;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		detector = new JsImportDetector();
	}

	private @NonNull List<AlignmentDetector.AlignmentBlock> getBlocks(String content) {
		var file = myFixture.configureByText("test.js", content);
		var doc = myFixture.getDocument(file);
		return detector.findBlocks(file, doc);
	}

	public void testGroupsConsecutiveImports() {
		var blocks = getBlocks("""
			import { pathToFileURL } from 'node:url'
			import { resolve, join } from 'node:path'
			import { parseArgs } from 'node:util'

			import { App } from '../../index.js'
			import { config } from './config.js'
			""");
		assertEquals(2, blocks.size());

		var b0 = blocks.get(0);
		assertEquals(3, b0.size());
		assertEquals("import { pathToFileURL }", b0.get(0).key());
		assertEquals("import { resolve, join }", b0.get(1).key());
		assertEquals("import { parseArgs }", b0.get(2).key());

		var b1 = blocks.get(1);
		assertEquals(2, b1.size());
		assertEquals("import { App }", b1.get(0).key());
		assertEquals("import { config }", b1.get(1).key());
	}

	public void testIgnoresSingleImport() {
		var blocks = getBlocks("""
			import { x } from 'y'
			const z = 1;
			""");
		assertTrue(blocks.isEmpty());
	}

	public void testSeparatorOffsetBeforeFrom() {
		var blocks = getBlocks("""
			import { a } from 'x'
			import { bb } from 'y'
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
			  import { a } from 'x'
			  import { bb } from 'y'
			""");
		assertEquals(1, blocks.size());
		assertEquals("import { a }", blocks.get(0).get(0).key());
		assertEquals("import { bb }", blocks.get(0).get(1).key());
	}

	public void testDefaultImport() {
		var blocks = getBlocks("""
			import foo from 'a'
			import bar from 'b'
			""");
		assertEquals(1, blocks.size());
		assertEquals(2, blocks.get(0).size());
		assertEquals("import foo", blocks.get(0).get(0).key());
		assertEquals("import bar", blocks.get(0).get(1).key());
	}

	public void testNamespaceImport() {
		var blocks = getBlocks("""
			import * as foo from 'a'
			import * as bar from 'b'
			""");
		assertEquals(1, blocks.size());
		assertEquals(2, blocks.get(0).size());
		assertEquals("import * as foo", blocks.get(0).get(0).key());
		assertEquals("import * as bar", blocks.get(0).get(1).key());
	}
}
