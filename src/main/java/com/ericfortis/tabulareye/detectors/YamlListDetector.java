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
 * This detector is only needed on proportional fonts. Its goal is to add a spacer
 * after list-marker hyphens, so the hyphen looks as wide as the space char. IOW,
 * it assumes the space glyph is wider than the hyphen.
 * <p>
 * For example, without this padding, `bar` would be aligned further right than `foo`:
 * <pre>
 * - foo: 1
 *   bar:
 *     baz: 2
 * </pre>
 * <p>
 * The terminology for {@code PropInfo} uses `key`, which makes no sense on yaml
 * lists, so think of `key` as the left-side (whatever goes before the spacer).
 * The key is either: the indentation whitespace plus the hyphen marker, or just the
 * indentation whitespace for their direct child props.
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

  private static int findHyphenOffset(YAMLSequenceItem item) {
    var child = item.getFirstChild();
    while (child != null) {
      if ("-".equals(child.getText()))
        return child.getTextRange().getStartOffset();
      child = child.getNextSibling();
    }
    return -1;
  }

  private static void addProp(AlignmentBlock block, Document doc, CharSequence chars, int contentStart) {
    if (contentStart <= 0 || chars.charAt(contentStart - 1) != ' ')
      return;
    var lineStart = doc.getLineStartOffset(doc.getLineNumber(contentStart));
    if (contentStart == lineStart)
      return;

    var key = doc.getText(new TextRange(lineStart, contentStart));
    block.add(new PropInfo(key, lineStart, contentStart - 1));
  }
}
