package com.tchalanet.server.core.sales.internal.domain.service;

import java.util.Set;


/**
 * Minimal, read-only view of the draw result needed for matching ticket lines.
 *
 * <p>All returned values should be normalized:
 * <ul>
 *   <li>twoDigits: collection of 2-digit strings "00".."99"</li>
 *   <li>pick3: 3-digit string, zero-padded if needed</li>
 *   <li>pick4: 4-digit string, zero-padded if needed</li>
 *   <li>pick5: 5-digit string, zero-padded if needed</li>
 * </ul>
 */
public interface DrawResultView {
    /**
     * All winning 2D numbers for the occurrence (already normalized "00".."99").
     */
    Set<String> twoDigits();

    /**
     * Winning 3D number, normalized "000".."999" (nullable if not applicable).
     */
    String pick3();

    /**
     * Winning 4D number, normalized "0000".."9999" (nullable).
     */
    String pick4();

    /**
     * Winning 5D number, normalized "00000".."99999" (nullable).
     */
    String pick5();
}
