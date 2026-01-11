package org.autorewriter.sql.common;

import lombok.AllArgsConstructor;
import org.apache.calcite.config.CalciteConnectionConfig;
import org.apache.calcite.plan.Context;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An instance of {@link Context}
 *
 * @author wangyanjing <wangyanjing@kuaishou.com>
 * Created on 2024-07-31
 */
@AllArgsConstructor
public class PlannerContext implements Context {
    private CalciteConnectionConfig calciteConnectionConfig;

    @Override
    public <C> @Nullable C unwrap(Class<C> clazz) {
        if (clazz.isInstance(this.calciteConnectionConfig)) {
            return clazz.cast(this.calciteConnectionConfig);
        }
        return null;
    }
}
