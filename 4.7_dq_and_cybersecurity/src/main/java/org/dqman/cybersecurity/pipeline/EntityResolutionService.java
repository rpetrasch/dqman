package org.dqman.cybersecurity.pipeline;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dqman.cybersecurity.model.Device;
import org.dqman.cybersecurity.model.KnownLocation;
import org.dqman.cybersecurity.model.User;
import org.dqman.cybersecurity.repository.DeviceRepository;
import org.dqman.cybersecurity.repository.KnownLocationRepository;
import org.dqman.cybersecurity.repository.UserRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Entity resolution against master data (users, devices, known locations).
 * All lookups are backed by Caffeine cache to avoid a DB round-trip per event.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EntityResolutionService {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final KnownLocationRepository locationRepository;

    @Cacheable("users-by-username")
    public Optional<User> resolveUser(String username) {
        return userRepository.findByUsername(username);
    }

    @Cacheable("devices-by-id")
    public Optional<Device> resolveDevice(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId);
    }

    @Cacheable("devices-by-owner")
    public List<Device> resolveUserDevices(Long userId) {
        return deviceRepository.findByOwner_Id(userId);
    }

    @Cacheable("locations-by-ip")
    public Optional<KnownLocation> resolveLocation(String ip) {
        return locationRepository.findByIpPrefix(ip);
    }

    public boolean isKnownDevice(Long userId, String deviceId) {
        return resolveUserDevices(userId).stream()
                .anyMatch(d -> d.getDeviceId().equals(deviceId));
    }
}
