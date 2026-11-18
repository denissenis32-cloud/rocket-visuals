# ROCKET VISUALS 1.0.0

Client-side visual & audio polish for Minecraft Java **26.3** (Fabric).
Requires: Fabric Loader >= 0.19.3, Fabric API 0.161.0+26.3 (or newer 26.3 build), Java 25.

Open the menu with **Right Shift** (rebind in Options > Controls > Key Binds > ROCKET VISUALS),
via the **ROCKET VISUALS** button on the main menu, or the shortcut button inside Options > Controls.
Settings are saved to `config/rocketvisuals.json`.

## Automatic build (GitHub Actions)
1. Create an empty repository on GitHub and upload ALL files of this project (including the hidden `.github` folder).
2. Open the **Actions** tab -> workflow **build** (it starts on every push; or press **Run workflow**).
3. When the run is green, open it and download **rocketvisuals-jar** from the **Artifacts** section.
   Inside is `rocketvisuals-1.0.0.jar` - put it into `.minecraft/mods` together with Fabric API.

## Local build
    ./gradlew build        (Windows: gradlew.bat build)      needs JDK 25
Result: `build/libs/rocketvisuals-1.0.0.jar`
If the wrapper jar cannot be downloaded: `gradle wrapper --gradle-version 9.5.1`

## Versions used (checked against official Fabric sources)
Minecraft 26.3, Java 25, Gradle 9.5.1, Fabric Loom `net.fabricmc.fabric-loom` 1.17-SNAPSHOT,
Fabric Loader 0.19.5, Fabric API 0.161.0+26.3.
