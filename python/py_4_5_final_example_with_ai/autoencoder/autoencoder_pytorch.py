# autoencoder_model.py
"""
Autoencoder model for vibration data anomaly detection using PyTorch.
"""
import os
import sys

os.environ["CUDA_VISIBLE_DEVICES"] = "-1"  # hides all GPU devices from TensorFlow
import logging
logger = logging.getLogger(__name__)
import torch
import torch.nn as nn


class Autoencoder(nn.Module):
    epochs = 20
    batch_size = 32
    lr = 0.001
    model_base_dir = os.environ.get('MODEL_PATH', '../models')
    model_name = "autoencoder.pth"  # pytorch (pth) model file path
    model_path = os.path.join(model_base_dir, model_name)
    # ToDo use dynamic file name, e.g. model_filename = f"autoencoder_epochs{epochs}_noise{noise_level}_fault{fault_injected}_{timestamp}.pth"
    small_model = False

    def __init__(self):
        """
        Initialize the autoencoder model.
        :param model_path: file path to save the model
        :param small_model: whether to use a small model (for faster training)
        """
        super(Autoencoder, self).__init__()

        if Autoencoder.small_model:
            self.encoder = nn.Sequential(
                nn.Linear(1, 16),
                nn.ReLU(),
                nn.Linear(16, 8),
                nn.ReLU()
            )
            self.decoder = nn.Sequential(
                nn.Linear(8, 16),
                nn.ReLU(),
                nn.Linear(16, 1)
            )
        else:
            self.encoder = nn.Sequential(
                nn.Linear(1, 32),
                nn.ReLU(),
                nn.Linear(32, 16),
                nn.ReLU(),
                nn.Linear(16, 8),
                nn.ReLU()
            )
            self.decoder = nn.Sequential(
                nn.Linear(8, 16),
                nn.ReLU(),
                nn.Linear(16, 32),
                nn.ReLU(),
                nn.Linear(32, 1)
            )
            try:
                os.makedirs(Autoencoder.model_base_dir, exist_ok=True)
            except OSError as e:
                logger.error(f"Error creating directory: {e}")
                raise

    def forward(self, x):
        """
        Defines how the input x flows through the network layers.
        It’s called automatically when the model is called with an input tensor, e.g.,
        output = model(input)
        :param x: input tensor
        :return: decoded tensor
        """
        encoded = self.encoder(x)
        decoded = self.decoder(encoded)
        return decoded

    def train(self, signal):
        """
        Train the autoencoder model on the provided signal data.
        :param signal: the signal data to train on
        :return: -
        """
        signal_tensor = torch.from_numpy(signal)
        criterion = nn.MSELoss()
        optimizer = torch.optim.Adam(self.parameters(), lr=Autoencoder.lr)
        # use learning rate decay: reduce learning rate every 5 epochs
        scheduler = torch.optim.lr_scheduler.StepLR(optimizer, step_size=5, gamma=0.5)

        dataset = torch.utils.data.TensorDataset(signal_tensor)
        dataloader = torch.utils.data.DataLoader(dataset, batch_size=Autoencoder.batch_size, shuffle=True)

        for epoch in range(Autoencoder.epochs):
            running_loss = 0.0
            for batch in dataloader:
                inputs = batch[0]
                outputs = self(inputs)
                loss = criterion(outputs, inputs)

                optimizer.zero_grad()
                loss.backward()
                optimizer.step()
                running_loss += loss.item()
            scheduler.step()
            avg_loss = running_loss / len(dataloader)
            logger.info(f"training: epoch: {epoch} -> avg_loss: {avg_loss}")

        logger.info(f"Training completed. Loss: {loss.item()}")

    def save_model(self):
        """
        Save the trained model to a file.
        :return: -
        """
        torch.save(self.state_dict(), Autoencoder.model_path)

    def load_model(self):
        """
        Load the trained model from a file.
        :return: -
        """
        self.load_state_dict(torch.load(Autoencoder.model_path, weights_only=True))
