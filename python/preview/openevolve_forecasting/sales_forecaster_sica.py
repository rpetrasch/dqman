import asyncio
import logging
from openevolve import OpenEvolve
from openevolve.config import Config


async def main():
    # Enable Debug Logging aggressively
    logging.basicConfig(level=logging.DEBUG)
    
    # Specifically target Openevolve loggers
    for name in logging.root.manager.loggerDict:
        if "openevolve" in name:
            logging.getLogger(name).setLevel(logging.DEBUG)

    # OpenEvolve will automatically look for 'config.yaml'
    # and use the Ollama settings defined there.
    config = Config.from_yaml("config.yaml")
    evolver = OpenEvolve(
        initial_program_path="sales_forecaster.py",
        evaluation_file="sales_forecaster_evaluator.py",
        config=config,
    )

    print("🚀 Starting evolution via Ollama...")
    best_result = await evolver.run()

    print(f"✅ Evolution complete! Optimized code:\n{best_result}")


if __name__ == "__main__":
    asyncio.run(main())