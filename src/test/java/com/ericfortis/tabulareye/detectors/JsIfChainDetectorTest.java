package com.ericfortis.tabulareye.detectors;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class JsIfChainDetectorTest extends BasePlatformTestCase {
	private JsIfChainDetector detector;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		detector = new JsIfChainDetector();
	}

	private @NonNull List<AlignmentDetector.AlignmentBlock> getBlocks(String content) {
		var file = myFixture.configureByText("test.js", content);
		var doc = myFixture.getDocument(file);
		return detector.findBlocks(file, doc);
	}

	public void testIfOnlyChain() {
		var blocks = getBlocks("""
			if (opts.foo) args.foo += 1
			if (opts.fooBar) args.fooBar += 1
			""");
		assertEquals(2, blocks.size());

		// Keyword block: both have conditions, aligns "if" widths
		var kwBlock = blocks.get(0);
		assertEquals(2, kwBlock.size());
		assertEquals("if", kwBlock.get(0).key());
		assertEquals("if", kwBlock.get(1).key());

		// Body block: prepends "else " on "if" lines to compensate for keyword spacer
		var bodyBlock = blocks.get(1);
		assertEquals(2, bodyBlock.size());
		assertEquals("else if (opts.foo) ", bodyBlock.get(0).key());
		assertEquals("else if (opts.fooBar) ", bodyBlock.get(1).key());
	}

	public void testIfElseIfElseChain() {
		var blocks = getBlocks("""
			if (opts.foo) args.foo += 1
			else if (opts.bar) args.bar += 2
			else args.baz = 3
			""");
		assertEquals(2, blocks.size());

		// Keyword block: only "if" and "else if" (lines with conditions)
		var kwBlock = blocks.get(0);
		assertEquals(2, kwBlock.size());
		assertEquals("if", kwBlock.get(0).key());
		assertEquals("else if", kwBlock.get(1).key());

		// Body block: all 3 lines contribute; "if" line gets "else " prepended
		var bodyBlock = blocks.get(1);
		assertEquals(3, bodyBlock.size());
		assertEquals("else if (opts.foo) ", bodyBlock.get(0).key());
		assertEquals("else if (opts.bar) ", bodyBlock.get(1).key());
		assertEquals("else ", bodyBlock.get(2).key());
	}

	public void testConsoleLogChain() {
		var blocks = getBlocks("""
			if (opts.foo) console.log(args.foo)
			else if (opts.bar) console.log(args.bar)
			else if (opts.baz) console.log(args.baz)
			""");
		assertEquals(2, blocks.size());

		var kwBlock = blocks.get(0);
		assertEquals(3, kwBlock.size());
		assertEquals("if", kwBlock.get(0).key());
		assertEquals("else if", kwBlock.get(1).key());
		assertEquals("else if", kwBlock.get(2).key());

		var bodyBlock = blocks.get(1);
		assertEquals(3, bodyBlock.size());
		assertEquals("else if (opts.foo) ", bodyBlock.get(0).key());
		assertEquals("else if (opts.bar) ", bodyBlock.get(1).key());
		assertEquals("else if (opts.baz) ", bodyBlock.get(2).key());
	}

	public void testIgnoresSingleConditional() {
		var blocks = getBlocks("""
			if (opts.foo) args.foo += 1
			const x = 1;
			""");
		assertTrue(blocks.isEmpty());
	}

	public void testMultilineBlockBreaksChain() {
		var blocks = getBlocks("""
			if (opts.foo) console.log(args.foo)
			else if (opts.bar) console.log(args.bar)
			else if (opts.baz) console.log(args.baz)
			else if (opts.config) {
				console.error('')
				process.exit(1)
			}
			""");
		// First 3 lines form the chain; 4th line (with {) breaks it
		assertEquals(2, blocks.size());

		var kwBlock = blocks.get(0);
		assertEquals(3, kwBlock.size());
		assertEquals(3, blocks.get(1).size());
	}

	public void testBlankLineBreaksChain() {
		var blocks = getBlocks("""
			if (opts.foo) args.foo += 1
			if (opts.bar) args.bar += 2

			if (opts.baz) args.baz += 3
			if (opts.qux) args.qux += 4
			""");
		// Two separate chains, each with 2 members → 4 blocks total
		assertEquals(4, blocks.size());

		// First chain: keyword block (0) + body block (1)
		assertEquals("else if (opts.foo) ", blocks.get(1).get(0).key());
		assertEquals("else if (opts.bar) ", blocks.get(1).get(1).key());

		// Second chain: keyword block (2) + body block (3)
		assertEquals("else if (opts.baz) ", blocks.get(3).get(0).key());
		assertEquals("else if (opts.qux) ", blocks.get(3).get(1).key());
	}

	public void testIgnoresElseOnly() {
		var blocks = getBlocks("""
			if (opts.foo) args.foo += 1
			else
			""");
		// The "else" with no body has empty body.trim(), so it ends the chain
		assertTrue(blocks.isEmpty());
	}

	public void testBodySeparatorOffset() {
		var blocks = getBlocks("""
			if (opts.foo) args.foo += 1
			if (opts.x) args.y += 2
			""");
		var bodyBlock = blocks.get(1);
		int sep0 = bodyBlock.get(0).separatorOffset();
		int sep1 = bodyBlock.get(1).separatorOffset();
		assertTrue(sep0 > 0);
		assertTrue(sep1 > sep0);
	}

	public void testKeywordSeparatorOffset() {
		var blocks = getBlocks("""
			if (opts.foo) args.foo += 1
			else if (opts.bar) args.bar += 2
			""");
		var kwBlock = blocks.get(0);
		int kw0Sep = kwBlock.get(0).separatorOffset(); // last char of "if"
		int kw1Sep = kwBlock.get(1).separatorOffset(); // last char of "else if"
		assertEquals(kw0Sep, kw0Sep);
		assertEquals(kw1Sep, kw1Sep);
		assertTrue(kw1Sep > kw0Sep);
	}

	public void testKeywordOffset() {
		var blocks = getBlocks("""
			if (opts.foo) args.foo += 1
			else if (opts.bar) args.bar += 2
			""");
		var kwBlock = blocks.get(0);
		// "if" starts at offset 0
		assertEquals(0, kwBlock.get(0).keyOffset());
		// "else if" starts at offset 26 (after "if (opts.foo) args.foo += 1\n")
		assertTrue(kwBlock.get(1).keyOffset() > 0);
	}

	public void testNestedParenInCondition() {
		var blocks = getBlocks("""
			if (v === undefined) continue
			else if (k === 'ref') v.elem = elem
			else if (k === 'style') Object.assign(elem.style, v)
			else if (k.startsWith('on')) elem.addEventListener(...[v].flat())
			else if (k in elem) elem[k] = v
			else elem.setAttribute(k, v)
			""");
		assertEquals(2, blocks.size());

		// 5 lines have conditions - first line is bare "if", rest are "else if"
		var kwBlock = blocks.get(0);
		assertEquals(5, kwBlock.size());

		// Body block: all 6 lines
		var bodyBlock = blocks.get(1);
		assertEquals(6, bodyBlock.size());
		// "if" line gets "else " prepended in body key
		assertEquals("else if (v === undefined) ", bodyBlock.get(0).key());
		assertEquals("else if (k === 'ref') ", bodyBlock.get(1).key());
		assertEquals("else if (k === 'style') ", bodyBlock.get(2).key());
		// Nested paren in condition: k.startsWith('on')
		assertEquals("else if (k.startsWith('on')) ", bodyBlock.get(3).key());
		assertEquals("else if (k in elem) ", bodyBlock.get(4).key());
		assertEquals("else ", bodyBlock.get(5).key());
	}
}
