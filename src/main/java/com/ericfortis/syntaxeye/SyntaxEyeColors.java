package com.ericfortis.syntaxeye;

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;

public final class SyntaxEyeColors {
    public static final TextAttributesKey FAINT_TEXT =
            TextAttributesKey.createTextAttributesKey(
                    "SYNTAX_EYE_FAINT",
                    DefaultLanguageHighlighterColors.IDENTIFIER
            );

    public static TextAttributes getFaintTextAttributes() {
        return EditorColorsManager.getInstance().getGlobalScheme().getAttributes(FAINT_TEXT);
    }
}
