package org.autorewriter.common.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PrecisionScale {
    private final int precision;
    private final int scale;
}

