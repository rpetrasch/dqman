import numpy as np
import matplotlib.pyplot as plt
import matplotlib.animation as animation
from matplotlib.widgets import Button, Slider

class MotorVibrationMonitor:
    ''' Class to monitor motor vibration using FFT and a sliding window approach.
        It uses a detector to identify anomalies in the signal.
    '''
    def __init__(self, t, signal, detector, sampling_rate):
        self.t = t
        self.signal = signal
        # self.detector = detector  # << here we inject the detector
        self.sampling_rate = sampling_rate
        self.window_size_s = 0.5
        self.step_samples = 20
        self.interval_ms = 100
        self.window_size = int(self.window_size_s * self.sampling_rate)
        self.n_frames = (len(signal) - self.window_size) // self.step_samples
        self.fault_frame_counter = 0
        # self.build_plot()
        self.old_fft_lines = []
        self.max_old_fft_lines = 10  # how many past FFTs to keep (fade out gradually)


    def build_plot(self, t, signal, times, freqs, normal_freqs, anomaly_freqs, magnitudes, window_size_s, sampling_rate):
        """
        Builds animated plot showing:
        - time-domain signal with moving window (top)
        - FFT magnitudes with normal/anomaly highlights (bottom)
        """
        window_size = int(window_size_s * sampling_rate)

        fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(10, 6))

        # Time-domain signal
        ax1.plot(t, signal, color='lightgray', label="Full Signal")
        window_line, = ax1.plot([], [], color='blue', label="FFT Window")
        ax1.set_xlim(t[0], t[-1])
        ax1.set_ylim(np.min(signal), np.max(signal))
        ax1.set_xlabel("Time (s)")
        ax1.set_ylabel("Amplitude")
        ax1.set_title("Time-domain signal with moving FFT window")
        ax1.legend(loc='upper right')

        # Frequency-domain FFT magnitude
        fft_line, = ax2.plot([], [], color='black', label='FFT magnitude')
        ax2.set_xlim(0, min(200, np.max(normal_freqs + anomaly_freqs + [100])))
        ax2.set_ylim(0, np.max(magnitudes) * 1.1)
        ax2.set_xlabel("Frequency (Hz)")
        ax2.set_ylabel("Magnitude")
        ax2.set_title("FFT magnitude with normal (green) and anomaly (red) markers")

        # Static vertical markers
        normal_lines = [ax2.axvline(f, color='green', linestyle='--', linewidth=1, label='Normal Frequency') for f in normal_freqs]
        anomaly_lines = [ax2.axvline(f, color='red', linestyle='--', linewidth=1.2, label='Anomaly Frequency') for f in anomaly_freqs]

        # Ensure legend is only created once
        handles, labels = ax2.get_legend_handles_labels()
        ax2.legend(dict(zip(labels, handles)).values(), dict(zip(labels, handles)).keys(), loc='upper right')

        def update_plot(frame_idx):
            t_center = times[frame_idx]
            half_win = window_size // 2
            center_idx = np.searchsorted(t, t_center)
            start = max(0, center_idx - half_win)
            end = min(len(signal), center_idx + half_win)

            # Update time window
            window_line.set_data(t[start:end], signal[start:end])
            # Update FFT magnitude
            fft_line.set_data(freqs, magnitudes[frame_idx])

            return [window_line, fft_line] + normal_lines + anomaly_lines

        return fig, update_plot



    def build_plot_(self):
        self.fig, (self.ax_signal, self.ax_fft) = plt.subplots(2, 1, figsize=(12, 8))
        plt.subplots_adjust(bottom=0.2, hspace=0.35)

        # Signal plot (top)
        self.ax_signal.set_xlim(self.t[0], self.t[-1])
        self.ax_signal.set_ylim(np.min(self.signal) - 1, np.max(self.signal) + 1)
        self.ax_signal.set_xlabel("Time (s)")
        self.ax_signal.set_ylabel("Vibration amplitude")
        self.ax_signal.set_title("Motor Vibration - Moving Time Window")
        self.ax_signal.grid()

        self.ax_signal.plot(self.t, self.signal, color='lightgray')
        self.window_line, = self.ax_signal.plot([], [], color='red', linewidth=2)

        self.warning_text = self.ax_signal.text(0.5, 0.9, "FAULT DETECTED!", color='red',
                                                fontsize=20, ha='center', va='center',
                                                transform=self.ax_signal.transAxes, visible=False)

        # FFT plot (bottom)
        self.fft_freqs = np.fft.fftfreq(self.window_size, d=1 / self.sampling_rate)
        self.positive_freqs = self.fft_freqs[:self.window_size // 2]
        # self.positive_freqs = np.fft.rfftfreq(self.window_size, d=1 / self.sampling_rate)
        self.ax_fft.set_xlim(0, 200)
        self.ax_fft.set_ylim(0, 90)
        self.ax_fft.set_xlabel("Frequency (Hz)")
        self.ax_fft.set_ylabel("Magnitude")
        self.ax_fft.set_title("FFT of Current Window")
        self.ax_fft.grid()

        self.fft_line, = self.ax_fft.plot([], [], color='blue')
        self.anomaly_points = self.ax_fft.scatter([], [], color='red')

        # 🔥 Add Restart Button
        ax_button = plt.axes([0.7, 0.05, 0.1, 0.075])
        self.button = Button(ax_button, 'Restart')
        self.button.on_clicked(self.restart_animation)

        # 🔥 Add Threshold Slider
        ax_slider = plt.axes([0.15, 0.05, 0.4, 0.03])
        self.slider = Slider(ax_slider, 'Threshold', 1.0, 20.0, valinit=self.detector.threshold)
        self.slider.on_changed(self.update_threshold)

        self.start_animation()

    def start_animation(self):
        self.frame_generator = np.arange(0, self.n_frames * self.step_samples, self.step_samples)
        self.ani = animation.FuncAnimation(self.fig, self.update, frames=self.frame_generator,
                                           interval=self.interval_ms, blit=False, repeat=True)

    def update(self, frame):
        start = frame
        end = start + self.window_size
        if end >= len(self.signal):
            start = len(self.signal) - self.window_size
            end = len(self.signal)

        window_t = self.t[start:end]
        window_signal = self.signal[start:end]
        self.window_line.set_data(window_t, window_signal)

        # fft_vals = np.fft.fft(window_signal)
        # windowed_signal = window_signal * np.hanning(len(window_signal))
        # fft_vals = np.fft.fft(windowed_signal)
        # magnitudeOld = np.abs(fft_vals)[:self.window_size//2]

        window_func = np.hanning(self.window_size)
        window_gain = np.sum(window_func) / self.window_size  # Used for amplitude correction
        magnitude, fft_freqs = self.detector.do_fft(start, end, window_signal, self.window_size, self.sampling_rate, window_gain)

        # Trim the FFT amd the window to have the same nuimber of values
        # min_len = min(len(self.positive_freqs), len(magnitude))
        # self.positive_freqs = self.positive_freqs[:min_len]
        # magnitude = magnitude[:min_len]
        # magnitude = magnitude[:-1]
        # fft_freqs = fft_freqs[:-1]

        # Update the FFT plot
        self.fft_line.set_data(self.positive_freqs, magnitude)

        # Plot the new FFT (blue, fresh)
        try:
            new_fft_line, = self.ax_fft.plot(self.positive_freqs, magnitude, color='blue', alpha=1.0)
            self.old_fft_lines.append(new_fft_line)
        except Exception as e:
            print(f"Error in FFT plot: {e}")


        # Fade out old FFT lines
        for idx, old_line in enumerate(self.old_fft_lines):
            fade_alpha = max(0, 1.0 - (idx + 1) / self.max_old_fft_lines)
            old_line.set_alpha(fade_alpha)

        # Limit number of old lines
        if len(self.old_fft_lines) > self.max_old_fft_lines:
            oldest_line = self.old_fft_lines.pop(0)
            oldest_line.remove()

        # Call the anomaly detector
        anomaly_detected = self.detector.detect(self.positive_freqs, magnitude)

        if anomaly_detected:
            self.fault_frame_counter += 1
            if (self.fault_frame_counter // 5) % 2 == 0:
                self.warning_text.set_visible(True)
                self.ax_signal.set_facecolor('mistyrose')
            else:
                self.warning_text.set_visible(False)
                self.ax_signal.set_facecolor('white')
        else:
            self.fault_frame_counter = 0
            self.warning_text.set_visible(False)
            self.ax_signal.set_facecolor('white')

        return self.window_line, self.fft_line, self.anomaly_points, self.warning_text

    def restart_animation(self, event):
        """Stops and restarts the animation cleanly."""
        if hasattr(self, 'ani') and self.ani.event_source:
            self.ani.event_source.stop()
        self.fault_frame_counter = 0
        self.start_animation()

    def update_threshold(self, val):
        """Updates the threshold inside the injected detector."""
        self.detector.threshold = val