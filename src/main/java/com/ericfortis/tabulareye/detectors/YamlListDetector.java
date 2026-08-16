package com.ericfortis.tabulareye.detectors;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLSequence;
import org.jetbrains.yaml.psi.YAMLSequenceItem;

import java.util.ArrayList;
import java.util.List;

// TODO ignore when using monospace fonts

/**
 * For proportional fonts, pads the {@code -} list marker so it takes up the
 * same width as a space. Content following {@code - } then lines up with
 * content indented two spaces (e.g. a nested child prop of the list item).
 * <p>
 * Each alignment row's key is its line prefix: the left whitespace plus the
 * {@code - } marker for list items, or just the left whitespace for their
 * direct child props. The spacer is inserted right before the content, so all
 * content starts end up at the same column.
 * <p>
 * Consider the example below, if the space glyph is much wider than the hyphen,
 * without this padding {@code foo} would sit further left than {@code prop0}:
 * <p>
 * steps:
 * - foo: 2
 * prop0:
 * baz: 1
 * - bar: 4
 * prop1: 5
 */
public class YamlListDetector extends AlignmentDetector {
  YamlListDetector() {
    super(YML_EXT);
  }

  @Override
  public String getDisplayName() {
    return "YAML Lists";
  }

  @Override
  @NotNull
  public List<AlignmentBlock> findBlocks(@NotNull PsiFile file, @NotNull Document doc) {
    List<AlignmentBlock> blocks = new ArrayList<>();
    var chars = doc.getCharsSequence();

    for (var el : PsiTreeUtil.collectElementsOfType(file, YAMLSequence.class)) {
      var block = new AlignmentBlock();
      for (var item : el.getItems()) {
        int hyphenStart = findHyphenOffset(item);
        if (hyphenStart < 0)
          continue;

        var value = item.getValue();
        if (value == null)
          continue;

        if (value instanceof YAMLMapping mapping && !mapping.getText().stripLeading().startsWith("{"))
          for (var kv : mapping.getKeyValues()) {
            var keyElem = kv.getKey();
            if (keyElem != null)
              addProp(block, doc, chars, keyElem.getTextRange().getStartOffset());
          }
        else if (doc.getLineNumber(value.getTextRange().getStartOffset()) == doc.getLineNumber(hyphenStart))
          addProp(block, doc, chars, value.getTextRange().getStartOffset());
      }
      if (block.isValid())
        blocks.add(block);
    }

    return blocks;
  }

  /**
   * Adds a row whose content starts at {@code contentStart}. The key is
   * everything on the line before it (whitespace, plus {@code - } for list
   * items), so the spacer gets inserted right before the content.
   */
  private static void addProp(AlignmentBlock block, Document doc, CharSequence chars, int contentStart) {
    if (contentStart <= 0 || chars.charAt(contentStart - 1) != ' ')
      return;
    var lineStart = doc.getLineStartOffset(doc.getLineNumber(contentStart));
    if (contentStart == lineStart)
      return;

    var key = doc.getText(new TextRange(lineStart, contentStart));
    block.add(new PropInfo(key, lineStart, contentStart - 1));
  }

  private static int findHyphenOffset(YAMLSequenceItem item) {
    var child = item.getFirstChild();
    while (child != null) {
      if ("-".equals(child.getText()))
        return child.getTextRange().getStartOffset();
      child = child.getNextSibling();
    }
    return -1;
  }
}
