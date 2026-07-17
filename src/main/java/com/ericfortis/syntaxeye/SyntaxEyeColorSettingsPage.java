package com.ericfortis.syntaxeye;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.PlainSyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import javax.swing.*;
import java.util.Map;

public class SyntaxEyeColorSettingsPage implements ColorSettingsPage {

    private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[]{
            new AttributesDescriptor("Faint text", SyntaxEyeColors.FAINT_TEXT),
    };

    @Override
    public @Nullable Icon getIcon() {
        return null;
    }

    @Override
    public @NotNull SyntaxHighlighter getHighlighter() {
        return new PlainSyntaxHighlighter();
    }

    @Override
    public @NotNull String getDemoText() {
        return """
                const <faint>FOO_</faint>BAR = 0
                const <faint>FOO_</faint>BAZ = 0
                function <faint>FOO_</faint>FUNC () {}
                """;
    }

    @Override
    public @Nullable Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return Map.of("faint", SyntaxEyeColors.FAINT_TEXT);
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Syntax Eye";
    }

    @Override
    public @NotNull AttributesDescriptor @NonNull [] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public @NotNull ColorDescriptor @NonNull [] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }
}
