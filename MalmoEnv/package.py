from pathlib import Path

version = Path('../VERSION').read_text().strip() 
Path('malmoenv/version.py').write_text('malmo_version="{}"'.format(version))

