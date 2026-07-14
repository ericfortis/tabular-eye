package com.ericfortis.tabulareye;

import com.ericfortis.tabulareye.detectors.AlignmentDetector;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class DetectorSettingsConfigurable implements Configurable {

    private static final ExtensionPointName<AlignmentDetector> EPN =
            ExtensionPointName.create("com.ericfortis.tabulareye.alignmentDetector");

    private final List<DetectorCheckbox> myCheckboxes = new ArrayList<>();

    @Override
    public String getDisplayName() {
        return "Tabular Eye";
    }

    @Override
    public @Nullable JComponent createComponent() {
        var myPanel = new JPanel();
        myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.Y_AXIS));

        var detectors = EPN.getExtensionList().stream()
                .sorted(Comparator.comparing(AlignmentDetector::getDisplayName))
                .toList();

        var settings = DetectorSettings.getInstance();

        myCheckboxes.clear();
        for (var detector : detectors) {
            var className = detector.getClass().getName();
            var label = buildLabel(detector);
            var checkbox = new JCheckBox(label);
            checkbox.setSelected(settings.isDetectorEnabled(className));
            myCheckboxes.add(new DetectorCheckbox(className, checkbox));
            myPanel.add(checkbox);
        }

        myPanel.add(Box.createVerticalGlue());

        var wrapper = new JPanel(new BorderLayout());
        wrapper.add(myPanel, BorderLayout.NORTH);
        return wrapper;
    }

    @Override
    public boolean isModified() {
        var settings = DetectorSettings.getInstance();
        for (var dc : myCheckboxes)
            if (settings.isDetectorEnabled(dc.className) != dc.checkbox.isSelected())
                return true;
        return false;
    }

    @Override
    public void apply() {
        var settings = DetectorSettings.getInstance();
        for (var dc : myCheckboxes)
            settings.setDetectorEnabled(dc.className, dc.checkbox.isSelected());
        settings.notifyListeners();
    }

    @Override
    public void reset() {
        var settings = DetectorSettings.getInstance();
        for (var dc : myCheckboxes)
            dc.checkbox.setSelected(settings.isDetectorEnabled(dc.className));
    }

    private static String buildLabel(AlignmentDetector detector) {
        var exts = detector.getExtensions();
        if (exts.isEmpty())
            return detector.getDisplayName();
        var extList = exts.stream().collect(Collectors.joining(", "));
        return detector.getDisplayName() + " (" + extList + ")";
    }

    private record DetectorCheckbox(String className, JCheckBox checkbox) {
    }
}
