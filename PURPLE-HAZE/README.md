# PURPLE HAZE 2.0

A phone-first 2D atmospheric exploration platformer prototype built with Python + Pygame.

## What's new in 2.0
- New title screen and Vesper / Shattered Isles atmosphere
- THE HOLLOW silhouette with violet glow and drifting shadow trail
- HILPO companion with smooth follow behavior and status display
- Four collectible Veil shards and a portal objective
- ECHO encounter with multi-line dialogue
- HILPO scan pulse and feedback messages
- Short shadow dash with cooldown
- Health indicators, particle bursts, layered platforms, stars and atmospheric haze
- Touch controls plus keyboard support
- Nearest-neighbor scaling to keep edges crisp

## Controls
- Move: A/D or Left/Right arrows; mobile left/right buttons
- Jump: Space, W, Up arrow, or JUMP
- Interact / talk: E or F, or ACT
- Scan: Q or SCAN
- Shadow dash: Left or Right Shift
- Exit / return to title: Escape

## Android (Pydroid 3)
1. Install Pydroid 3 and its Pygame support/plugin.
2. Download this folder or open `main.py` from the repository.
3. Run `main.py` in landscape orientation.

## Desktop
```bash
python -m pip install -r requirements.txt
python main.py
```

## Prototype status
This is a playable 2.0 mechanics and atmosphere prototype. Character art, environments, effects and voices are currently drawn/generated at runtime with Pygame primitives; they are not the final high-quality pixel-art asset set. HILPO uses scripted feedback, not a connected AI model. Audio, save/load, full creature taming, localization and the complete story are not implemented yet.

## Android APK
The repository's GitHub Actions workflow may attempt an Android build. A workflow file alone does not guarantee a successful APK. Check the Actions tab for a successful run and downloadable artifact before treating an APK as available.
