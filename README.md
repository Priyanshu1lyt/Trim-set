# Armor Trim & Ore Customizer

Fabric mod for Minecraft Java **1.21.11**.

Press **M** (rebindable in Options → Controls → Key Binds) to open the customizer:

- Left: 3D preview of your real character wearing your real armor, updating live as you pick a trim + material.
- Right: **TRIMS** tab (every vanilla pattern, with template icons) and **ORES** tab (every vanilla trim material, with item icons). Picking a trim moves you on to ORES.
- **SAME TRIM FOR ALL ARMOR** toggle (default OFF). ON = one trim + one material applied to helmet, chestplate, leggings and boots. OFF = each piece configured independently.
- **Apply** / **Cancel**, with clear status messages (missing armor, incomplete selection, success).

Trims are applied with Minecraft's real armor trim data component. Protection, toughness, enchantments, durability, names and all other item data are untouched, and the same item stack is modified in place (no replacing, no duplication).

The mod must be installed on the server as well for multiplayer (singleplayer works out of the box).

## Build

GitHub Actions builds the jar on every push: open the **Actions** tab, pick the latest run and download the `ArmorTrimCustomizer` artifact.
