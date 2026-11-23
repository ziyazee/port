import {
  useEffect,
  useRef,
  useState,
  type ChangeEvent,
  type FormEvent,
  type ReactNode,
} from "react";
import { apiClient } from "@/services/apiClient";
import { loadGoogleMapsScript } from "@/config/googleMaps";

type OrderFormProps = {
  onCancel: () => void;
  onCreated?: (orderId: number) => void;
};

type CustomerAccountOption = {
  id: number;
  companyName: string;
};

type VehicleType = "LOWBED" | "FLATBED" | "CURTAIN_TRAILER" | "REFER_TRUCK";
type VehicleStatus = "AVAILABLE" | "ASSIGNED" | "MAINTENANCE" | "INACTIVE";
type CargoSpecification = "REGULAR_CARGO" | "OVERSIZE_CARGO";

type VehicleAvailability = {
  vehicleType: VehicleType;
  availableCount: number;
  assignedCount: number;
  maintenanceCount: number;
  inactiveCount: number;
};

type VehicleResponse = {
  id: number;
  fleetNumber: string;
  plateNumber: string;
  vehicleType: VehicleType;
  status: VehicleStatus;
};

type OrderResponse = {
  id: number;
  companyName: string;
  assignedVehicles: number;
  orderStatus: string;
};

type OrderFormState = {
  customerAccountId: string;
  fromLocation: string;
  toLocation: string;
  vehicleType: VehicleType;
  loadDescription: string;
  cargoSpecification: CargoSpecification;
  cargoHeight: string;
  cargoWidth: string;
  cargoLength: string;
  cargoWeight: string;
  quantityUnits: string;
};

type ManualVehicleRow = {
  id: number;
  selected: boolean;
  vehicleNumber: string;
  vehicleType: VehicleType;
};

type Coordinates = {
  lat: number;
  lng: number;
};

type RouteSummary = {
  distance: string;
  duration: string;
};

type OrderFormDraft = {
  form: OrderFormState;
  manualVehicles: ManualVehicleRow[];
  selectedVehicleIds: number[];
};

type VehicleTypeMeta = {
  label: string;
  summary: string;
  icon: string;
};

const vehicleTypeMeta: Record<VehicleType, VehicleTypeMeta> = {
  LOWBED: {
    label: "Lowbed",
    summary: "Heavy and oversized cargo",
    icon: "rv_hookup",
  },
  FLATBED: {
    label: "Flatbed",
    summary: "Standard open-deck cargo",
    icon: "local_shipping",
  },
  CURTAIN_TRAILER: {
    label: "Curtain Trailer",
    summary: "Side loading and protected loads",
    icon: "garage_home",
  },
  REFER_TRUCK: {
    label: "Reefer Truck",
    summary: "Temperature controlled freight",
    icon: "ac_unit",
  },
};

const vehicleTypes = Object.keys(vehicleTypeMeta) as VehicleType[];

const recentPickupLocations = ["Dammam Industrial", "King Fahd Port", "Jubail 2"];
const recentDestinationLocations = ["Riyadh Dry Port", "NEOM Hub 1", "Al-Kharj Road"];

const initialForm: OrderFormState = {
  customerAccountId: "",
  fromLocation: "",
  toLocation: "",
  vehicleType: "LOWBED",
  loadDescription: "Steel Pipes",
  cargoSpecification: "REGULAR_CARGO",
  cargoHeight: "0",
  cargoWidth: "0",
  cargoLength: "0",
  cargoWeight: "0",
  quantityUnits: "0",
};

const orderDraftStorageKey = "porter:create-order:draft";

function readOrderDraft(): Partial<OrderFormDraft> | null {
  if (typeof window === "undefined") {
    return null;
  }

  const rawDraft = window.localStorage.getItem(orderDraftStorageKey);
  if (!rawDraft) {
    return null;
  }

  try {
    return JSON.parse(rawDraft) as Partial<OrderFormDraft>;
  } catch {
    window.localStorage.removeItem(orderDraftStorageKey);
    return null;
  }
}

function clearOrderDraft() {
  if (typeof window !== "undefined") {
    window.localStorage.removeItem(orderDraftStorageKey);
  }
}

function Section({
  title,
  icon,
  step,
  tone,
  actions,
  children,
}: {
  title: string;
  icon: string;
  step?: string;
  tone?: string;
  actions?: ReactNode;
  children: ReactNode;
}) {
  return (
    <section className={`order-section${tone ? ` order-section--${tone}` : ""}`}>
      <div className="order-section__heading">
        {step ? <span className="order-section__step">{step}</span> : null}
        <h2>
          <span className="material-symbols-outlined order-section__icon">
            {icon}
          </span>
          {title}
        </h2>
        {actions}
      </div>
      {children}
    </section>
  );
}

function Field({
  label,
  children,
}: {
  label: string;
  children: ReactNode;
}) {
  return (
    <label className="order-field">
      <span>{label}</span>
      {children}
    </label>
  );
}

function vehicleDistance(vehicle: VehicleResponse, index: number) {
  if (vehicle.status !== "AVAILABLE") {
    return "Unavailable";
  }
  return `${(4.2 + index * 2.1).toFixed(1)} km`;
}

function vehicleAvailabilityTime(vehicle: VehicleResponse, index: number) {
  if (vehicle.status === "AVAILABLE" && index === 0) {
    return "Immediate";
  }
  if (vehicle.status === "AVAILABLE") {
    return `${index * 15 + 15} mins`;
  }
  if (vehicle.status === "ASSIGNED") {
    return "Assigned";
  }
  if (vehicle.status === "MAINTENANCE") {
    return "In maintenance";
  }
  return "Inactive";
}

function vehicleLocation(index: number) {
  const locations = [
    "Riyadh South Industrial",
    "Al-Kharj Road",
    "Dammam Port",
    "Jubail Logistics Yard",
    "King Fahd Port",
  ];
  return locations[index % locations.length];
}

function statusLabel(status: VehicleStatus) {
  return status
    .toLowerCase()
    .replace("_", " ")
    .replace(/^\w/, (letter) => letter.toUpperCase());
}

function placeAddress(place: any) {
  return (
    place.formattedAddress ??
    place.formatted_address ??
    place.displayName?.text ??
    place.displayName ??
    ""
  );
}

function placeCoordinates(place: any): Coordinates | null {
  const location = place.location ?? place.geometry?.location;
  if (!location) {
    return null;
  }

  const lat = typeof location.lat === "function" ? location.lat() : location.lat;
  const lng = typeof location.lng === "function" ? location.lng() : location.lng;

  if (typeof lat !== "number" || typeof lng !== "number") {
    return null;
  }

  return { lat, lng };
}

export function OrderForm({ onCancel, onCreated }: OrderFormProps) {
  const mapContainerRef = useRef<HTMLDivElement | null>(null);
  const pickupInputRef = useRef<HTMLInputElement | null>(null);
  const destinationInputRef = useRef<HTMLInputElement | null>(null);
  const mapRef = useRef<any>(null);
  const geocoderRef = useRef<any>(null);
  const directionsServiceRef = useRef<any>(null);
  const directionsRendererRef = useRef<any>(null);
  const [initialDraft] = useState(() => readOrderDraft());
  const [form, setForm] = useState<OrderFormState>(() => ({
    ...initialForm,
    ...initialDraft?.form,
  }));
  const [customers, setCustomers] = useState<CustomerAccountOption[]>([]);
  const [availability, setAvailability] = useState<VehicleAvailability[]>([]);
  const [vehicles, setVehicles] = useState<VehicleResponse[]>([]);
  const [allVehicles, setAllVehicles] = useState<VehicleResponse[]>([]);
  const [loadingSetup, setLoadingSetup] = useState(true);
  const [loadingVehicles, setLoadingVehicles] = useState(false);
  const [loadingAllVehicles, setLoadingAllVehicles] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [showAvailableVehicles, setShowAvailableVehicles] = useState(false);
  const [showAllVehicles, setShowAllVehicles] = useState(false);
  const [showManualVehicles, setShowManualVehicles] = useState(false);
  const [manualVehicles, setManualVehicles] = useState<ManualVehicleRow[]>(
    initialDraft?.manualVehicles ?? []
  );
  const [selectedVehicleIds, setSelectedVehicleIds] = useState<number[]>(
    initialDraft?.selectedVehicleIds ?? []
  );
  const [pickupCoordinates, setPickupCoordinates] = useState<Coordinates | null>(null);
  const [destinationCoordinates, setDestinationCoordinates] =
    useState<Coordinates | null>(null);
  const [mapsReady, setMapsReady] = useState(false);
  const [mapsError, setMapsError] = useState("");
  const [routeError, setRouteError] = useState("");
  const [routeSummary, setRouteSummary] = useState<RouteSummary | null>(null);
  const [error, setError] = useState("");
  const [createdOrder, setCreatedOrder] = useState<OrderResponse | null>(null);

  useEffect(() => {
    if (typeof window === "undefined") {
      return;
    }

    const draft: OrderFormDraft = {
      form,
      manualVehicles,
      selectedVehicleIds,
    };

    window.localStorage.setItem(orderDraftStorageKey, JSON.stringify(draft));
  }, [
    form,
    manualVehicles,
    selectedVehicleIds,
  ]);

  useEffect(() => {
    let ignore = false;

    async function loadOrderData() {
      try {
        const [customerResponse, availabilityResponse] = await Promise.all([
          apiClient.get<CustomerAccountOption[]>("/customer-accounts"),
          apiClient.get<VehicleAvailability[]>("/vehicles/availability"),
        ]);

        if (!ignore) {
          setCustomers(customerResponse);
          setAvailability(availabilityResponse);
        }
      } catch (exception) {
        if (!ignore) {
          setError(
            exception instanceof Error
              ? exception.message
              : "Could not load order setup data."
          );
        }
      } finally {
        if (!ignore) {
          setLoadingSetup(false);
        }
      }
    }

    loadOrderData();

    return () => {
      ignore = true;
    };
  }, []);

  useEffect(() => {
    let ignore = false;

    function updateRouteSummaryFromDirections(directions: any) {
      const leg = directions?.routes?.[0]?.legs?.[0];
      if (!leg) {
        setRouteSummary(null);
        return;
      }

      setRouteSummary({
        distance: leg.distance?.text ?? "-",
        duration: leg.duration?.text ?? "-",
      });
    }

    async function initializeGoogleMaps() {
      try {
        await loadGoogleMapsScript();

        const [mapsLibrary, placesLibrary, routesLibrary, geocodingLibrary] = await Promise.all([
          window.google.maps.importLibrary("maps"),
          window.google.maps.importLibrary("places"),
          window.google.maps.importLibrary("routes"),
          window.google.maps.importLibrary("geocoding"),
        ]);

        if (ignore || !mapContainerRef.current) {
          return;
        }

        const map = new mapsLibrary.Map(mapContainerRef.current, {
          center: { lat: 24.7136, lng: 46.6753 },
          fullscreenControl: false,
          mapTypeControl: false,
          streetViewControl: false,
          zoom: 6,
        });

        const directionsRenderer = new routesLibrary.DirectionsRenderer({
          draggable: true,
          map,
          polylineOptions: {
            strokeColor: "#2563eb",
            strokeOpacity: 0.95,
            strokeWeight: 5,
          },
          suppressMarkers: false,
        });

        directionsRenderer.addListener("directions_changed", () => {
          updateRouteSummaryFromDirections(directionsRenderer.getDirections());
        });

        mapRef.current = map;
        geocoderRef.current = new geocodingLibrary.Geocoder();
        directionsServiceRef.current = new routesLibrary.DirectionsService();
        directionsRendererRef.current = directionsRenderer;

        window.requestAnimationFrame(() => {
          window.google.maps.event?.trigger?.(map, "resize");
          map.setCenter({ lat: 24.7136, lng: 46.6753 });
        });

        function mountAutocomplete(
          input: HTMLInputElement | null,
          field: "fromLocation" | "toLocation",
          setCoordinates: (coordinates: Coordinates | null) => void
        ) {
          if (!input) {
            return;
          }

          const autocomplete = new placesLibrary.Autocomplete(input, {
            fields: [
              "address_components",
              "formatted_address",
              "geometry",
              "name",
              "place_id",
            ],
            types: ["geocode", "establishment"],
          });

          autocomplete.addListener("place_changed", () => {
            const place = autocomplete.getPlace();
            const address = placeAddress(place);
            const coordinates = placeCoordinates(place);

            if (address) {
              updateField(field, address);
            }
            setRouteSummary(null);
            setRouteError("");
            setCoordinates(coordinates);
          });
        }

        mountAutocomplete(pickupInputRef.current, "fromLocation", setPickupCoordinates);
        mountAutocomplete(
          destinationInputRef.current,
          "toLocation",
          setDestinationCoordinates
        );

        setMapsReady(true);
      } catch (exception) {
        if (!ignore) {
          setMapsError(
            exception instanceof Error
              ? exception.message
              : "Could not load Google Maps."
          );
        }
      }
    }

    initializeGoogleMaps();

    return () => {
      ignore = true;
    };
  }, []);

  useEffect(() => {
    if (
      !mapsReady ||
      !pickupCoordinates ||
      !destinationCoordinates ||
      !directionsServiceRef.current ||
      !directionsRendererRef.current
    ) {
      return;
    }

    directionsServiceRef.current.route(
      {
        destination: destinationCoordinates,
        origin: pickupCoordinates,
        travelMode: window.google.maps.TravelMode.DRIVING,
      },
      (result: any, status: string) => {
        if (status === window.google.maps.DirectionsStatus.OK && result) {
          setRouteError("");
          directionsRendererRef.current.setDirections(result);
        } else {
          setRouteSummary(null);
          setRouteError(`Could not render route: ${status}`);
        }
      }
    );
  }, [destinationCoordinates, mapsReady, pickupCoordinates]);

  useEffect(() => {
    let ignore = false;

    async function loadVehicles() {
      setLoadingVehicles(true);
      try {
        const response = await apiClient.get<VehicleResponse[]>(
          `/vehicles?vehicleType=${form.vehicleType}`
        );
        if (!ignore) {
          setVehicles(response);
        }
      } catch (exception) {
        if (!ignore) {
          setError(
            exception instanceof Error
              ? exception.message
              : "Could not load vehicles."
          );
        }
      } finally {
        if (!ignore) {
          setLoadingVehicles(false);
        }
      }
    }

    loadVehicles();

    return () => {
      ignore = true;
    };
  }, [form.vehicleType]);

  useEffect(() => {
    if ((!showAllVehicles && !showManualVehicles) || allVehicles.length > 0) {
      return;
    }

    let ignore = false;

    async function loadAllVehicles() {
      setLoadingAllVehicles(true);
      setError("");

      try {
        const response = await apiClient.get<VehicleResponse[]>("/vehicles");
        if (!ignore) {
          setAllVehicles(response);
        }
      } catch (exception) {
        if (!ignore) {
          setError(
            exception instanceof Error
              ? exception.message
              : "Could not load all vehicles."
          );
        }
      } finally {
        if (!ignore) {
          setLoadingAllVehicles(false);
        }
      }
    }

    loadAllVehicles();

    return () => {
      ignore = true;
    };
  }, [allVehicles.length, showAllVehicles, showManualVehicles]);

  const cargoDimensions = [
    form.cargoLength ? `${form.cargoLength} cm L` : "",
    form.cargoWidth ? `${form.cargoWidth} cm W` : "",
    form.cargoHeight ? `${form.cargoHeight} cm H` : "",
  ]
    .filter(Boolean)
    .join(" x ");
  const availableVehicles = vehicles.filter(
    (vehicle) => vehicle.status === "AVAILABLE"
  );
  const availableVehicleIds = new Set(availableVehicles.map((vehicle) => vehicle.id));
  const remainingVehicles = allVehicles.filter(
    (vehicle) => !availableVehicleIds.has(vehicle.id)
  );

  function updateField(field: keyof OrderFormState, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
    if (field === "vehicleType") {
      setShowAvailableVehicles(false);
      setShowAllVehicles(false);
      setShowManualVehicles(false);
      setSelectedVehicleIds([]);
    }
    setCreatedOrder(null);
  }

  function updateInput(field: keyof OrderFormState) {
    return (event: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) =>
      updateField(field, event.target.value);
  }

  function geocodeLocation(
    location: string,
    setCoordinates: (coordinates: Coordinates | null) => void
  ) {
    const address = location.trim();

    if (!address || !mapsReady || !geocoderRef.current) {
      return;
    }

    geocoderRef.current.geocode(
      { address },
      (results: any[] | null, status: string) => {
        const okStatus = window.google?.maps?.GeocoderStatus?.OK ?? "OK";

        if (status === okStatus && results?.[0]) {
          const coordinates = placeCoordinates(results[0]);
          setCoordinates(coordinates);
          setRouteError("");

          if (coordinates && mapRef.current) {
            mapRef.current.panTo(coordinates);
          }
          return;
        }

        setCoordinates(null);
        setRouteSummary(null);
        setRouteError("Could not find this location. Select a Google Maps suggestion.");
      }
    );
  }

  function selectPickupLocation(location: string) {
    setPickupCoordinates(null);
    setRouteSummary(null);
    updateField("fromLocation", location);
    geocodeLocation(location, setPickupCoordinates);
  }

  function selectDestinationLocation(location: string) {
    setDestinationCoordinates(null);
    setRouteSummary(null);
    updateField("toLocation", location);
    geocodeLocation(location, setDestinationCoordinates);
  }

  function showAllVehicleTable() {
    setShowAllVehicles(true);
  }

  function showManualVehicleTable() {
    setShowManualVehicles(true);
    if (manualVehicles.length === 0) {
      setManualVehicles([
        {
          id: Date.now(),
          selected: true,
          vehicleNumber: "",
          vehicleType: form.vehicleType,
        },
      ]);
    }
  }

  function updateManualVehicle(
    id: number,
    field: keyof Omit<ManualVehicleRow, "id">,
    value: string | boolean
  ) {
    setManualVehicles((current) =>
      current.map((vehicle) =>
        vehicle.id === id ? { ...vehicle, [field]: value } : vehicle
      )
    );
  }

  function addManualVehicleRow() {
    setManualVehicles((current) => [
      ...current,
      {
        id: Date.now(),
        selected: true,
        vehicleNumber: "",
        vehicleType: form.vehicleType,
      },
    ]);
  }

  function toggleVehicleSelection(vehicleId: number, selected: boolean) {
    setSelectedVehicleIds((current) => {
      if (selected) {
        return current.includes(vehicleId) ? current : [...current, vehicleId];
      }

      return current.filter((id) => id !== vehicleId);
    });
  }

  function removeAssignedVehicles(vehicleIds: number[]) {
    if (vehicleIds.length === 0) {
      return;
    }

    setVehicles((current) => current.filter((vehicle) => !vehicleIds.includes(vehicle.id)));
    setAllVehicles((current) =>
      current.map((vehicle) =>
        vehicleIds.includes(vehicle.id) ? { ...vehicle, status: "ASSIGNED" } : vehicle
      )
    );
    setSelectedVehicleIds([]);
  }

  async function resolveVehicleIdsToAssign() {
    const selectedManualVehicles = manualVehicles.filter((vehicle) => vehicle.selected);
    if (selectedManualVehicles.length === 0) {
      return [...selectedVehicleIds];
    }

    const missingVehicleNumber = selectedManualVehicles.some(
      (vehicle) => !vehicle.vehicleNumber.trim()
    );
    if (missingVehicleNumber) {
      throw new Error("Enter a fleet or plate number for every selected manual vehicle.");
    }

    const registeredVehicles =
      allVehicles.length > 0
        ? allVehicles
        : await apiClient.get<VehicleResponse[]>("/vehicles");
    if (allVehicles.length === 0) {
      setAllVehicles(registeredVehicles);
    }

    const manualVehicleIds = selectedManualVehicles.map((manualVehicle) => {
      const vehicleNumber = manualVehicle.vehicleNumber.trim().toLocaleLowerCase();
      const matches = registeredVehicles.filter(
        (vehicle) =>
          vehicle.fleetNumber.trim().toLocaleLowerCase() === vehicleNumber ||
          vehicle.plateNumber.trim().toLocaleLowerCase() === vehicleNumber
      );

      if (matches.length === 0) {
        throw new Error(
          `Vehicle "${manualVehicle.vehicleNumber.trim()}" is not registered in the fleet.`
        );
      }
      if (matches.length > 1) {
        throw new Error(
          `Vehicle number "${manualVehicle.vehicleNumber.trim()}" is ambiguous. Select it from the fleet table.`
        );
      }

      const vehicle = matches[0];
      if (vehicle.status !== "AVAILABLE") {
        throw new Error(
          `Vehicle "${manualVehicle.vehicleNumber.trim()}" is ${statusLabel(
            vehicle.status
          ).toLowerCase()} and cannot be assigned.`
        );
      }
      if (vehicle.vehicleType !== manualVehicle.vehicleType) {
        throw new Error(
          `Vehicle "${manualVehicle.vehicleNumber.trim()}" is registered as ${
            vehicleTypeMeta[vehicle.vehicleType].label
          }.`
        );
      }

      return vehicle.id;
    });

    return [...new Set([...selectedVehicleIds, ...manualVehicleIds])];
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError("");
    setCreatedOrder(null);

    if (
      !form.fromLocation.trim() ||
      !form.toLocation.trim() ||
      !pickupCoordinates ||
      !destinationCoordinates
    ) {
      setSubmitting(false);
      setError("Select pickup and destination locations from Google Maps to enable tracking.");
      return;
    }

    try {
      const vehicleIdsToAssign = await resolveVehicleIdsToAssign();
      const response = await apiClient.post<OrderResponse>("/orders", {
        customerAccountId: Number(form.customerAccountId),
        fromLocation: form.fromLocation,
        toLocation: form.toLocation,
        pickupLatitude: pickupCoordinates.lat,
        pickupLongitude: pickupCoordinates.lng,
        dropLatitude: destinationCoordinates.lat,
        dropLongitude: destinationCoordinates.lng,
        vehicleType: form.vehicleType,
        loadDescription: form.loadDescription,
        cargoSpecification: form.cargoSpecification,
        cargoHeight: form.cargoHeight || null,
        cargoWeight: form.cargoWeight || null,
        cargoDimensions: cargoDimensions || null,
        quantityUnits: Math.max(1, Number(form.quantityUnits) || 0),
        vehicleIds: vehicleIdsToAssign,
        createdBy: "operations",
      });

      removeAssignedVehicles(vehicleIdsToAssign);
      setCreatedOrder({
        ...response,
        assignedVehicles: response.assignedVehicles,
        orderStatus: response.orderStatus,
      });
      clearOrderDraft();
      onCreated?.(response.id);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "Could not create order.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="order-workspace" onSubmit={submit}>
      <div className="order-workspace__scroll">
        <div className="order-layout-grid">
          <Section title="Customer Details" icon="corporate_fare" step="01" tone="customer">
            <div className="order-customer-panel">
              <Field label="Select Company">
                <div className="order-input-icon">
                  <span className="material-symbols-outlined">search</span>
                  <select
                    required
                    disabled={loadingSetup}
                    value={form.customerAccountId}
                    onChange={updateInput("customerAccountId")}
                  >
                    <option value="" disabled>
                      Select a company
                    </option>
                    {customers.map((customer) => (
                      <option key={customer.id} value={customer.id}>
                        {customer.companyName}
                      </option>
                    ))}
                  </select>
                </div>
              </Field>
              <div className="order-chip-row" aria-label="Suggested customers">
                {customers.slice(0, 4).map((customer) => (
                  <button
                    key={customer.id}
                    type="button"
                    onClick={() => updateField("customerAccountId", String(customer.id))}
                  >
                    {customer.companyName}
                  </button>
                ))}
              </div>
            </div>
          </Section>

          <Section title="Transport Details" icon="route" step="02" tone="route">
            <div className="order-location-grid">
              <div className="order-location-card">
                <Field label="Pickup Location">
                  <div className="order-input-icon order-input-icon--pickup">
                    <span className="material-symbols-outlined">location_on</span>
                    <input
                      ref={pickupInputRef}
                      required
                      placeholder="Enter pickup address or facility code"
                      value={form.fromLocation}
                      onChange={(event) => {
                        setPickupCoordinates(null);
                        setRouteSummary(null);
                        updateField("fromLocation", event.target.value);
                      }}
                      onBlur={() => geocodeLocation(form.fromLocation, setPickupCoordinates)}
                      onKeyDown={(event) => {
                        if (event.key === "Enter") {
                          event.preventDefault();
                          geocodeLocation(form.fromLocation, setPickupCoordinates);
                        }
                      }}
                    />
                  </div>
                </Field>
                <div className="order-chip-row order-chip-row--muted">
                  <span>Recent:</span>
                  {recentPickupLocations.map((location) => (
                    <button
                      key={location}
                      type="button"
                      onClick={() => selectPickupLocation(location)}
                    >
                      {location}
                    </button>
                  ))}
                </div>
              </div>

              <div className="order-location-card">
                <Field label="Destination Location">
                  <div className="order-input-icon order-input-icon--drop">
                    <span className="material-symbols-outlined">fmd_bad</span>
                    <input
                      ref={destinationInputRef}
                      required
                      placeholder="Enter destination address"
                      value={form.toLocation}
                      onChange={(event) => {
                        setDestinationCoordinates(null);
                        setRouteSummary(null);
                        updateField("toLocation", event.target.value);
                      }}
                      onBlur={() =>
                        geocodeLocation(form.toLocation, setDestinationCoordinates)
                      }
                      onKeyDown={(event) => {
                        if (event.key === "Enter") {
                          event.preventDefault();
                          geocodeLocation(form.toLocation, setDestinationCoordinates);
                        }
                      }}
                    />
                  </div>
                </Field>
                <div className="order-chip-row order-chip-row--muted">
                  <span>Recent:</span>
                  {recentDestinationLocations.map((location) => (
                    <button
                      key={location}
                      type="button"
                      onClick={() => selectDestinationLocation(location)}
                    >
                      {location}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </Section>

          <section className="order-section order-map-placeholder" aria-label="Map preview">
            <div className="order-map-canvas" ref={mapContainerRef} />
            {!mapsReady ? (
              <div className="order-map-state">
                {mapsError || "Loading Google Maps..."}
              </div>
            ) : null}
            {mapsReady && !routeSummary ? (
              <div className="order-map-state order-map-state--hint">
                {routeError || "Select pickup and destination to preview route."}
              </div>
            ) : null}
            {routeSummary ? (
              <div className="order-route-summary">
                <span>{routeSummary.distance}</span>
                <strong>{routeSummary.duration}</strong>
              </div>
            ) : null}
          </section>

          <section className="order-section order-load-vehicle-section">
            <div className="order-load-vehicle-layout">
              <div className="order-load-grid">
                <h3 className="order-subsection-title">Load Information</h3>
                <textarea
                  className="order-load-description"
                  required
                  placeholder="Please write full description of the load details.."
                  value={form.loadDescription}
                  onChange={updateInput("loadDescription")}
                />

                <div className="order-dimension-grid">
                  <Field label="Height (cm)">
                    <input
                      min="0"
                      type="number"
                      value={form.cargoHeight}
                      onChange={updateInput("cargoHeight")}
                    />
                  </Field>
                  <Field label="Width (cm)">
                    <input
                      min="0"
                      type="number"
                      value={form.cargoWidth}
                      onChange={updateInput("cargoWidth")}
                    />
                  </Field>
                  <Field label="Length (cm)">
                    <input
                      min="0"
                      type="number"
                      value={form.cargoLength}
                      onChange={updateInput("cargoLength")}
                    />
                  </Field>
                  <Field label="Weight (kg)">
                    <input
                      min="0"
                      type="number"
                      value={form.cargoWeight}
                      onChange={updateInput("cargoWeight")}
                    />
                  </Field>
                </div>
              </div>

              <div className="order-vehicle-requirement-panel">
                <h3 className="order-subsection-title">Vehicle Requirement</h3>
                <div className="order-vehicle-grid">
                  {vehicleTypes.map((vehicleType) => {
                    const meta = vehicleTypeMeta[vehicleType];
                    const active = form.vehicleType === vehicleType;

                    return (
                      <button
                        className={`order-vehicle-card${
                          active ? " order-vehicle-card--active" : ""
                        }`}
                        key={vehicleType}
                        type="button"
                        onClick={() => updateField("vehicleType", vehicleType)}
                      >
                        <span className="material-symbols-outlined order-vehicle-card__icon">
                          {meta.icon}
                        </span>
                        <span className="order-vehicle-card__copy">
                          <strong>{meta.label}</strong>
                          <small>{meta.summary}</small>
                        </span>
                        <span className="order-vehicle-card__qty">
                          <input
                            aria-label={`${meta.label} quantity`}
                            min="0"
                            type="number"
                            value={active ? form.quantityUnits : "0"}
                            onChange={(event) => {
                              updateField("vehicleType", vehicleType);
                              updateField("quantityUnits", event.target.value);
                            }}
                            onClick={(event) => event.stopPropagation()}
                          />
                        </span>
                      </button>
                    );
                  })}
                </div>
              </div>
            </div>
          </section>

          {!showAvailableVehicles ? (
            <div className="order-vehicle-search order-vehicle-search--standalone">
              <button
                className="order-vehicle-search__button"
                type="button"
                onClick={() => setShowAvailableVehicles(true)}
              >
                <span className="material-symbols-outlined">search</span>
                Search Available Vehicles
              </button>
            </div>
          ) : null}

          {showAvailableVehicles ? (
            <Section title="Available Vehicles" icon="local_shipping" step="05" tone="fleet">
              {error ? (
                <p className="customer-form-message customer-form-message--error" role="alert">
                  {error}
                </p>
              ) : null}

              {createdOrder ? (
                <p className="customer-form-message customer-form-message--success" role="status">
                  Order #{createdOrder.id} created for {createdOrder.companyName}.
                </p>
              ) : null}

              <div className="order-table-wrap">
                <table className="order-vehicle-table">
                  <thead>
                    <tr>
                      <th>Select</th>
                      <th>Vehicle Type</th>
                      <th>Fleet / Plate</th>
                      <th>Distance from Source</th>
                      <th>Available At</th>
                    </tr>
                  </thead>
                  <tbody>
                    {loadingVehicles ? (
                      <tr>
                        <td colSpan={5}>Loading vehicles...</td>
                      </tr>
                    ) : null}
                    {!loadingVehicles && availableVehicles.length === 0 ? (
                      <tr>
                        <td colSpan={5}>No vehicles found for this type.</td>
                      </tr>
                    ) : null}
                    {!loadingVehicles
                      ? availableVehicles.map((vehicle, index) => (
                          <tr key={vehicle.id}>
                            <td>
                              <label className="order-vehicle-check">
                                <input
                                  aria-label={`Select ${vehicle.fleetNumber}`}
                                  checked={selectedVehicleIds.includes(vehicle.id)}
                                  type="checkbox"
                                  onChange={(event) =>
                                    toggleVehicleSelection(vehicle.id, event.target.checked)
                                  }
                                />
                                <span className="material-symbols-outlined">check</span>
                              </label>
                            </td>
                            <td>
                              <strong>{vehicleTypeMeta[vehicle.vehicleType].label}</strong>
                            </td>
                            <td>
                              <span className="order-fleet-number">
                                {vehicle.fleetNumber}
                              </span>
                              <span>{vehicle.plateNumber}</span>
                            </td>
                            <td>{vehicleDistance(vehicle, index)}</td>
                            <td>{vehicleAvailabilityTime(vehicle, index)}</td>
                          </tr>
                        ))
                      : null}
                  </tbody>
                </table>
              </div>

              {!showAllVehicles ? (
                <div className="order-table-extension">
                  <span>Need to compare the wider fleet?</span>
                  <button type="button" onClick={showAllVehicleTable}>
                    Show All Vehicles
                  </button>
                </div>
              ) : null}

              {showAllVehicles ? (
                <div className="order-all-vehicles">
                  <div className="order-all-vehicles__heading">
                    <h3>Other Vehicles</h3>
                    <span>{remainingVehicles.length} vehicles</span>
                  </div>
                  <div className="order-table-wrap">
                    <table className="order-vehicle-table">
                      <thead>
                        <tr>
                          <th>Select</th>
                          <th>Vehicle Type</th>
                          <th>Fleet / Plate</th>
                          <th>Distance from Source</th>
                          <th>Available At</th>
                          <th>Available Location</th>
                          <th>Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {loadingAllVehicles ? (
                          <tr>
                            <td colSpan={7}>Loading all vehicles...</td>
                          </tr>
                        ) : null}
                        {!loadingAllVehicles && remainingVehicles.length === 0 ? (
                          <tr>
                            <td colSpan={7}>No other vehicles found.</td>
                          </tr>
                        ) : null}
                        {!loadingAllVehicles
                          ? remainingVehicles.map((vehicle, index) => (
                              <tr key={vehicle.id}>
                                <td>
                                  <label className="order-vehicle-check">
                                    <input
                                      aria-label={`Select ${vehicle.fleetNumber}`}
                                      checked={selectedVehicleIds.includes(vehicle.id)}
                                      disabled={vehicle.status !== "AVAILABLE"}
                                      type="checkbox"
                                      onChange={(event) =>
                                        toggleVehicleSelection(vehicle.id, event.target.checked)
                                      }
                                    />
                                    <span className="material-symbols-outlined">
                                      check
                                    </span>
                                  </label>
                                </td>
                                <td>
                                  <strong>
                                    {vehicleTypeMeta[vehicle.vehicleType].label}
                                  </strong>
                                </td>
                                <td>
                                  <span className="order-fleet-number">
                                    {vehicle.fleetNumber}
                                  </span>
                                  <span>{vehicle.plateNumber}</span>
                                </td>
                                <td>{vehicleDistance(vehicle, index)}</td>
                                <td>{vehicleAvailabilityTime(vehicle, index)}</td>
                                <td>{vehicleLocation(index)}</td>
                                <td>
                                  <span
                                    className={`order-status-pill order-status-pill--${vehicle.status.toLowerCase()}`}
                                  >
                                    {statusLabel(vehicle.status)}
                                  </span>
                                </td>
                              </tr>
                            ))
                          : null}
                      </tbody>
                    </table>
                  </div>
                </div>
              ) : null}

              {!showManualVehicles ? (
                <div className="order-table-extension">
                  <span>Vehicle is not listed?</span>
                  <button type="button" onClick={showManualVehicleTable}>
                    Manually Add Vehicle Details
                  </button>
                </div>
              ) : null}

              {showManualVehicles ? (
                <div className="order-all-vehicles">
                  <div className="order-all-vehicles__heading">
                    <h3>Manual Vehicle Details</h3>
                  </div>
                  <div className="order-table-wrap">
                    <table className="order-vehicle-table order-manual-vehicle-table">
                      <thead>
                        <tr>
                          <th>Select</th>
                          <th>Vehicle Number</th>
                          <th>Vehicle Type</th>
                        </tr>
                      </thead>
                      <tbody>
                        {manualVehicles.map((vehicle) => (
                          <tr key={vehicle.id}>
                            <td>
                              <label className="order-vehicle-check">
                                <input
                                  aria-label="Select manual vehicle"
                                  checked={vehicle.selected}
                                  type="checkbox"
                                  onChange={(event) =>
                                    updateManualVehicle(
                                      vehicle.id,
                                      "selected",
                                      event.target.checked
                                    )
                                  }
                                />
                                <span className="material-symbols-outlined">
                                  check
                                </span>
                              </label>
                            </td>
                            <td>
                              <input
                                aria-label="Vehicle number"
                                placeholder="Enter vehicle number"
                                value={vehicle.vehicleNumber}
                                onChange={(event) =>
                                  updateManualVehicle(
                                    vehicle.id,
                                    "vehicleNumber",
                                    event.target.value
                                  )
                                }
                              />
                            </td>
                            <td>
                              <select
                                aria-label="Vehicle type"
                                value={vehicle.vehicleType}
                                onChange={(event) =>
                                  updateManualVehicle(
                                    vehicle.id,
                                    "vehicleType",
                                    event.target.value as VehicleType
                                  )
                                }
                              >
                                {vehicleTypes.map((vehicleType) => (
                                  <option key={vehicleType} value={vehicleType}>
                                    {vehicleTypeMeta[vehicleType].label}
                                  </option>
                                ))}
                              </select>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                  <div className="order-table-extension">
                    <span>Add another manual vehicle entry</span>
                    <button type="button" onClick={addManualVehicleRow}>
                      Add One More Row
                    </button>
                  </div>
                </div>
              ) : null}
            </Section>
          ) : null}
        </div>
      </div>

      <footer className="order-action-bar order-action-bar--wide">
        <div className="order-action-bar__left">
          <button
            type="button"
            onClick={() => {
              clearOrderDraft();
              onCancel();
            }}
          >
            Cancel
          </button>
          <button type="button">Save Draft</button>
        </div>
        <div className="order-action-bar__right">
          <button className="order-action-bar__primary" disabled={submitting || loadingSetup} type="submit">
            <span className="material-symbols-outlined">search_check</span>
            {submitting ? "Creating..." : "Create Order"}
          </button>
        </div>
      </footer>
    </form>
  );
}
