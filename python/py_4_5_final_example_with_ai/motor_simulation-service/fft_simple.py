import numpy as np
import matplotlib.pyplot as plt
from motor_simulator import Motor, normal_freqs, fault_freqs_default, noise_level, fault_time_s

def detect_frequencies_no_hanning(signal, sampling_rate, window_size, overlap=0.5, threshold_ratio=0.1):
    """
    Detect frequencies over time without Hanning window.

    Parameters:
    - signal: 1D array of amplitude values
    - sampling_rate: samples per second (Hz)
    - window_size: number of samples per window
    - overlap: fractional overlap between windows (0.5 = 50%)
    - threshold_ratio: threshold as fraction of max magnitude in each window

    Returns:
    - times: center time of each window
    - freqs: FFT frequency bins
    - all_freqs_per_window: list of detected frequencies for each window
    - magnitudes: 2D array [window][freq_bin] for plotting
    """
    step = int(window_size * (1 - overlap))
    times = []
    all_freqs_per_window = []
    magnitudes = []

    # Frequency bins (computed once, same for all windows)
    freqs = np.fft.fftfreq(window_size, d=1 / sampling_rate)[:window_size // 2]

    for start in range(0, len(signal) - window_size + 1, step):
        end = start + window_size
        window_signal = signal[start:end]

        # Center time of this window
        t_center = (start + window_size // 2) / sampling_rate
        times.append(t_center)

        # FFT without windowing
        fft_vals = np.fft.fft(window_signal)
        fft_vals = fft_vals[:window_size // 2]

        # Magnitude (normalized)
        magnitude = (2.0 / window_size) * np.abs(fft_vals)
        magnitudes.append(magnitude)

        # Detect peaks above threshold
        threshold = threshold_ratio * np.max(magnitude)
        anomalies = magnitude > threshold
        detected_freqs = freqs[anomalies]
        detected_freqs = detected_freqs[detected_freqs > 0]  # Only positive frequencies

        all_freqs_per_window.append(detected_freqs)

    return np.array(times), freqs, all_freqs_per_window, np.array(magnitudes)


def plot_frequencies_over_time(times, freqs, all_freqs_per_window, magnitudes):
    """
    Plot detected frequencies over time with a spectrogram-like view.

    Parameters:
    - times: center times of windows
    - freqs: frequency bins
    - all_freqs_per_window: list of detected frequencies for each window
    - magnitudes: 2D array of magnitudes
    """
    fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(12, 8))

    # Plot 1: Spectrogram-style heatmap
    im = ax1.imshow(magnitudes.T, aspect='auto', origin='lower',
                    extent=[times[0], times[-1], freqs[0], freqs[-1]],
                    cmap='viridis', interpolation='nearest')
    ax1.set_ylabel('Frequency (Hz)')
    ax1.set_title('FFT Magnitudes Over Time (Spectrogram)')
    plt.colorbar(im, ax=ax1, label='Magnitude')

    # Plot 2: Detected frequencies as scatter points
    all_times = []
    all_detected_freqs = []

    for i, (t, freqs_in_window) in enumerate(zip(times, all_freqs_per_window)):
        all_times.extend([t] * len(freqs_in_window))
        all_detected_freqs.extend(freqs_in_window)

    if len(all_times) > 0:
        ax2.scatter(all_times, all_detected_freqs, alpha=0.6, s=30, color='red')

    ax2.set_xlabel('Time (s)')
    ax2.set_ylabel('Frequency (Hz)')
    ax2.set_title('Detected Frequencies Over Time')
    ax2.grid(True, alpha=0.3)

    plt.tight_layout()
    plt.show()


# org.dqman.Main
if __name__ == "__main__":
    # Generate test signal
    motor = Motor(normal_freqs)
    t, signal = motor.create_motor_vibration(duration_s=5.0, sampling_rate=sampling_rate,
                                             noise_level=noise_level, fault_freqs=fault_freqs_default,
                                             fault_time_s=fault_time_s)

    # Detect frequencies
    window_size = 512  # samples
    times, freqs, all_freqs_per_window, magnitudes = detect_frequencies_no_hanning(
        signal=signal,
        sampling_rate=sampling_rate,
        window_size=window_size,
        overlap=0.5,
        threshold_ratio=0.1
    )

    # Plot results
    plot_frequencies_over_time(times, freqs, all_freqs_per_window, magnitudes)

    # Print detected frequencies
    print("Detected frequencies per window:")
    for i, (t, freqs_detected) in enumerate(zip(times, all_freqs_per_window)):
        print(f"  Window {i} (t={t:.2f}s): {np.round(freqs_detected, 1)} Hz")