package com.ericfortis.tabulareye;

import com.ericfortis.tabulareye.detectors.AlignmentDetector;
import com.ericfortis.tabulareye.detectors.AlignmentDetector.AlignmentBlock;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Spacers {
  /**
   * A fully transparent inlay that occupies exactly {@code widthPx} pixels.
   * Note: antialiasing (user setting) greyscale makes columns not align 100% perfectly.
   */
  private record Spacer(int widthPx) implements EditorCustomElementRenderer {
    @Override
    public int calcWidthInPixels(@NotNull Inlay inlay) {
      return widthPx;
    }

    @Override
    public int calcHeightInPixels(@NotNull Inlay inlay) {
      return 1;
    }

		/* DEBUG COLOR
		@Override
		public void paint(@NotNull Inlay inlay, @NotNull Graphics g, @NotNull Rectangle targetRegion, @NotNull com.intellij.openapi.editor.markup.TextAttributes textAttributes) {
			g.setColor(new Color(200, 0, 0, 60));
			g.fillRect(targetRegion.x, targetRegion.y, targetRegion.width, targetRegion.height);
		}
        */
  }

  private record Wanted(int offset, int widthPx) {
  }

  private boolean isRefreshing = false;
  private final Editor editor;
  private final List<Inlay<Spacer>> slotInlays = new ArrayList<>();


  public Spacers(Editor editor) {
    this.editor = editor;
  }

  public void refresh(List<AlignmentBlock> blocks) {
    if (isRefreshing || editor.isDisposed())
      return;
    isRefreshing = true;
    try {
      var wanted = new ArrayList<Wanted>();
      for (var b : blocks)
        measure(b, wanted);
      reconcile(wanted);
    } finally {
      isRefreshing = false;
    }
  }

  public void clearAll() {
    for (var inlay : slotInlays)
      if (inlay != null && inlay.isValid())
        Disposer.dispose(inlay);
    slotInlays.clear();
  }

  public List<AlignmentBlock> calcAlignments(List<AlignmentDetector> detectors, PsiFile psiFile, Document doc) {
    List<AlignmentBlock> allBlocks = new ArrayList<>();
    for (var d : detectors) {
      ProgressManager.checkCanceled();
      var blocks = d.findBlocks(psiFile, doc);
      if (!blocks.isEmpty())
        allBlocks.addAll(blocks);
    }
    return allBlocks;
  }

  private void measure(AlignmentBlock block, List<Wanted> out) {
    var props = block.props();

    var doc = editor.getDocument();
    int maxValidOffset = doc.getTextLength() - 1;

    int maxSepX = 0;
    int[] sepXs = new int[props.size()];
    boolean[] valid = new boolean[props.size()];
    for (int i = 0; i < props.size(); i++) {
      int sepOffset = props.get(i).separatorOffset();
      if (sepOffset < 0 || sepOffset > maxValidOffset)
        continue;
      valid[i] = true;
      sepXs[i] = editor.offsetToXY(sepOffset).x;
      maxSepX = Math.max(maxSepX, sepXs[i]);
    }

    for (int i = 0; i < props.size(); i++)
      if (valid[i] && maxSepX > sepXs[i])
        out.add(new Wanted(props.get(i).separatorOffset() + 1, maxSepX - sepXs[i]));
  }

  private void reconcile(List<Wanted> wanted) {
    var model = editor.getInlayModel();

    for (int i = 0; i < wanted.size(); i++) {
      var want = wanted.get(i);
      Inlay<Spacer> inlay = i < slotInlays.size() ? slotInlays.get(i) : null;

      if (inlay != null && inlay.isValid()
         && inlay.getOffset() == want.offset()
         && inlay.getWidthInPixels() == want.widthPx())
        continue;

      if (inlay != null && inlay.isValid())
        Disposer.dispose(inlay);

      var added = model.addInlineElement(want.offset(), true, new Spacer(want.widthPx()));
      if (i < slotInlays.size())
        slotInlays.set(i, added);
      else
        slotInlays.add(added);
    }

    while (slotInlays.size() > wanted.size()) {
      var stale = slotInlays.remove(slotInlays.size() - 1);
      if (stale != null && stale.isValid())
        Disposer.dispose(stale);
    }
  }
}
