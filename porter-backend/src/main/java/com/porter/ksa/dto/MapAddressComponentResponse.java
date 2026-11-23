package com.porter.ksa.dto;

import java.util.List;

public record MapAddressComponentResponse(
        String longName,
        String shortName,
        List<String> types
) {
}
