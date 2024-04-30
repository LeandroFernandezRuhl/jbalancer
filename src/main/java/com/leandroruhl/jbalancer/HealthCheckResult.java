package com.leandroruhl.jbalancer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@EqualsAndHashCode
public class HealthCheckResult {
    private final Server server;
    private final boolean isUp;

    public boolean isUp() {
        return isUp;
    }
}
