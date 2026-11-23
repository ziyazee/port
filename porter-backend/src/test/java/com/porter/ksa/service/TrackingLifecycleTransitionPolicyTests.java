package com.porter.ksa.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.porter.ksa.entity.TrackingStatus;
import org.junit.jupiter.api.Test;

class TrackingLifecycleTransitionPolicyTests {

    private final TrackingLifecycleTransitionPolicy policy = new TrackingLifecycleTransitionPolicy();

    @Test
    void gpsTransitionsMustFollowTheLifecycleSequence() {
        assertTrue(policy.canTransition(
                TrackingStatus.ASSIGNED,
                TrackingStatus.EN_ROUTE_TO_PICKUP,
                LifecycleTransitionSource.GPS
        ));
        assertFalse(policy.canTransition(
                TrackingStatus.ASSIGNED,
                TrackingStatus.IN_TRANSIT,
                LifecycleTransitionSource.GPS
        ));
    }

    @Test
    void manualTransitionsCanMoveForwardButCannotMoveBackward() {
        assertTrue(policy.canTransition(
                TrackingStatus.ARRIVED_AT_PICKUP,
                TrackingStatus.DELIVERED,
                LifecycleTransitionSource.MANUAL
        ));
        assertFalse(policy.canTransition(
                TrackingStatus.IN_TRANSIT,
                TrackingStatus.ARRIVED_AT_PICKUP,
                LifecycleTransitionSource.MANUAL
        ));
    }

    @Test
    void terminalStatusesCannotBeReopened() {
        assertFalse(policy.canTransition(
                TrackingStatus.DELIVERED,
                TrackingStatus.IN_TRANSIT,
                LifecycleTransitionSource.MANUAL
        ));
        assertFalse(policy.canTransition(
                TrackingStatus.CANCELLED,
                TrackingStatus.EN_ROUTE_TO_PICKUP,
                LifecycleTransitionSource.MANUAL
        ));
    }
}
