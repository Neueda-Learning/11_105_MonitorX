package com.MonitorX.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record Rule(
        int id,
        @NotBlank String name,
        @NotBlank String type,
        @NotBlank String severity,
        @NotNull String parameters, // JSON configuration string
        boolean isActive
) {}
