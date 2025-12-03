"""
Package installation configuration
"""

from setuptools import setup, find_packages

with open("README.md", "r", encoding="utf-8") as fh:
    long_description = fh.read()

with open("requirements.txt", "r", encoding="utf-8") as fh:
    requirements = [line.strip() for line in fh if line.strip() and not line.startswith("#")]

setup(
    name="mlops-pipeline: DQ pipeline with LSTM autoencoder",
    version="1.0.0",
    author="Roland and Richard Petrasch",
    author_email="roland.petrasch@gmail.com",
    description="MLOps Data Quality Pipeline with LSTM Autoencoder",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/rpetrasch/dqman",
    packages=find_packages(),
    classifiers=[
        "Development Status :: 4 - Beta",
        "Intended Audience :: Developers",
        "Topic :: Software Development :: Libraries :: Python Modules",
        "License :: OSI Approved :: OSC License 1.0",
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.11",
        "Programming Language :: Python :: 3.12",
        "Programming Language :: Python :: 3.13",
    ],
    python_requires=">=3.11",
    install_requires=requirements,
    extras_require={
        "dev": [
            "pytest>=7.0.0",
            "pytest-cov>=4.0.0",
            "black>=22.0.0",
            "flake8>=5.0.0",
            "mypy>=0.990",
        ],
        "viz": [
            "matplotlib>=3.5.0",
            "seaborn>=0.12.0",
            "plotly>=5.0.0",
        ],
    },
    entry_points={
        "console_scripts": [
            "pipeline=pipeline.main:main",
        ],
    },
)