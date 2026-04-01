package com.psyassistant.scheduling.event;

import com.psyassistant.scheduling.domain.AppointmentStatus;
import java.util.UUID;

/**
 * Event published when an appointment's status changes.
 *
 * <p>This event enables event-driven workflows such as automatic session record creation
 * when an appointment transitions to COMPLETED or IN_PROGRESS status.
 *
 * <p>The event is published synchronously within the transaction that changes the status,
 * ensuring that event listeners can participate in the same transactional boundary.
 */
public record AppointmentStatusChangedEvent(
        UUID appointmentId,
        AppointmentStatus oldStatus,
        AppointmentStatus newStatus,
        UUID actorUserId,
        String actorName
) {
    /**
     * Factory method for creating an event.
     *
     * @param appointmentId appointment UUID
     * @param oldStatus previous status
     * @param newStatus new status
     * @param actorUserId user who triggered the change
     * @param actorName display name of the actor
     * @return new event instance
     */
    public static AppointmentStatusChangedEvent of(
            final UUID appointmentId,
            final AppointmentStatus oldStatus,
            final AppointmentStatus newStatus,
            final UUID actorUserId,
            final String actorName) {
        return new AppointmentStatusChangedEvent(
                appointmentId,
                oldStatus,
                newStatus,
                actorUserId,
                actorName
        );
    }
}
