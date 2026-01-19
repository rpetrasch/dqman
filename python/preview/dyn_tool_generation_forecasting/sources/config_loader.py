"""
Simple configuration loader.
"""
import yaml
import os

class Config:
    def __init__(self, path="config/settings.yaml"):
        """
        Constructor.
        :param path: Path to the config file.
        """
        self.path = path
        self._config = self._load()

    def _load(self):
        """Loads the config file."""
        if not os.path.exists(self.path):
            raise FileNotFoundError(f"Config file not found: {self.path}")
        with open(self.path, 'r') as f:
            return yaml.safe_load(f)

    @property
    def llm(self):
        return self._config.get('llm', {})
        
    @property
    def tools_dir(self):
        return self._config.get('execution', {}).get('tools_dir', 'sources/dynamic_tools')

    @property
    def data_config(self):
        return self._config.get('data', {})

# Global instance for easy access if needed
default_config = Config()
