package org.autorewriter.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Column {

    private String name;
    private ColumnDataType type;
    private boolean partitioned;
}
