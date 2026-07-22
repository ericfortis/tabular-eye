package com.ericfortis.tabulareye;

import com.ericfortis.tabulareye.detectors.AlignmentDetector;
import com.ericfortis.tabulareye.detectors.AlignmentDetector.AlignmentBlock;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
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

    private boolean isRefreshing = false;
    private final Editor editor;
    private final List<Inlay<Spacer>> activeInlays = new ArrayList<>();


    public Spacers(Editor editor) {
        this.editor = editor;
    }

    public void refresh(List<AlignmentBlock> blocks) {
        if (isRefreshing || editor.isDisposed())
            return;
        isRefreshing = true;
        try {
            clearAll();
            for (var b : blocks)
                render(b);
        } finally {
            isRefreshing = false;
        }
    }

    public void clearAll() {
        for (var inlay : activeInlays)
            if (inlay.isValid())
                Disposer.dispose(inlay);
        activeInlays.clear();
    }

    public List<AlignmentBlock> calcAlignments(List<AlignmentDetector> detectors, PsiFile psiFile, Document doc) {
        List<AlignmentBlock> allBlocks = new ArrayList<>();
        for (var d : detectors) {
            var blocks = d.findBlocks(psiFile, doc);
            if (!blocks.isEmpty())
                allBlocks.addAll(blocks);
        }
        return allBlocks;
    }

    private void render(AlignmentBlock block) {
        var props = block.props();

        int maxSepX = 0;
        int[] sepXs = new int[props.size()];
        for (int i = 0; i < props.size(); i++) {
            sepXs[i] = editor.offsetToXY(props.get(i).separatorOffset()).x;
            maxSepX = Math.max(maxSepX, sepXs[i]);
        }

        var model = editor.getInlayModel();
        for (int i = 0; i < props.size(); i++) {
            int spacerWidth = maxSepX - sepXs[i];
            if (spacerWidth > 0) {
                var inlay = model.addInlineElement(props.get(i).separatorOffset() + 1, true, new Spacer(spacerWidth));
                if (inlay != null)
                    activeInlays.add(inlay);
            }
        }
    }
}
