package com.ericfortis.tabulareye.detectors;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NginxDetector extends AlignmentDetector {
  NginxDetector() {
    super(Collections.emptyList());
  }

  @Override
  public String getDisplayName() {
    return "Nginx Config";
  }

  @Override
  public boolean isApplicable(@NotNull PsiFile file) {
    var vFile = file.getVirtualFile();
    return vFile != null && vFile.getName().endsWith("nginx.conf");
  }

  @Override
  @NotNull
  public List<AlignmentBlock> findBlocks(@NotNull PsiFile file, @NotNull Document doc) {
    List<AlignmentBlock> blocks = new ArrayList<>();
    var current = new AlignmentBlock();
    var lineCount = doc.getLineCount();
    var text = doc.getText();

    for (int i = 0; i < lineCount; i++) {
      ProgressManager.checkCanceled();
      var start = doc.getLineStartOffset(i);
      var end = doc.getLineEndOffset(i);
      var originalLine = text.substring(start, end);
      var line = originalLine.trim();

      if (line.isEmpty() || line.startsWith("#") || line.contains("{") || line.contains("}")) {
        if (current.isValid())
          blocks.add(current);
        current = new AlignmentBlock();
        continue;
      }

      var spaceIdx = line.indexOf(' ');
      if (spaceIdx > 0) { // Find the actual offset of the first space in the original (non-trimmed) line
        var key = line.substring(0, spaceIdx);
        var leadingSpaces = originalLine.indexOf(line);
        var keyOffset = start + leadingSpaces;
        var separatorOffset = keyOffset + spaceIdx;
        current.add(new PropInfo(key, keyOffset, separatorOffset));
      } else { // Line with no space (single-word directive like just "etag;") breaks the block
        if (current.isValid())
          blocks.add(current);
        current = new AlignmentBlock();
      }
    }

    if (current.isValid())
      blocks.add(current);

    return blocks;
  }
}
