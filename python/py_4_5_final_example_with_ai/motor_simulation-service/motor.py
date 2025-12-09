import numpy as np


class Motor:
    '''Class to simulate motor vibrations with normal and faulty components'''

    def __init__(self, freqs):
        self.freqs = freqs

    def get_min_freq(self):
        return np.min(self.freqs)

    def get_max_freq(self):
        return np.max(self.freqs)

    def create_motor_vibration(self, duration_s=5.0, sampling_rate=1000, noise_level=0.3, fault_freqs=None, fault_time_s=2.5):
        '''Simulate a motor vibration signal with normal and faulty components and added noise'''
        # Create time vector
        t = np.linspace(0, duration_s, int(duration_s * sampling_rate), endpoint=False)
        # Generate normal vibrations
        # vibration_normal = np.sin(2 * np.pi * 30 * t) + 0.5 * np.sin(2 * np.pi * 60 * t)  ToDo delete this
        vibration_normal = sum(np.sin(2 * np.pi * f * t) for f in self.freqs)

        # Add fault starting halfway
        vibration_with_fault = np.copy(vibration_normal)

        # Add faulty frequencies
        fault_amplitude = 0.7
        if fault_freqs is not None:
            # Create fault mask: 0 before fault_time_s, 1 after
            fault_mask = (t >= fault_time_s).astype(float)
            for ff in fault_freqs:
                fault_component = fault_amplitude * np.sin(2 * np.pi * ff * t) * fault_mask
                vibration_with_fault += fault_component

        # Add noise
        if noise_level > 0:
            noise = noise_level * np.random.normal(size=t.shape)
            vibration_with_fault += noise

        return t.tolist(), vibration_with_fault.tolist()