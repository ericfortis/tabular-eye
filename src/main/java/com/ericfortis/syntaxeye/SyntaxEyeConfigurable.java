package com.ericfortis.syntaxeye;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class SyntaxEyeConfigurable implements Configurable {

    private JCheckBox myEnabledCheckbox;
    private JTextArea myWordsArea;

    @Override
    public String getDisplayName() {
        return "Syntax Eye";
    }

    @Override
    public @Nullable JComponent createComponent() {
        var panel = new JPanel(new BorderLayout(8, 8));

        myEnabledCheckbox = new JCheckBox("Enable Syntax Eye");
        panel.add(myEnabledCheckbox, BorderLayout.NORTH);

        myWordsArea = new JTextArea(15, 40);
        myWordsArea.setLineWrap(true);
        myWordsArea.setWrapStyleWord(false);
        var scrollPane = new JScrollPane(myWordsArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Words (one per line)"));
        panel.add(scrollPane, BorderLayout.CENTER);

        var wrapper = new JPanel(new BorderLayout());
        wrapper.add(panel, BorderLayout.NORTH);
        return wrapper;
    }

    @Override
    public boolean isModified() {
        var settings = SyntaxEyeSettings.getInstance();
        if (settings.isEnabled() != myEnabledCheckbox.isSelected())
            return true;
        return !settings.getWordsText().equals(myWordsArea.getText());
    }

    @Override
    public void apply() {
        var settings = SyntaxEyeSettings.getInstance();
        settings.setEnabled(myEnabledCheckbox.isSelected());
        settings.setWordsText(myWordsArea.getText());
        settings.notifyListeners();
    }

    @Override
    public void reset() {
        var settings = SyntaxEyeSettings.getInstance();
        myEnabledCheckbox.setSelected(settings.isEnabled());
        myWordsArea.setText(settings.getWordsText());
    }
}
