package org.dqman.cybersecurity.repository;

import org.dqman.cybersecurity.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Device} master data.
 * Provides lookups needed for the new-device streaming detection rule:
 * resolve the device from its hardware ID and verify it belongs to the session user.
 */
public interface DeviceRepository extends JpaRepository<Device, Long> {

    /**
     * Finds a device by its MDM hardware identifier.
     *
     * @param deviceId the device identifier as it appears in log events (e.g. {@code CORP-LAPTOP-001})
     * @return the matching device record, or empty if unknown
     */
    Optional<Device> findByDeviceId(String deviceId);

    /**
     * Returns all devices registered to a given user in the MDM.
     * Used to check whether the device seen in a log event is among the user's known devices.
     *
     * @param userId the surrogate key of the owning {@link org.dqman.cybersecurity.model.User}
     * @return list of all MDM-registered devices for that user, possibly empty
     */
    List<Device> findByOwner_Id(Long userId);
}
