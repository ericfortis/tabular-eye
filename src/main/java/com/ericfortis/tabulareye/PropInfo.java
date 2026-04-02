package com.ericfortis.tabulareye;

/**
 * One alignable property: the key name text and the document offset
 * of the colon character (we'll place the inlay at colonOffset + 1).
 */
public record PropInfo(String keyText, int colonOffset) {
}
