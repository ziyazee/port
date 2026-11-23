package com.porter.ksa.gps;

import com.porter.ksa.entity.GpsDevice;
import com.porter.ksa.entity.GpsProviderType;
import java.util.List;

public interface GpsProviderClient {

    GpsProviderType provider();

    List<GpsLocationSnapshot> fetchLatestLocations(List<GpsDevice> devices);
}
