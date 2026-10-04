package com.ericfortis.tabulareye;

import com.ericfortis.tabulareye.detectors.AlignmentDetector.AlignmentBlock;
import com.ericfortis.tabulareye.detectors.AlignmentDetector.PropInfo;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Inlay;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class SpacersTest extends BasePlatformTestCase {
  private static final String DOC = String.join("\n",
      "a:",
      "  bb:",
      "    ccc:",
      "dddddddddd:",
      "            ff:"
   ) + "\n";

  private Spacers spacers;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.configureByText("test.txt", DOC);
    spacers = new Spacers(myFixture.getEditor());
  }

  private AlignmentBlock block(String... keys) {
    var block = new AlignmentBlock();
    for (var key : keys) {
      int keyOffset = text().indexOf(key);
      block.add(new PropInfo(key, keyOffset, keyOffset + key.length() - 1));
    }
    return block;
  }

  private static int spacersFor(int keyCount) {
    return keyCount - 1;
  }

  private @NonNull String text() {
    return myFixture.getEditor().getDocument().getText();
  }

  private Document doc() {
    return myFixture.getEditor().getDocument();
  }

  private @NonNull List<Inlay<?>> inlays() {
    return myFixture.getEditor().getInlayModel()
       .getInlineElementsInRange(0, doc().getTextLength());
  }

  private void edit(Runnable change) {
    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      change.run();
      PsiDocumentManager.getInstance(getProject()).commitDocument(doc());
    });
  }

  public void testAddsOneInlayPerPropThatNeedsSpacing() {
    spacers.refresh(List.of(block("a:", "bb:", "ccc:")));

    assertEquals(spacersFor(3), inlays().size());
  }

  public void testReusesInlaysWhenBlocksAreUnchanged() {
    spacers.refresh(List.of(block("a:", "bb:", "ccc:")));
    var before = new ArrayList<>(inlays());
    assertEquals(spacersFor(3), before.size());

    spacers.refresh(List.of(block("a:", "bb:", "ccc:")));

    var after = inlays();
    assertEquals(before.size(), after.size());
    for (int i = 0; i < before.size(); i++) {
      assertSame("inlay " + i + " should have been reused", before.get(i), after.get(i));
      assertTrue("reused inlay " + i + " should still be valid", before.get(i).isValid());
    }
  }

  public void testReusesInlaysAfterAnUnrelatedEdit() {
    spacers.refresh(List.of(block("a:", "bb:", "ccc:")));
    var before = new ArrayList<>(inlays());
    assertEquals(spacersFor(3), before.size());

    edit(() -> doc().insertString(0, "\n"));

    spacers.refresh(List.of(block("a:", "bb:", "ccc:")));

    var after = inlays();
    assertEquals(before.size(), after.size());
    for (int i = 0; i < before.size(); i++)
      assertSame("inlay " + i + " should have survived the shift", before.get(i), after.get(i));
  }

  public void testReplacesInlaysWhenWidthsChange() {
    spacers.refresh(List.of(block("a:", "bb:", "ccc:")));
    var before = new ArrayList<>(inlays());
    assertEquals(spacersFor(3), before.size());
    var widthsBefore = before.stream().map(Inlay::getWidthInPixels).toList();

    edit(() -> doc().replaceString(text().indexOf("    ccc:"), text().indexOf("    ccc:") + 1, ""));

    spacers.refresh(List.of(block("a:", "bb:", "ccc:")));

    var after = inlays();
    assertEquals(before.size(), after.size());
    var widthsAfter = after.stream().map(Inlay::getWidthInPixels).toList();
    assertFalse("widths should have changed", widthsBefore.equals(widthsAfter));
    for (var inlay : before)
      assertFalse("the replaced inlay should be disposed", inlay.isValid());
  }

  public void testKeepsEarlierBlocksWhenALaterBlockDisappears() {
    var first = block("a:", "bb:", "ccc:");
    var second = block("dddddddddd:", "ff:");
    spacers.refresh(List.of(first, second));
    assertEquals(spacersFor(3) + spacersFor(2), inlays().size());

    spacers.refresh(List.of(first));

    var after = inlays();
    assertEquals(spacersFor(3), after.size());
    for (var inlay : after)
      assertTrue(inlay.isValid());
  }

  public void testDisposesInlaysWhenBlocksGrow() {
    var first = block("a:", "bb:", "ccc:");
    var second = block("dddddddddd:", "ff:");
    spacers.refresh(List.of(first));
    var before = new ArrayList<>(inlays());
    assertEquals(spacersFor(3), before.size());

    spacers.refresh(List.of(first, second));

    var after = inlays();
    assertEquals(before.size() + spacersFor(2), after.size());
    for (var inlay : before) {
      assertTrue("the earlier block should not have been touched", inlay.isValid());
    }
  }

  public void testClearAllDisposesEverything() {
    spacers.refresh(List.of(block("a:", "bb:", "ccc:")));
    var before = new ArrayList<>(inlays());
    assertEquals(spacersFor(3), before.size());

    spacers.clearAll();

    assertEquals(0, inlays().size());
    for (var inlay : before)
      assertFalse(inlay.isValid());
  }

  public void testRefreshAfterClearAllRebuilds() {
    var blocks = List.of(block("a:", "bb:", "ccc:"));
    spacers.refresh(blocks);
    spacers.clearAll();

    spacers.refresh(blocks);

    assertEquals(spacersFor(3), inlays().size());
  }
}