export const googleMapsConfig = {
  apiKey: import.meta.env.VITE_GOOGLE_MAPS_API_KEY ?? "",
};

let googleMapsScriptPromise: Promise<void> | null = null;

export function loadGoogleMapsScript() {
  if (typeof window === "undefined") {
    return Promise.reject(new Error("Google Maps can only be loaded in a browser."));
  }

  if (window.google?.maps) {
    return Promise.resolve();
  }

  if (!googleMapsConfig.apiKey) {
    return Promise.reject(new Error("Google Maps API key is not configured."));
  }

  if (googleMapsScriptPromise) {
    return googleMapsScriptPromise;
  }

  googleMapsScriptPromise = new Promise<void>((resolve, reject) => {
    const callbackName = `porterGoogleMapsReady_${Date.now()}`;
    const script = document.createElement("script");

    window[callbackName] = () => {
      delete window[callbackName];
      resolve();
    };

    script.onerror = () => {
      delete window[callbackName];
      googleMapsScriptPromise = null;
      reject(new Error("Could not load Google Maps."));
    };

    const params = new URLSearchParams({
      key: googleMapsConfig.apiKey,
      libraries: "places,maps,routes,geocoding",
      callback: callbackName,
      loading: "async",
      v: "weekly",
    });

    script.async = true;
    script.defer = true;
    script.src = `https://maps.googleapis.com/maps/api/js?${params.toString()}`;
    document.head.appendChild(script);
  });

  return googleMapsScriptPromise;
}
