# Phantom Armor Client

This is the required **Fabric client mod** for the Phantom Armor Paper plugin.

## What it does

- Shows the unlocked ability list in the bottom-left corner.
- Shows the selected ability and its cooldown at bottom-center.
- Pressing **Left Alt** once selects the next ability. It does not use Ctrl, mouse-wheel scrolling, or the right Alt key.

## Install

1. Run `gradlew.bat build` in this folder (or use the prebuilt JAR from `build/libs` once it has been built).
2. Install Fabric Loader for Minecraft **1.21.4** and Fabric API **0.119.4+1.21.4**.
3. Put `PhantomArmorClient-1.0.0.jar` in the Minecraft client's `mods` folder, along with Fabric API.
4. Put the rebuilt `../target/PhantomArmor-1.1.0.jar` in the Paper server's `plugins` folder.

Every player who wants the custom controls and HUD needs this client mod. Players without it can still join the Paper server, but will not have the custom HUD or Left Alt ability selection.
