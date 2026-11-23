FROM python:3.12-slim

WORKDIR /app

COPY simulator.py /app/simulator.py

ENV SPRING_URL=http://host.docker.internal:8080
ENV VEHICLE_FLEET_NUMBER=LB-001
ENV GPS_INTERVAL_SECONDS=5

CMD ["python", "/app/simulator.py"]
