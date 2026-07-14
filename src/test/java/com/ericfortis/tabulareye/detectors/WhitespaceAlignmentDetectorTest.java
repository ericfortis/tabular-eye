package com.ericfortis.tabulareye.detectors;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class WhitespaceAlignmentDetectorTest extends BasePlatformTestCase {
	private WhitespaceAlignmentDetector detector;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		detector = new WhitespaceAlignmentDetector();
	}

	private @NonNull List<AlignmentDetector.AlignmentBlock> getBlocks(String fileName, String content) {
		var file = myFixture.configureByText(fileName, content);
		var doc = myFixture.getDocument(file);
		return detector.findBlocks(file, doc);
	}

	private @NonNull List<AlignmentDetector.AlignmentBlock> getBlocks(String content) {
		return getBlocks("Test.c", content);
	}

	public void testIsApplicableForC() {
		var file = myFixture.configureByText("Test.c", "int x;");
		assertTrue(detector.isApplicable(file));
	}

	public void testIsApplicableForGo() {
		var file = myFixture.configureByText("Test.go", "var x int");
		assertTrue(detector.isApplicable(file));
	}

	public void testIsNotApplicableForOtherFiles() {
		var file = myFixture.configureByText("Test.txt", "int x;");
		assertFalse(detector.isApplicable(file));
	}

	public void testDefineMacros() {
		var blocks = getBlocks("""
			 #define NGX_HTTP_GZIP_STATIC_OFF     0
			 #define NGX_HTTP_GZIP_STATIC_ON      1
			 #define NGX_HTTP_GZIP_STATIC_ALWAYS  2
			 """);
		assertEquals(1, blocks.size());

		var block = blocks.getFirst();
		assertEquals(3, block.size());
		assertEquals("#define NGX_HTTP_GZIP_STATIC_OFF", block.get(0).key().strip());
		assertEquals("#define NGX_HTTP_GZIP_STATIC_ON", block.get(1).key().strip());
		assertEquals("#define NGX_HTTP_GZIP_STATIC_ALWAYS", block.get(2).key().strip());
	}

	public void testVariableDeclarationsWithLeadingIndentation() {
		var blocks = getBlocks("""
			    u_char                       *p;
			    size_t                        root;
			    ngx_log_t                    *log;  /* comment  here */
			 """);
		assertEquals(1, blocks.size());

		var block = blocks.getFirst();
		assertEquals(3, block.size());
		assertEquals("u_char", block.get(0).key().strip());
		assertEquals("size_t", block.get(1).key().strip());
		assertEquals("ngx_log_t", block.get(2).key().strip());
	}

	public void testEmptyLineSeparatesBlocks() {
		var blocks = getBlocks("""
			 #define A  1
			 #define B  2

			 #define C  3
			 #define D  4
			 """);
		assertEquals(2, blocks.size());
		assertEquals(2, blocks.get(0).size());
		assertEquals(2, blocks.get(1).size());
	}

	public void testLineWithoutMultipleSpacesBreaksBlock() {
		var blocks = getBlocks("""
			 #define A  1
			 #define B  2
			 int noop();
			 #define C  3
			 #define D  4
			 """);
		assertEquals(2, blocks.size());
		assertEquals("A", block(blocks, 0, 0));
		assertEquals("B", block(blocks, 0, 1));
		assertEquals("C", block(blocks, 1, 0));
		assertEquals("D", block(blocks, 1, 1));
	}

	private String block(List<AlignmentDetector.AlignmentBlock> blocks, int blockIdx, int propIdx) {
		var line = blocks.get(blockIdx).get(propIdx).key().strip();
		return line.substring(line.lastIndexOf(' ') + 1);
	}

	public void testSingleLineIsNotABlock() {
		var blocks = getBlocks("""
			 #define A  1
			 """);
		assertTrue(blocks.isEmpty());
	}

	public void testLeadingWhitespaceOnlyIsNotAKey() {
		var blocks = getBlocks("""
			 #define A  1
			 #define B  2
			     no_key_before_this_run
			 #define C  3
			 #define D  4
			 """);
		assertEquals(2, blocks.size());
	}

	public void testSeparatorOffsetIsAtEndOfWhitespaceRun() {
		var content = "#define A  1\n#define BB  2\n";
		var blocks = getBlocks(content);
		assertEquals(1, blocks.size());

		var prop = blocks.getFirst().get(0);
		// "#define A" occupies indices 0-8, followed by a 2-space run at
		// indices 9-10; separatorOffset (10) + 1 must land right on "1" (index 11),
		// i.e. right after the whitespace run ends, not in the middle of it.
		assertEquals("#define A  ", prop.key());
		assertEquals(10, prop.separatorOffset());
		assertEquals('1', content.charAt(prop.separatorOffset() + 1));
	}
}
