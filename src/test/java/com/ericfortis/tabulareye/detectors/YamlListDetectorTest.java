package com.ericfortis.tabulareye.detectors;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class YamlListDetectorTest extends BasePlatformTestCase {
  private YamlListDetector detector;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    detector = new YamlListDetector();
  }

  private @NonNull List<AlignmentDetector.AlignmentBlock> getBlocks(String content) {
    var file = myFixture.configureByText("test.yml", content);
    var doc = myFixture.getDocument(file);
    return detector.findBlocks(file, doc);
  }

  public void testBasicYamlList() {
    var blocks = getBlocks("""
       list:
         - item1
         - item2
         - item3
       """);
    assertEquals(1, blocks.size());

    var block = blocks.getFirst();
    assertEquals(3, block.size());
    for (var prop : block.props())
      assertEquals("  - ", prop.key());
  }

  public void testNestedListProps() {
    var blocks = getBlocks("""
       steps:
         - foo: 2
           prop0:
             baz: 1
         - bar: 4
           prop1: 5
       """);
    assertEquals(1, blocks.size());

    var block = blocks.getFirst();
    assertEquals(4, block.size());
    assertEquals("  - ", block.get(0).key());
    assertEquals("    ", block.get(1).key());
    assertEquals("  - ", block.get(2).key());
    assertEquals("    ", block.get(3).key());
  }

  public void testSkipsItemWithValueOnNextLine() {
    var blocks = getBlocks("""
       list:
         -
           item1
         - item2
         - item3
       """);
    assertEquals(1, blocks.size());
    assertEquals(2, blocks.getFirst().size());
  }

  public void testIgnoresInlineList() {
    var blocks = getBlocks("list: [a, b, c]");
    assertTrue(blocks.isEmpty());
  }
}
