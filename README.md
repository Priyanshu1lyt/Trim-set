# Armor Trim & Ore Customizer

Fabric mod for Minecraft Java **1.21.11**.

**Authors:** NotHeartYT, Praveen_god_67, Nebrion

Press **M** (rebindable in Options → Controls → Key Binds) to open the customizer:

- Left: 3D preview of your real character wearing your real armor, updating live as you pick a trim + material.
- Right: **TRIMS** tab (every vanilla pattern, with template icons) and **ORES** tab (every vanilla trim material, with item icons). Picking a trim moves you on to ORES.
- **SAME TRIM FOR ALL ARMOR** toggle (default OFF). ON = one trim + one material applied to helmet, chestplate, leggings and boots. OFF = each piece configured independently.
- **Apply** / **Cancel**, with clear status messages.

## Server use
The mod is **client-side only**. It does NOT need to be installed on the server.
You only need to be an **operator (OP)** on the server. Apply uses the vanilla `/item replace` command,
and the server itself decides whether you are allowed to run it.
In singleplayer, enable *Allow Cheats*.

Every existing component of the armor (enchantments, durability, custom name, lore, etc.) is copied,
only the trim changes.

## Build
GitHub Actions builds the jar on every push: open the **Actions** tab, pick the latest run and download the `ArmorTrimCustomizer` artifact.
