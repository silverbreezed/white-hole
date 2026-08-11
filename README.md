# Recover items lost to the Void — with more item recovery options coming soon.

This mod is designed to cure one of Minecraft's ultimate frustrations: **dying in the Void** with absolutely zero chance to recovering your items.

With the **White Hole Altar** and **Cosmic Eye**, you can recover your lost inventory from the Void (and despawned items for the future updates).

![White Hole Altar](https://github.com/silverbreezed/white-hole/blob/26.2/assets/gif/altar.gif?raw=true)

> **Not literally a *white hole* physics object.** Instead, it taps into an invisible cosmic force that expels what a Black Hole (the Void) has swallowed.

---

## 🟥 The Problem

As a survival player, you're exploring **The End**, and you slip and fall into the Void.

In vanilla Minecraft, **the Void destroys everything**. Your hard-earned end-game gear is literally gone forever with absolutely zero chance to recover it.

## 🟩 The Solution: The White Hole Altar

This mod introduces a high-risk, high-reward mechanic that allows you to recover items lost in your most recent Void death.

* **The White Hole Altar:** A mysterious, ancient block generated naturally at the center of **Ancient Cities** in the Overworld. It replaces the single chest normally found at the center of an Ancient City with a simple cosmic structure.

* **The Cosmic Eye:** A mysterious cosmic item that can be found in **Ancient City chests** and offered to the Altar.

* **Void Salvation:** If you die in the Void, your lost inventory is safely stored. Return to the Altar, insert the **Cosmic Eye**, and the White Hole will recover your items!

## 🔎 Where to Find the Cosmic Eye?

The Cosmic Eye can be found naturally in **Ancient City chests**, with a rarity comparable to that of an **Enchanted Golden Apple**.

![Cosmic Eye Item](https://cdn.modrinth.com/data/cached_images/ebec0951770750fb5a3622e5237b0a59e259f39c.png)

---

## ❓ Frequently Asked Questions (FAQ)

**Q: Can this mod recover items that I lost BEFORE installing it?**<br>
A: **No.** The mod can only track and save your inventory data for deaths that occur after the mod has been successfully installed and activated.

**Q: Is this mod multiplayer-friendly?**<br>
A: **Yes**, but currently the mod must be installed on both the server and the client to function properly. However, standalone server support (via Polymer) is actively being developed.

---

## ℹ️ Future Updates

* Players will be able to **recover the last despawned items after dying** by using the White Hole Altar.
* Polymer integration to enable multiplayer servers to use the mod without requiring every player to install the mod on their client.
* GUI configuration with `cloth_config_api` configuration menu for both **Fabric and NeoForge**.
* Port to other Minecraft versions (especially 1.21.11, 26.1.2)

---

## ⚙️ Default Configuration

`minecraft/config/whitehole.json`

```json
{
  "recoverItemsFromEndVoid": true,
  "recoverItemsFromOverworldVoid": false,
  "recoverItemsFromNetherVoid": false,
  "recoverItemsFromDespawn": true,
  "maxSavedItemSnapshots": 3,
  "altarCooldown": false,
  "defaultAltarCooldown": 1200
}
```

* `recoverItemsFromDespawn`: **please note** this is planned for a future update and is currently not functional.
* `maxSavedItemSnapshots`: determines how many full player-inventory snapshots can be saved by the White Hole.
* `defaultAltarCooldown`: determines the default cooldown duration for the Altar.
