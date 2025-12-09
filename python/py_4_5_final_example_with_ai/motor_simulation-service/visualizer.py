import numpy as np
import matplotlib.pyplot as plt
from matplotlib import animation
from scipy.signal import spectrogram


# Plot time-domain signal
def plot_signal(t,  signal):
    """    Plot the time-domain signal of motor vibration.
    Parameters:
    - t: 1D array of time values
    - signal: 1D array of signal values
    """
    fig = plt.figure(figsize=(12,4))
    plt.plot(t, signal)
    plt.title("Raw Data for Simulated Motor Vibration with Noise (Time Domain)")
    plt.xlabel("Time (s)")
    plt.ylabel("Vibration amplitude")
    plt.grid()
    fig.canvas.manager.set_window_title("Raw Sample Plot")
    plt.show()


# Plot detected frequency anomalies
def plot_anomalies(times, freqs, magnitudes, anomaly_freqs, tolerance=1.0):
    """
    Plot anomaly frequencies over time with their magnitudes.
    Parameters:
    - times: 1D array of window center times (length = n_windows)
    - freqs: 1D array of frequency bins (length = n_freqs)
    - magnitudes: 2D array (n_windows, n_freqs)
    - anomaly_freqs: list of anomaly frequencies to highlight
    - tolerance: allowed deviation in Hz for matching anomaly frequencies
    """
    fig = plt.figure(figsize=(12, 6))

    for anomaly_freq in anomaly_freqs:
        # Find index(es) in freqs within tolerance
        freq_indices = np.where(np.abs(freqs - anomaly_freq) <= tolerance)[0]
        if len(freq_indices) == 0:
            print(f"Warning: Anomaly frequency {anomaly_freq} Hz not found in freq bins.")
            continue

        for idx in freq_indices:
            # Plot the magnitude over time at this frequency index
            plt.scatter(
                times,
                np.full_like(times, freqs[idx]),
                c=magnitudes[:, idx],
                cmap='viridis',
                marker='o',
                label=f"{anomaly_freq} Hz" if idx == freq_indices[0] else None
            )

    fig.canvas.manager.set_window_title("Anomalies Over Time")
    plt.colorbar(label='Magnitude')
    plt.xlabel("Time (s)")
    plt.ylabel("Frequency (Hz)")
    plt.title("Anomalous Frequencies Over Time with Magnitudes")
    plt.legend()
    plt.grid()
    plt.show()


# Plot spectrogram for normal frequency and frequency anomalies
def plot_spectrogram(signal, sampling_rate):
    """
    Plot spectrogram of a signal
    Parameters:
    - signal: 1D array of signal values
    - sampling_rate: sampling rate of the signal in Hz
    """
    signal = np.array(signal)
    f, t, Sxx = spectrogram(signal, fs=sampling_rate, window='hann',
                            nperseg=500, noverlap=250, scaling='spectrum', mode='magnitude')

    fig = plt.figure(figsize=(12, 6))
    fig.canvas.manager.set_window_title("Spectrogram")
    plt.pcolormesh(t, f, Sxx, shading='gouraud')
    plt.colorbar(label="Magnitude")
    plt.title("Spectrogram of Motor Vibration")
    plt.ylabel('Frequency (Hz)')
    plt.xlabel('Time (s)')
    plt.ylim(0, 120)  # Focus on interesting range
    plt.grid()
    plt.show()

# Animation function
def animate_moving_window_with_fft(t, signal, window_size_s=0.5, sampling_rate=1000,
                                   step_samples=20, interval_ms=100, anomaly_threshold=5.0, min_magnitude=0.5):
    """
    Animate a moving window over a signal and display its FFT.
    smaller steps = slower animation
    Parameters:
    - t: 1D array of time values
    - signal: 1D array of signal values
    - window_size_s: size of the moving window in seconds
    - sampling_rate: sampling rate of the signal in Hz
    - step_samples: number of samples to move the window each frame
    - interval_ms: time interval between frames in milliseconds (slower frame rate)
    - anomaly_threshold: threshold for detecting anomalies in FFT magnitude
    - min_magnitude: minimum magnitude (ignore tiny noise)
    """
    window_size = int(window_size_s * sampling_rate)

    fig, (ax_signal, ax_fft) = plt.subplots(2, 1, figsize=(12, 8))
    fig.canvas.manager.set_window_title("Animation of Moving Window with FFT")
    plt.tight_layout(pad=4)

    # Top plot: signal
    ax_signal.set_xlim(t[0], t[-1])
    ax_signal.set_ylim(np.min(signal) - 1, np.max(signal) + 1)
    ax_signal.set_xlabel("Time (s)")
    ax_signal.set_ylabel("Vibration amplitude")
    ax_signal.set_title("Motor Vibration - Moving Time Window")
    ax_signal.grid()

    ax_signal.plot(t, signal, color='lightgray')
    window_line, = ax_signal.plot([], [], color='red', linewidth=2)

    warning_text = ax_signal.text(0.5, 0.9, "FAULT DETECTED!", color='red',
                                  fontsize=20, ha='center', va='center',
                                  transform=ax_signal.transAxes, visible=False)

    # Bottom plot: FFT
    fft_freqs = np.fft.fftfreq(window_size, d=1/sampling_rate)
    positive_freqs = fft_freqs[:window_size//2]
    ax_fft.set_xlim(0, 200)
    ax_fft.set_ylim(0, None)
    ax_fft.set_xlabel("Frequency (Hz)")
    ax_fft.set_ylabel("Magnitude")
    ax_fft.set_title("FFT of Current Window")
    ax_fft.grid()

    fft_line, = ax_fft.plot([], [], color='blue')
    anomaly_points = ax_fft.scatter([], [], color='red')

    def update(frame):
        start = frame
        end = start + window_size
        if end >= len(signal):
            start = len(signal) - window_size
            end = len(signal)

        window_t = t[start:end]
        window_signal = signal[start:end]
        window_line.set_data(window_t, window_signal)

        fft_vals = np.fft.fft(window_signal)
        magnitude = np.abs(fft_vals)[:window_size//2]

        fft_line.set_data(positive_freqs, magnitude)

        # Improved anomaly detection: above median and above min magnitude
        median_mag = np.median(magnitude)
        anomalies = (magnitude > anomaly_threshold * median_mag) & (magnitude > min_magnitude)

        anomaly_data = np.c_[positive_freqs[anomalies], magnitude[anomalies]]
        anomaly_points.set_offsets(anomaly_data)

        # Fault warning only if true anomalies
        warning_text.set_visible(np.any(anomalies))

        return window_line, fft_line, anomaly_points, warning_text

    n_frames = (len(signal) - window_size) // step_samples
    ani = animation.FuncAnimation(fig, update, frames=np.arange(0, n_frames * step_samples, step_samples),
                                  interval=interval_ms, blit=False, repeat=True)
    plt.show()
