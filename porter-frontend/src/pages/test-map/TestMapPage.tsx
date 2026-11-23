import { useCallback, useEffect, useRef, useState, type FormEvent } from "react";
import { googleMapsConfig, loadGoogleMapsScript } from "@/config/googleMaps";
import { Sidebar } from "@/features/dashboard/components/Sidebar";
import { Topbar } from "@/features/dashboard/components/Topbar";
import { apiClient } from "@/services/apiClient";

type TestMapPageProps = {
  onBackToDashboard: () => void;
  displayName: string;
  role: "ADMIN" | "OPERATIONS";
};

type MapAddressComponent = {
  longName: string | null;
  shortName: string | null;
  types: string[];
};

type MapDetails = {
  provider: string;
  formattedAddress: string | null;
  placeId: string | null;
  plusCode: string | null;
  compoundPlusCode: string | null;
  types: string[];
  addressComponents: MapAddressComponent[];
};

type WialonCurrentLocation = {
  provider: "WIALON";
  wialonUnitId: number;
  unitName: string | null;
  latitude: number;
  longitude: number;
  speedKph: number;
  headingDegrees: number;
  recordedAt: string;
  receivedAt: string;
  mapDetails: MapDetails | null;
};

type RouteSummary = {
  distanceText: string;
  durationText: string;
  distanceMeters: number;
  durationSeconds: number;
};

const DEFAULT_UNIT_ID = "734455";
const DEFAULT_CENTER = { lat: 24.7136, lng: 46.6753 };
const DESTINATION = { lat: 52.320937, lng: 9.801226 };
const TRUCK_MARKER_SIZE = { width: 44, height: 78 };
const LIVE_REFRESH_MS = 10_000;
const MOVE_ANIMATION_MS = 1_200;
const MIN_DEAD_RECKONING_SPEED_KPH = 1;
const MAX_DEAD_RECKONING_STEP_SECONDS = 1;
const EARTH_RADIUS_METERS = 6_371_000;
const MIN_INFERRED_DISTANCE_METERS = 3;
const MAP_EDGE_PADDING_RATIO = 0.18;

type Coordinates = {
  lat: number;
  lng: number;
};

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "medium",
  }).format(new Date(value));
}

function findAddressPart(details: MapDetails | null, type: string) {
  return details?.addressComponents.find((component) => component.types.includes(type))
    ?.longName;
}

function nearlySamePosition(start: Coordinates, end: Coordinates) {
  return Math.abs(start.lat - end.lat) < 0.000001 && Math.abs(start.lng - end.lng) < 0.000001;
}

function interpolatePosition(start: Coordinates, end: Coordinates, progress: number) {
  return {
    lat: start.lat + (end.lat - start.lat) * progress,
    lng: start.lng + (end.lng - start.lng) * progress,
  };
}

function easeInOut(progress: number) {
  return progress < 0.5
    ? 2 * progress * progress
    : 1 - Math.pow(-2 * progress + 2, 2) / 2;
}

function speedKphToMetersPerSecond(speedKph: number) {
  return (speedKph * 1000) / 3600;
}

function projectPosition(
  position: Coordinates,
  headingDegrees: number,
  distanceMeters: number
) {
  const angularDistance = distanceMeters / EARTH_RADIUS_METERS;
  const bearing = (headingDegrees * Math.PI) / 180;
  const lat1 = (position.lat * Math.PI) / 180;
  const lng1 = (position.lng * Math.PI) / 180;

  const lat2 = Math.asin(
    Math.sin(lat1) * Math.cos(angularDistance) +
      Math.cos(lat1) * Math.sin(angularDistance) * Math.cos(bearing)
  );
  const lng2 =
    lng1 +
    Math.atan2(
      Math.sin(bearing) * Math.sin(angularDistance) * Math.cos(lat1),
      Math.cos(angularDistance) - Math.sin(lat1) * Math.sin(lat2)
    );

  return {
    lat: (lat2 * 180) / Math.PI,
    lng: (((lng2 * 180) / Math.PI + 540) % 360) - 180,
  };
}

function distanceMeters(start: Coordinates, end: Coordinates) {
  const lat1 = (start.lat * Math.PI) / 180;
  const lat2 = (end.lat * Math.PI) / 180;
  const deltaLat = ((end.lat - start.lat) * Math.PI) / 180;
  const deltaLng = ((end.lng - start.lng) * Math.PI) / 180;
  const a =
    Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
    Math.cos(lat1) *
      Math.cos(lat2) *
      Math.sin(deltaLng / 2) *
      Math.sin(deltaLng / 2);
  return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function bearingDegrees(start: Coordinates, end: Coordinates) {
  const lat1 = (start.lat * Math.PI) / 180;
  const lat2 = (end.lat * Math.PI) / 180;
  const deltaLng = ((end.lng - start.lng) * Math.PI) / 180;
  const x = Math.sin(deltaLng) * Math.cos(lat2);
  const y =
    Math.cos(lat1) * Math.sin(lat2) -
    Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLng);
  return (((Math.atan2(x, y) * 180) / Math.PI) + 360) % 360;
}

function createTruckOverlay(
  map: any,
  position: { lat: number; lng: number },
  headingDegrees: number,
  title: string
) {
  class TruckOverlay extends window.google.maps.OverlayView {
    private element: HTMLDivElement | null = null;
    private position = position;
    private headingDegrees = headingDegrees;
    private title = title;

    onAdd() {
      this.element = document.createElement("div");
      this.element.className = "test-map-truck-marker";
      this.element.style.width = `${TRUCK_MARKER_SIZE.width}px`;
      this.element.style.height = `${TRUCK_MARKER_SIZE.height}px`;
      this.element.title = this.title;

      const image = document.createElement("img");
      image.alt = "";
      image.draggable = false;
      image.src = "/map-markers/truck-topdown.svg";
      this.element.appendChild(image);

      this.getPanes()?.overlayMouseTarget.appendChild(this.element);
    }

    draw() {
      if (!this.element) {
        return;
      }

      const projection = this.getProjection();
      const point = projection.fromLatLngToDivPixel(
        new window.google.maps.LatLng(this.position.lat, this.position.lng)
      );
      if (!point) {
        return;
      }

      this.element.style.transform = [
        `translate(${point.x - TRUCK_MARKER_SIZE.width / 2}px, ${point.y - TRUCK_MARKER_SIZE.height / 2}px)`,
        `rotate(${this.headingDegrees}deg)`,
      ].join(" ");
    }

    onRemove() {
      this.element?.remove();
      this.element = null;
    }

    setPosition(nextPosition: { lat: number; lng: number }) {
      this.position = nextPosition;
      this.draw();
    }

    setHeading(nextHeadingDegrees: number) {
      this.headingDegrees = nextHeadingDegrees;
      this.draw();
    }

    setTitle(nextTitle: string) {
      this.title = nextTitle;
      if (this.element) {
        this.element.title = nextTitle;
      }
    }
  }

  const overlay = new TruckOverlay();
  overlay.setMap(map);
  return overlay;
}

function keepTruckInView(map: any, position: Coordinates, force = false) {
  if (force) {
    map.panTo(position);
    return;
  }

  const bounds = map.getBounds?.();
  if (!bounds) {
    map.panTo(position);
    return;
  }

  const northEast = bounds.getNorthEast();
  const southWest = bounds.getSouthWest();
  const latPadding =
    Math.abs(northEast.lat() - southWest.lat()) * MAP_EDGE_PADDING_RATIO;
  const lngPadding =
    Math.abs(northEast.lng() - southWest.lng()) * MAP_EDGE_PADDING_RATIO;

  const insidePaddedBounds =
    position.lat <= northEast.lat() - latPadding &&
    position.lat >= southWest.lat() + latPadding &&
    position.lng <= northEast.lng() - lngPadding &&
    position.lng >= southWest.lng() + lngPadding;

  if (!insidePaddedBounds) {
    map.panTo(position);
  }
}

export function TestMapPage({
  onBackToDashboard,
  displayName,
  role,
}: TestMapPageProps) {
  const [unitId, setUnitId] = useState(DEFAULT_UNIT_ID);
  const [trackedUnitId, setTrackedUnitId] = useState("");
  const [location, setLocation] = useState<WialonCurrentLocation | null>(null);
  const [loading, setLoading] = useState(false);
  const [lastUpdatedAt, setLastUpdatedAt] = useState<string | null>(null);
  const [liveEnabled, setLiveEnabled] = useState(true);
  const [backgroundRefreshing, setBackgroundRefreshing] = useState(false);
  const [visualMotionMode, setVisualMotionMode] = useState<"parked" | "reported" | "inferred">("parked");
  const [routeSummary, setRouteSummary] = useState<RouteSummary | null>(null);
  const [routeError, setRouteError] = useState("");
  const [mapLoading, setMapLoading] = useState(true);
  const [error, setError] = useState("");
  const [mapError, setMapError] = useState("");
  const mapContainerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<any>(null);
  const markerRef = useRef<any>(null);
  const destinationMarkerRef = useRef<any>(null);
  const directionsServiceRef = useRef<any>(null);
  const directionsRendererRef = useRef<any>(null);
  const snapRequestIdRef = useRef(0);
  const pollingRef = useRef(false);
  const markerPositionRef = useRef<Coordinates | null>(null);
  const animationFrameRef = useRef<number | null>(null);
  const deadReckoningFrameRef = useRef<number | null>(null);
  const deadReckoningLastFrameAtRef = useRef<number | null>(null);
  const lastRealPointRef = useRef<{
    position: Coordinates;
    receivedAtMs: number;
  } | null>(null);
  const latestMotionRef = useRef({
    headingDegrees: 0,
    speedKph: 0,
  });

  useEffect(() => {
    let ignore = false;

    async function initializeMap() {
      setMapLoading(true);
      setMapError("");

      try {
        await loadGoogleMapsScript();
        const [mapsLibrary, routesLibrary] = await Promise.all([
          window.google.maps.importLibrary("maps"),
          window.google.maps.importLibrary("routes"),
        ]);

        if (ignore || !mapContainerRef.current) {
          return;
        }

        const map = new mapsLibrary.Map(mapContainerRef.current, {
          center: DEFAULT_CENTER,
          fullscreenControl: false,
          mapTypeControl: false,
          streetViewControl: false,
          zoom: 11,
        });

        const marker = createTruckOverlay(
          map,
          DEFAULT_CENTER,
          0,
          "Current vehicle location"
        );
        const directionsRenderer = new routesLibrary.DirectionsRenderer({
          map,
          preserveViewport: true,
          polylineOptions: {
            strokeColor: "#16a34a",
            strokeOpacity: 0.9,
            strokeWeight: 5,
          },
          suppressMarkers: true,
        });
        const destinationMarker = new window.google.maps.Marker({
          icon: {
            url: "/map-markers/destination-hub.svg",
            scaledSize: new window.google.maps.Size(96, 72),
            anchor: new window.google.maps.Point(48, 56),
          },
          map,
          position: DESTINATION,
          title: "Destination hub",
        });

        mapRef.current = map;
        markerRef.current = marker;
        destinationMarkerRef.current = destinationMarker;
        directionsServiceRef.current = new routesLibrary.DirectionsService();
        directionsRendererRef.current = directionsRenderer;
      } catch (exception) {
        if (!ignore) {
          setMapError(
            exception instanceof Error ? exception.message : "Could not load Google Maps."
          );
        }
      } finally {
        if (!ignore) {
          setMapLoading(false);
        }
      }
    }

    initializeMap();

    return () => {
      ignore = true;
      if (animationFrameRef.current) {
        window.cancelAnimationFrame(animationFrameRef.current);
      }
      stopDeadReckoning();
    };
  }, []);

  useEffect(() => {
    if (!location || !mapRef.current || !markerRef.current) {
      return;
    }

    const requestId = snapRequestIdRef.current + 1;
    snapRequestIdRef.current = requestId;
    const currentLocation = location;
    const rawPosition = {
      lat: currentLocation.latitude,
      lng: currentLocation.longitude,
    };

    async function updateMarkerOnRoad() {
      const position = await snapToNearestRoad(rawPosition);
      if (snapRequestIdRef.current !== requestId || !mapRef.current || !markerRef.current) {
        return;
      }

      animateTruckTo(position, currentLocation.headingDegrees);
      markerRef.current.setTitle(
        currentLocation.unitName ?? `Unit ${currentLocation.wialonUnitId}`
      );
      mapRef.current.setZoom(16);
      updateMotionFromRealPoint(position, currentLocation);
      updateRouteSummary(position);
      startDeadReckoning();
    }

    updateMarkerOnRoad();
  }, [location]);

  const loadLocation = useCallback(async (nextUnitId = unitId, options?: { background?: boolean }) => {
    const trimmedUnitId = nextUnitId.trim();
    if (!trimmedUnitId) {
      setError("Enter a Wialon unit id.");
      return;
    }

    if (options?.background && pollingRef.current) {
      return;
    }

    if (options?.background) {
      pollingRef.current = true;
      setBackgroundRefreshing(true);
    } else {
      setLoading(true);
    }
    setError("");

    try {
      const response = await apiClient.get<WialonCurrentLocation>(
        `/gps/wialon/units/${encodeURIComponent(trimmedUnitId)}/current-location`
      );
      setLocation(response);
      setTrackedUnitId(trimmedUnitId);
      setLastUpdatedAt(new Date().toISOString());
    } catch (exception) {
      setError(
        exception instanceof Error ? exception.message : "Could not load vehicle location."
      );
    } finally {
      if (options?.background) {
        pollingRef.current = false;
        setBackgroundRefreshing(false);
      } else {
        setLoading(false);
      }
    }
  }, [unitId]);

  useEffect(() => {
    if (!trackedUnitId || !liveEnabled) {
      return;
    }

    const intervalId = window.setInterval(() => {
      loadLocation(trackedUnitId, { background: true });
    }, LIVE_REFRESH_MS);

    return () => window.clearInterval(intervalId);
  }, [liveEnabled, loadLocation, trackedUnitId]);

  function animateTruckTo(position: Coordinates, headingDegrees: number) {
    if (!mapRef.current || !markerRef.current) {
      return;
    }

    if (animationFrameRef.current) {
      window.cancelAnimationFrame(animationFrameRef.current);
    }

    const start = markerPositionRef.current ?? position;
    if (nearlySamePosition(start, position)) {
      markerRef.current.setPosition(position);
      markerRef.current.setHeading(headingDegrees);
      keepTruckInView(mapRef.current, position, true);
      markerPositionRef.current = position;
      return;
    }

    const startedAt = performance.now();

    function frame(now: number) {
      if (!mapRef.current || !markerRef.current) {
        return;
      }

      const progress = Math.min((now - startedAt) / MOVE_ANIMATION_MS, 1);
      const easedProgress = easeInOut(progress);
      const nextPosition = interpolatePosition(start, position, easedProgress);

      markerRef.current.setPosition(nextPosition);
      markerRef.current.setHeading(headingDegrees);
      keepTruckInView(mapRef.current, nextPosition);

      if (progress < 1) {
        animationFrameRef.current = window.requestAnimationFrame(frame);
        return;
      }

      markerPositionRef.current = position;
      animationFrameRef.current = null;
    }

    animationFrameRef.current = window.requestAnimationFrame(frame);
  }

  function startDeadReckoning() {
    if (deadReckoningFrameRef.current !== null) {
      return;
    }

    deadReckoningLastFrameAtRef.current = performance.now();
    deadReckoningFrameRef.current = window.requestAnimationFrame(deadReckonFrame);
  }

  function stopDeadReckoning() {
    if (deadReckoningFrameRef.current !== null) {
      window.cancelAnimationFrame(deadReckoningFrameRef.current);
      deadReckoningFrameRef.current = null;
    }
    deadReckoningLastFrameAtRef.current = null;
  }

  function deadReckonFrame(now: number) {
    deadReckoningFrameRef.current = null;

    if (!markerRef.current || !mapRef.current || !markerPositionRef.current) {
      deadReckoningLastFrameAtRef.current = now;
      deadReckoningFrameRef.current = window.requestAnimationFrame(deadReckonFrame);
      return;
    }

    const motion = latestMotionRef.current;
    if (motion.speedKph < MIN_DEAD_RECKONING_SPEED_KPH) {
      deadReckoningLastFrameAtRef.current = now;
      deadReckoningFrameRef.current = window.requestAnimationFrame(deadReckonFrame);
      return;
    }

    const previousFrameAt = deadReckoningLastFrameAtRef.current ?? now;
    const elapsedSeconds = Math.min(
      (now - previousFrameAt) / 1000,
      MAX_DEAD_RECKONING_STEP_SECONDS
    );
    deadReckoningLastFrameAtRef.current = now;

    const distanceMeters = speedKphToMetersPerSecond(motion.speedKph) * elapsedSeconds;
    const nextPosition = projectPosition(
      markerPositionRef.current,
      motion.headingDegrees,
      distanceMeters
    );

    markerRef.current.setPosition(nextPosition);
    markerRef.current.setHeading(motion.headingDegrees);
    keepTruckInView(mapRef.current, nextPosition);
    markerPositionRef.current = nextPosition;

    deadReckoningFrameRef.current = window.requestAnimationFrame(deadReckonFrame);
  }

  function updateMotionFromRealPoint(
    position: Coordinates,
    currentLocation: WialonCurrentLocation
  ) {
    const receivedAtMs = Date.now();
    const previousPoint = lastRealPointRef.current;
    let headingDegrees = currentLocation.headingDegrees;
    let speedKph = currentLocation.speedKph;
    let mode: "parked" | "reported" | "inferred" =
      speedKph >= MIN_DEAD_RECKONING_SPEED_KPH ? "reported" : "parked";

    if (speedKph < MIN_DEAD_RECKONING_SPEED_KPH && previousPoint) {
      const movedMeters = distanceMeters(previousPoint.position, position);
      const elapsedSeconds = Math.max(
        (receivedAtMs - previousPoint.receivedAtMs) / 1000,
        1
      );

      if (movedMeters >= MIN_INFERRED_DISTANCE_METERS) {
        speedKph = (movedMeters / elapsedSeconds) * 3.6;
        headingDegrees = bearingDegrees(previousPoint.position, position);
        mode = "inferred";
      }
    }

    latestMotionRef.current = {
      headingDegrees,
      speedKph,
    };
    lastRealPointRef.current = {
      position,
      receivedAtMs,
    };
    setVisualMotionMode(mode);
  }

  function updateRouteSummary(origin: Coordinates) {
    if (!directionsServiceRef.current || !directionsRendererRef.current) {
      return;
    }

    directionsServiceRef.current.route(
      {
        destination: DESTINATION,
        origin,
        travelMode: window.google.maps.TravelMode.DRIVING,
      },
      (result: any, status: string) => {
        if (status !== window.google.maps.DirectionsStatus.OK || !result) {
          setRouteSummary(null);
          setRouteError(`Could not calculate route: ${status}`);
          return;
        }

        const leg = result.routes?.[0]?.legs?.[0];
        directionsRendererRef.current.setDirections(result);
        setRouteError("");
        setRouteSummary({
          distanceText: leg?.distance?.text ?? "-",
          durationText: leg?.duration?.text ?? "-",
          distanceMeters: leg?.distance?.value ?? 0,
          durationSeconds: leg?.duration?.value ?? 0,
        });
      }
    );
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    loadLocation();
  }

  const city = findAddressPart(location?.mapDetails ?? null, "locality");
  const region = findAddressPart(
    location?.mapDetails ?? null,
    "administrative_area_level_1"
  );
  const country = findAddressPart(location?.mapDetails ?? null, "country");

  return (
    <main className="dashboard-page">
      <Sidebar
        activeItem="Test Map"
        onDashboard={onBackToDashboard}
        onTestMap={() => undefined}
        role={role}
      />
      <div className="dashboard-shell">
        <Topbar
          breadcrumb={["Tracking", "Test Map"]}
          displayName={displayName}
          role={role}
          showSearch={false}
        />

        <section className="test-map-workspace">
          <header className="test-map-header">
            <div>
              <h1>Test Map</h1>
              <p>Fetch a Wialon unit location and plot it on Google Maps.</p>
            </div>
            <button
              className="customer-button customer-button--secondary"
              type="button"
              onClick={onBackToDashboard}
            >
              <span className="material-symbols-outlined">arrow_back</span>
              Back
            </button>
          </header>

          <form className="test-map-search" onSubmit={submit}>
            <label>
              <span>Wialon Unit ID</span>
              <input
                inputMode="numeric"
                placeholder="734455"
                value={unitId}
                onChange={(event) => setUnitId(event.target.value)}
              />
            </label>
            <button
              className="customer-button customer-button--primary"
              disabled={loading}
              type="submit"
            >
              <span className="material-symbols-outlined">my_location</span>
              {loading ? "Loading..." : "Locate"}
            </button>
          </form>

          <div className="test-map-livebar">
            <label className="test-map-toggle">
              <input
                checked={liveEnabled}
                type="checkbox"
                onChange={(event) => setLiveEnabled(event.target.checked)}
              />
              <span>Live refresh</span>
            </label>
            <span className={`test-map-live-status${trackedUnitId && liveEnabled ? " is-live" : ""}`}>
              {trackedUnitId && liveEnabled ? "Live" : "Idle"}
            </span>
            <span>
              Every {LIVE_REFRESH_MS / 1000}s
              {backgroundRefreshing ? " · refreshing" : ""}
            </span>
            {lastUpdatedAt ? <span>Last updated {formatDateTime(lastUpdatedAt)}</span> : null}
          </div>

          <section className="test-map-routebar" aria-label="Destination route summary">
            <div>
              <span className="material-symbols-outlined">flag</span>
              <div>
                <strong>Destination Hub</strong>
                <span>{DESTINATION.lat.toFixed(6)}, {DESTINATION.lng.toFixed(6)}</span>
              </div>
            </div>
            <dl>
              <div>
                <dt>Distance Left</dt>
                <dd>{routeSummary?.distanceText ?? "-"}</dd>
              </div>
              <div>
                <dt>ETA</dt>
                <dd>{routeSummary?.durationText ?? "-"}</dd>
              </div>
            </dl>
            {routeError ? <p>{routeError}</p> : null}
          </section>

          {error ? (
            <p className="customer-form-message customer-form-message--error" role="alert">
              {error}
            </p>
          ) : null}

          <div className="test-map-grid">
            <section className="test-map-surface" aria-label="Vehicle map">
              <div ref={mapContainerRef} className="test-map-canvas" />
              {mapLoading || mapError ? (
                <div className="test-map-overlay">
                  {mapError || "Loading map..."}
                </div>
              ) : null}
            </section>

            <aside className="test-map-details" aria-label="Location details">
              <header>
                <span className="material-symbols-outlined">pin_drop</span>
                <div>
                  <h2>{location?.unitName ?? "No unit loaded"}</h2>
                  <p>{location ? `Unit ${location.wialonUnitId}` : "Search to load data"}</p>
                </div>
              </header>

              {location ? (
                <>
                  <dl className="test-map-metrics">
                    <div>
                      <dt>Latitude</dt>
                      <dd>{location.latitude.toFixed(6)}</dd>
                    </div>
                    <div>
                      <dt>Longitude</dt>
                      <dd>{location.longitude.toFixed(6)}</dd>
                    </div>
                    <div>
                      <dt>Speed</dt>
                      <dd>{location.speedKph.toFixed(1)} km/h</dd>
                    </div>
                    <div>
                      <dt>Heading</dt>
                      <dd>{location.headingDegrees.toFixed(1)}°</dd>
                    </div>
                    <div>
                      <dt>Visual Mode</dt>
                      <dd>
                        {visualMotionMode === "reported"
                          ? "Predicting from speed"
                          : visualMotionMode === "inferred"
                            ? "Predicting from movement"
                            : "Parked"}
                      </dd>
                    </div>
                  </dl>

                  <dl className="test-map-address">
                    <div>
                      <dt>Address</dt>
                      <dd>{location.mapDetails?.formattedAddress ?? "Not available"}</dd>
                    </div>
                    <div>
                      <dt>City</dt>
                      <dd>{city ?? "-"}</dd>
                    </div>
                    <div>
                      <dt>Region</dt>
                      <dd>{region ?? "-"}</dd>
                    </div>
                    <div>
                      <dt>Country</dt>
                      <dd>{country ?? "-"}</dd>
                    </div>
                    <div>
                      <dt>Place ID</dt>
                      <dd>{location.mapDetails?.placeId ?? "-"}</dd>
                    </div>
                    <div>
                      <dt>Recorded</dt>
                      <dd>{formatDateTime(location.recordedAt)}</dd>
                    </div>
                  </dl>
                </>
              ) : (
                <p className="test-map-empty">Load a unit to see map and address details.</p>
              )}
            </aside>
          </div>
        </section>
      </div>
    </main>
  );
}

async function snapToNearestRoad(position: { lat: number; lng: number }) {
  if (!googleMapsConfig.apiKey) {
    return position;
  }

  const params = new URLSearchParams({
    points: `${position.lat},${position.lng}`,
    key: googleMapsConfig.apiKey,
  });

  try {
    const response = await fetch(
      `https://roads.googleapis.com/v1/nearestRoads?${params.toString()}`
    );
    if (!response.ok) {
      return position;
    }

    const body = (await response.json()) as {
      snappedPoints?: Array<{
        location?: {
          latitude?: number;
          longitude?: number;
        };
      }>;
    };
    const snappedLocation = body.snappedPoints?.[0]?.location;
    if (
      typeof snappedLocation?.latitude !== "number" ||
      typeof snappedLocation?.longitude !== "number"
    ) {
      return position;
    }

    return {
      lat: snappedLocation.latitude,
      lng: snappedLocation.longitude,
    };
  } catch {
    return position;
  }
}
