import numpy as np
import logging

from matplotlib.animation import FuncAnimation
""" Motor Vibration Simulation and Anomaly Detection via FFT (AnomalyDetector) """

logger = logging.getLogger(__name__)
import matplotlib.pyplot as plt
from visualizer import plot_anomalies, plot_spectrogram, plot_signal, animate_moving_window_with_fft
from monitor import MotorVibrationMonitor
from anomaly_detector import AnomalyDetector
from motor import Motor

plot = True  # Plot the raw data and FFT analysis on the screen
normal_freqs = [25, 67]  # Normal operation (motor vibration frequencies)
fault_freqs_default = [13, 45, 89]  # Abnormal motor vibration frequencies (anomalies)
fault_time_s = 2.5  # Time at which fault is injected
noise_level = 0.65  # Noise level for motor vibration simulation
sampling_rate = 1000  # 1000 samples per second
window_size_s = 0.5  # Window size for moving window FFT analysis
magnitude_threshold = 0.0  # Minimum magnitude to consider a frequency component significant, 0.2 eliminates noise

def simulate_motor_vibration(duration, sampling_rate, noise, fault_time, inject_fault):
    """
    Simulate motor vibration signal with optional fault injection and noise.
    """
    global normal_freqs, fault_freqs_default
    motor = Motor(normal_freqs)

    if not inject_fault:
        fault_freqs = None
    else:
        fault_freqs = fault_freqs_default

    t, signal = motor.create_motor_vibration(duration_s=duration, sampling_rate=sampling_rate,
                                             noise_level=noise, fault_freqs=fault_freqs, fault_time_s=fault_time)
    return t, signal


if __name__ == "__main__":
    # Set up motor and anomaly detector
    motor = Motor(normal_freqs)
    detector = AnomalyDetector(threshold=5.0)

    # Simulate a motor vibration signal with normal and faulty components and added noise
    t, signal = motor.create_motor_vibration(duration_s=5.0, sampling_rate=sampling_rate,
                                             noise_level=noise_level, fault_freqs=fault_freqs_default,
                                             fault_time_s=fault_time_s)
    # Run windowed FFT and get the magnitudes (incl. normal frequencies)
    times, freqs, magnitudes = detector.do_fft(t, signal, window_size_s=window_size_s,
                sampling_rate=sampling_rate, magnitude_threshold=magnitude_threshold)

    do_fft_no_hanning
    # anomaly detection (sort out the anomalies)
    anomaly_freqs = detector.detect_anomalies(freqs, normal_freqs, magnitudes)

    print("Detected frequencies (Hz) at times (s):")
    for i, t_point in enumerate(times):
        magnitudes_in_window = magnitudes[i]
        # optionally threshold magnitude
        detected_freqs = freqs[magnitudes_in_window > magnitude_threshold]
        print(f"Time {t_point:.2f} s: Frequencies {np.round(detected_freqs, 1)} Hz", end=' ')
        for anomaly_freq in anomaly_freqs:
            if anomaly_freq in detected_freqs:
                print(f"**Anomaly at {anomaly_freq} Hz**", end=' ')
        print()

    if plot:
        plot_signal(t, signal)  # Raw motor vibration signal
        plot_anomalies(times, freqs, magnitudes, anomaly_freqs)  # Chatter plot for anomaly freqs
        plot_spectrogram(signal, sampling_rate)  # Spectrogram of motor vibration signal

        animate_moving_window_with_fft(t, signal, window_size_s=0.5, sampling_rate=sampling_rate,
                                       step_samples=20, interval_ms=100,
                                       anomaly_threshold=10.0, min_magnitude=1.0)

        # Create the monitor and build the plot
        monitor = MotorVibrationMonitor(t, signal, detector, sampling_rate=sampling_rate)
        fig, update_fn = monitor.build_plot(t, signal, times, freqs, normal_freqs, anomaly_freqs, magnitudes, window_size_s, sampling_rate)
        ani = FuncAnimation(fig, update_fn, frames=len(times), interval=200, blit=True)
        plt.tight_layout()
        fig.canvas.manager.set_window_title("FFT and Sliding Window")
        plt.show()

    # freq_mask = (freqs >= 0) & (freqs <= 100)
    # filtered_freqs = freqs[freq_mask]
    # filtered_magnitudes = magnitudes[:, freq_mask]
    # plt.figure().canvas.manager.set_window_title("Spectrogram")
    # plt.pcolormesh(times, filtered_freqs, filtered_magnitudes.T, shading='auto')
    # plt.xlabel("Time [s]")
    # plt.ylabel("Frequency [Hz]")
    # plt.colorbar(label="Magnitude")
    # plt.title("Spectrogram (0–100 Hz)")
    # plt.show()
