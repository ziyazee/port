package com.porter.ksa.dto;

import java.util.List;

public record MapDetailsResponse(
        String provider,
        String formattedAddress,
        String placeId,
        String plusCode,
        String compoundPlusCode,
        List<String> types,
        List<MapAddressComponentResponse> addressComponents
) {
}
