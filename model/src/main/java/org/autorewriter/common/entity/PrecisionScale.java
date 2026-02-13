package org.autorewriter.common.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Precision and Scale for numeric types
 * 参考 adaptiveengine 的 PrecisionScale
 *
 * @author AutoRewriter
 * Created on 2026-02-13
 */
@Getter
@AllArgsConstructor
public class PrecisionScale {
    private final int precision;
    private final int scale;
}

