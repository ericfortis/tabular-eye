package com.ericfortis.tabulareye;

import com.ericfortis.tabulareye.detectors.AlignmentDetector;
import com.ericfortis.tabulareye.detectors.AlignmentDetector.AlignmentBlock;
import com.ericfortis.tabulareye.detectors.AlignmentDetector.PropInfo;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.colors.EditorFontType;
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
            for (var b : blocks)
                for (var prop : b.props())
                    setKeyWidth(prop);

            if (!blocks.isEmpty())
                allBlocks.addAll(blocks);
        }
        return allBlocks;
    }

    // Handles proportional fonts, and since bold is wider, always using it for measuring
    // handles mixed plain and bold left sides.
    private void setKeyWidth(PropInfo prop) {
        prop.setKeyWidth(getBoldFontMetrics().stringWidth(prop.key()));
    }

    private void render(AlignmentBlock block) {
        var props = block.props();

        int maxWidth = 0;
        for (var p : props)
            maxWidth = Math.max(maxWidth, p.keyWidth());

        // Insert spacers
        var model = editor.getInlayModel();
        for (var p : props) {
            int spacerWidth = maxWidth - p.keyWidth();
            if (spacerWidth > 0) { // skips longest
                int placeAt = p.separatorOffset() + 1;
                var inlay = model.addInlineElement(placeAt, true, new Spacer(spacerWidth));
                if (inlay != null)
                    activeInlays.add(inlay);
            }
        }
    }

    private FontMetrics getBoldFontMetrics() {
        var font = editor.getColorsScheme().getFont(EditorFontType.BOLD);
        return editor.getContentComponent().getFontMetrics(font);
    }
}
