# PURPLE HAZE

A phone-first 2D exploration platformer prototype built with Python + Pygame.

## Current prototype
- Purple floating-island atmosphere and scrolling camera
- Keyboard controls and persistent on-screen touch controls
- Jumping, platform collision, and simple fall recovery
- Three collectible crystals
- ECHO, a local scripted guide with several dialogue lines
- Portal objective and a mini-map showing the player and remaining crystals

## Controls
- Move: A/D or Left/Right arrows; use the on-screen arrows on mobile
- Jump: Space, W, Up arrow, or the on-screen JUMP button
- Talk to ECHO: E or the on-screen ECHO button when nearby
- Exit desktop test: Escape

## Run on Android with Pydroid 3
1. Install Pydroid 3 and its Pygame support/plugin.
2. Open `main.py` from this folder.
3. Run in landscape orientation.

## Run on desktop
```bash
python -m pip install -r requirements.txt
python main.py
```

## Android APK build
The GitHub Actions workflow named **Build PURPLE HAZE Android APK** attempts to build a debug APK and upload it as an artifact. Check the Actions tab on the `purple-haze` branch for the run result. A workflow file is not proof that an APK has built: the artifact appears only after a successful build.

## Project status
This is an early playable prototype, not a finished game. ECHO currently uses local dialogue and is not connected to an AI service. A future AI connection must use a secure backend; never put API keys in this repository or inside an APK.
