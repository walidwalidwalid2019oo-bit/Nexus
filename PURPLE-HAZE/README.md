# PURPLE HAZE

A small, phone-first 2D exploration platformer prototype built with Python + Pygame.

## Current prototype
- Purple floating-island atmosphere
- Keyboard and on-screen touch controls
- Jumping and simple platform collision
- Three crystals to collect
- ECHO, a local scripted guide (not connected to an AI service yet)
- Portal objective and a simple minimap

## Run on Android
1. Install Pydroid 3.
2. Install the Pygame plugin for Pydroid 3.
3. Open `main.py` from this folder and run it in landscape orientation.

## Run on desktop
```bash
python -m pip install -r requirements.txt
python main.py
```

## Project status
This is an early prototype, not a finished game or APK. The ECHO character currently uses local dialogue. A real AI connection should be added later through a secure backend; never put API keys in this repository or inside an APK.
