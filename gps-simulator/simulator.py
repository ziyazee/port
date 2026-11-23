import json
import math
import os
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone


SPRING_URL = os.environ.get("SPRING_URL", "http://host.docker.internal:8080").rstrip("/")
VEHICLE_FLEET_NUMBER = os.environ.get("VEHICLE_FLEET_NUMBER", "LB-001")
INTERVAL_SECONDS = float(os.environ.get("GPS_INTERVAL_SECONDS", "5"))

# Riyadh-area route. The simulator interpolates between these points and loops.
ROUTE = [
    (24.7136, 46.6753),
    (24.7224, 46.6901),
    (24.7358, 46.7068),
    (24.7487, 46.7242),
    (24.7606, 46.7395),
    (24.7741, 46.7561),
]


def bearing_degrees(start, end):
    lat1 = math.radians(start[0])
    lat2 = math.radians(end[0])
    delta_lng = math.radians(end[1] - start[1])
    x = math.sin(delta_lng) * math.cos(lat2)
    y = math.cos(lat1) * math.sin(lat2) - math.sin(lat1) * math.cos(lat2) * math.cos(delta_lng)
    return (math.degrees(math.atan2(x, y)) + 360) % 360


def interpolate(start, end, fraction):
    return (
        start[0] + (end[0] - start[0]) * fraction,
        start[1] + (end[1] - start[1]) * fraction,
    )


def next_position(step):
    segment_steps = 6
    segment_index = (step // segment_steps) % len(ROUTE)
    next_index = (segment_index + 1) % len(ROUTE)
    fraction = (step % segment_steps) / segment_steps
    start = ROUTE[segment_index]
    end = ROUTE[next_index]
    lat, lng = interpolate(start, end, fraction)
    heading = bearing_degrees(start, end)
    speed = 42 + (step % 5) * 3
    return lat, lng, speed, heading


def post_update(payload):
    request = urllib.request.Request(
        f"{SPRING_URL}/api/gps/updates",
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=10) as response:
        return response.status, response.read().decode("utf-8")


def main():
    print(
        f"Starting GPS simulator vehicle={VEHICLE_FLEET_NUMBER} springUrl={SPRING_URL} interval={INTERVAL_SECONDS}s",
        flush=True,
    )
    step = 0
    while True:
        lat, lng, speed, heading = next_position(step)
        payload = {
            "vehicleFleetNumber": VEHICLE_FLEET_NUMBER,
            "latitude": round(lat, 6),
            "longitude": round(lng, 6),
            "speedKph": round(speed, 1),
            "headingDegrees": round(heading, 1),
            "recordedAt": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
        }

        try:
            status, body = post_update(payload)
            print(f"sent status={status} payload={payload} response={body}", flush=True)
        except urllib.error.URLError as error:
            print(f"send failed payload={payload} error={error}", flush=True)

        step += 1
        time.sleep(INTERVAL_SECONDS)


if __name__ == "__main__":
    main()
