package com.MonitorX.models;

public record Customer(
        int id,
        String name,
        String accountNumber,
        String registeredCountry
) {}
