import numpy as np
from torch.fft import fftfreq

class AnomalyDetector:
    """ Class to detect anomalies in motor vibrations using FFT analysis."""

    def __init__(self, target_freq_min=70, target_freq_max=90, freq_threshold=5, threshold=5.0):
        self.target_freq_min = target_freq_min
        self.target_freq_max = target_freq_max
        self.threshold = threshold  # Absolute magnitude threshold
        self.freq_threshold = freq_threshold  # Frequency threshold for detection (minimal frequency)


    def frequency_threshold_filter(self, signal, threshold, window=None):
        """
        Removes all frequency components with normalized magnitude below threshold.
        Parameters:
        - signal: 1D numpy array of time-domain signal
        - threshold: float, minimum normalized magnitude to keep
        - window: optional 1D numpy array for windowing (default is no window)
        Returns time-domain filtered signal.
        """
        N = len(signal)

        # Optional window
        if window is None:
            window = np.ones(N)
        windowed_signal = signal * window

        # FFT and scaling
        fft_vals = np.fft.rfft(windowed_signal)
        magnitude = (2.0 / N) * np.abs(fft_vals)

        # Zero weak components
        fft_vals[magnitude < threshold] = 0

        # Inverse FFT and remove window effect (approx)
        filtered = np.fft.irfft(fft_vals, n=N)
        return filtered


    def do_fft(self, t, signal, window_size_s=1.0, sampling_rate=1000, overlap=0.5, magnitude_threshold=None):
        """
        Perform windowed FFT with overlap and return magnitudes.
        Parameters:
        - t: time array (same length as signal)
        - signal: input 1D signal
        - window_size_s: window size in seconds
        - sampling_rate: samples per second
        - overlap: fractional overlap between windows (e.g. 0.5 for 50%)
        Returns:
        - times: center times of each window
        - freqs: FFT frequency bins
        - magnitudes: 2D array [window index][frequency bin]
        """
        window_size = int(window_size_s * sampling_rate)
        step = int(window_size * (1 - overlap))
        if step <= 0:
            raise ValueError("Overlap too high; resulting step size <= 0")

        freqs = np.fft.rfftfreq(window_size, d=1 / sampling_rate)
        times = []
        magnitudes = []

        for start in range(0, len(signal) - window_size + 1, step):
            end = start + window_size
            window_signal = signal[start:end]
            t_center = t[start + window_size // 2]
            times.append(t_center)

            windowed_signal = window_signal * np.hanning(window_size)
            fft_vals = np.fft.rfft(windowed_signal)
            magnitude = (2.0 / window_size) * np.abs(fft_vals)

            # Apply threshold if specified
            if magnitude_threshold is not None:
                fft_vals[magnitude < magnitude_threshold] = 0
                magnitude = (2.0 / window_size) * np.abs(fft_vals) # Normalize the magnitudes so they're comparable to the original signal's amplitude

            magnitudes.append(magnitude)

        return np.array(times), freqs, np.array(magnitudes)


    def detect_anomalies(self, freqs, normal_freqs, magnitudes, threshold_ratio=0.5, tolerance=3.0, group_distance=3.0):
        """
        Detect anomalous frequencies robustly:
        - Groups nearby strong peaks
        - Ignores any close to normal frequencies

        Parameters:
        - freqs: array of FFT bins (1D)
        - normal_freqs: list of known/expected frequencies
        - magnitudes: 2D array [time_frame, freq_bin]
        - threshold_ratio: relative threshold to max magnitude
        - tolerance: Hz distance to normal freqs to ignore
        - group_distance: Hz to merge nearby anomaly bins into one group

        Returns:
        - List of detected anomaly frequencies (grouped and filtered)
        """
        max_magnitude = np.max(magnitudes)
        peak_mask = np.max(magnitudes, axis=0) > threshold_ratio * max_magnitude
        strong_freqs = freqs[peak_mask]

        # Step 1: group close peaks into frequency clusters
        if len(strong_freqs) == 0:
            return []

        strong_freqs = np.sort(strong_freqs)
        groups = []
        current_group = [strong_freqs[0]]

        for f in strong_freqs[1:]:
            if abs(f - current_group[-1]) <= group_distance:
                current_group.append(f)
            else:
                groups.append(current_group)
                current_group = [f]
        groups.append(current_group)

        # Step 2: reduce each group to representative (mean or strongest)
        detected = [round(np.mean(g), 1) for g in groups]

        # Step 3: exclude anything close to normal frequencies
        filtered = []
        for f in detected:
            if all(abs(f - nf) > tolerance for nf in normal_freqs):
                filtered.append(f)

        return filtered


    def snap_to_nearest(self, freqs, target_freqs, tolerance=5.0):
        """
        Snap each frequency in `target_freqs` to the nearest value in `freqs`
        if within `tolerance` Hz.

        Parameters:
        - freqs: 1D np.array of allowed frequency values (e.g., FFT bins)
        - target_freqs: list of floats to snap
        - tolerance: max distance allowed to snap (Hz)

        Returns:
        - snapped: list of snapped frequencies
        """
        snapped = []
        for f in target_freqs:
            idx = np.argmin(np.abs(freqs - f))
            nearest = freqs[idx]
            if abs(nearest - f) <= tolerance:
                snapped.append(round(nearest, 2))
        return sorted(set(snapped))


    def detect_(self, freqs, magnitudes):
        """ Detects anomalies in the frequency domain.
        Given frequency array and magnitude array (both numpy arrays),
        returns True if anomaly detected.
        """
        mask = (freqs >= self.target_freq_min) & (freqs <= self.target_freq_max)
        target_magnitudes = magnitudes[mask]
        if np.any(target_magnitudes > self.threshold):
            return True
        else:
            return False

    # Perform FFT anomaly detection in sliding windows
    def detect_fft_anomalies_windowed_(self, signal, sampling_rate, window_size_s=0.5):
        """ Detects anomalies in the time domain using FFT analysis.
        Given a signal, sampling rate, and window size in seconds,
        returns times and frequencies of detected anomalies.
        """
        window_size = int(window_size_s * sampling_rate)
        step_size = window_size // 2  # 50% overlap
        n_windows = (len(signal) - window_size) // step_size

        times = []
        anomaly_freqs = []

        window_func = np.hanning(window_size)
        window_gain = np.sum(window_func) / window_size  # Used for amplitude correction

        for i in range(n_windows):
            start = i * step_size
            end = start + window_size
            t_center = (start + end) / 2 / sampling_rate

            magnitude, fft_freqs = self.do_fft(start, end, signal, window_size, sampling_rate, window_gain, times=times, anomaly_freqs=anomaly_freqs)
            if np.any(magnitude > self.threshold):  # Assuming self.threshold exists
                dominant_freqs = fft_freqs[magnitude > self.threshold]
                times.append(t_center)
                anomaly_freqs.append(dominant_freqs.tolist())

        # Filter out frequencies below the frequency threshold
        # anomaly_freqs = [[freq for freq in freqs if freq >= self.freq_threshold] for freqs in anomaly_freqs]

        return times, anomaly_freqs


    def remove_harmonics_(self, freqs, tolerance=1.0):
        """Remove harmonic frequencies from a list, keeping only fundamentals."""
        if len(freqs) == 0:
            return freqs

        freqs = np.sort(freqs)  # Sort ascending
        fundamentals = []

        for f in freqs:
            is_harmonic = False
            for base in fundamentals:
                ratio = f / base
                if abs(ratio - round(ratio)) < (tolerance / base):
                    is_harmonic = True
                    break
            if not is_harmonic:
                fundamentals.append(f)

        return np.array(fundamentals)

    # ToDo check if this is used (currently not) and can be deleted
    def do_fft_no_hanning(self, start, end, signal, window_size, sampling_rate, window_gain, times=[], anomaly_freqs=[]):
        t_center = (start + end) / 2 / sampling_rate
        window_signal = signal[start:end]
        windowed_signal = window_signal * np.hanning(
            len(window_signal))  # Apply Hanning window to reduce spectral leakage

        # ToDo Use rfft to avoid duplicate negative frequencies
        current_window_len = len(window_signal)
        fft_vals = np.fft.fft(windowed_signal)
        fft_vals = fft_vals[:current_window_len // 2]
        fft_freqs = np.fft.fftfreq(current_window_len, d=1 / sampling_rate)[:current_window_len // 2]

        # magnitude = np.abs(fft_vals)
        magnitude = (2.0 / window_size) * np.abs(fft_vals) / window_gain
        median_mag = np.median(magnitude)
        anomalies = magnitude > self.threshold * median_mag
        freqs_anomalous = fft_freqs[anomalies]
        # Only take positive frequencies
        freqs_anomalous = freqs_anomalous[(freqs_anomalous > 0)]

        if len(freqs_anomalous) > 0:
            freqs_anomalous = self.remove_harmonics(freqs_anomalous, tolerance=1.0)

            times.append(t_center)
            anomaly_freqs.append(freqs_anomalous)

        # Normalize magnitude: correct for window function and scale by length
        magnitude = (2.0 / window_size) * np.abs(fft_vals) / window_gain
        return magnitude, fft_freqs

