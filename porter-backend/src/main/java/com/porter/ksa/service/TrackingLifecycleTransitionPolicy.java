package com.porter.ksa.service;

import com.porter.ksa.entity.TrackingStatus;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TrackingLifecycleTransitionPolicy {

    private final Map<TrackingStatus, Set<TrackingStatus>> automaticTransitions;

    public TrackingLifecycleTransitionPolicy() {
        automaticTransitions = new EnumMap<>(TrackingStatus.class);
        automaticTransitions.put(TrackingStatus.ASSIGNED, Set.of(TrackingStatus.EN_ROUTE_TO_PICKUP));
        automaticTransitions.put(TrackingStatus.EN_ROUTE_TO_PICKUP, Set.of(TrackingStatus.ARRIVED_AT_PICKUP));
        automaticTransitions.put(TrackingStatus.ARRIVED_AT_PICKUP, Set.of(TrackingStatus.LEFT_PICKUP));
        automaticTransitions.put(TrackingStatus.LEFT_PICKUP, Set.of(TrackingStatus.IN_TRANSIT));
        automaticTransitions.put(TrackingStatus.IN_TRANSIT, Set.of(TrackingStatus.ARRIVED_AT_DROP));
        automaticTransitions.put(TrackingStatus.ARRIVED_AT_DROP, Set.of(TrackingStatus.UNLOADING));
        automaticTransitions.put(TrackingStatus.UNLOADING, Set.of(TrackingStatus.DELIVERED));
        automaticTransitions.put(TrackingStatus.DELIVERED, Set.of());
        automaticTransitions.put(TrackingStatus.CANCELLED, Set.of());
    }

    public boolean canTransition(
            TrackingStatus current,
            TrackingStatus target,
            LifecycleTransitionSource source
    ) {
        if (current == target) {
            return true;
        }
        if (isTerminal(current)) {
            return false;
        }
        if (target == TrackingStatus.CANCELLED) {
            return source == LifecycleTransitionSource.MANUAL;
        }
        if (source == LifecycleTransitionSource.MANUAL) {
            return target.ordinal() > current.ordinal();
        }

        return automaticTransitions.getOrDefault(current, Set.of()).contains(target);
    }

    public boolean isTerminal(TrackingStatus status) {
        return status == TrackingStatus.DELIVERED || status == TrackingStatus.CANCELLED;
    }
}
