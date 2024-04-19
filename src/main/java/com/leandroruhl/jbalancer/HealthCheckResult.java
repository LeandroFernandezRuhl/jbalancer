package com.leandroruhl.jbalancer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class HealthCheckResult {
    private final Server server;
    private final boolean isUp;

    public boolean isUp() {
        return isUp;
    }
}
