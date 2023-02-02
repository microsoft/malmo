import os

# Set MALMO_XSD_PATH environment variable.
os.environ["MALMO_XSD_PATH"] = os.path.join(os.path.dirname(__file__), "Schemas")
