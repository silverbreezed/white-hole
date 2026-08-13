# Recover your Despawned Items or Items that Lost to the End Void

This mod is designed to cure two of Minecraft's ultimate frustrations:
- Dying and **losing the hard-gained items when they naturally despawn** before you can reach them
- and **dying in the Void** with absolutely zero chance to recover it

With the **White Hole Altar** and **Cosmic Eye**, you can recover despawned items after dying and your lost inventory after falling into the Void.

![White Hole Altar](https://raw.githubusercontent.com/silverbreezed/assets/refs/heads/main/altar.gif)

> **Not literally a *white hole* physics object.** Instead, it taps into an invisible cosmic force that expels what time or the Void has swallowed.

---

## 🟥 The Frustration of Vanilla Minecraft

* **The Brutal Despawn Timer:** You die far from spawn or deep in a treacherous cave. You sprint and navigate back to your death point as fast as humanly possible, but your items was despawned and literally gone forever.
* **The End Void:** You slip while bridging across **The End**, and your hard-gained gear lost into the void with zero chance of recovery.

Vanilla Minecraft offers no second chances when time runs out or the void takes your loot. **White Hole gives you a lore-friendly way to get it back.**

## 🟩 The Solution: The White Hole Altar

This mod introduces a high-risk, high-reward mechanic that allows you to recover items from most recent despawned items after dying, and recent death into the Void.

* **The White Hole Altar:** A mysterious structure naturally generated at the center of **Ancient Cities** in the Overworld.
* **The Cosmic Eye:** A rare artifact to open the white hole hidden in **Ancient City chests**

## 🔎 Finding the Cosmic Eye

The Cosmic Eye can be found naturally in **Ancient City chests**, with a rarity comparable to an **Enchanted Golden Apple**.

![Cosmic Eye Item](https://cdn.modrinth.com/data/cached_images/ebec0951770750fb5a3622e5237b0a59e259f39c.png)

---

## ❓ Frequently Asked Questions (FAQ)

**Q: Can this mod recover items lost BEFORE installing it?**<br>
A: **No.** The mod can only track and save data for deaths and despawns that occur while the mod is active.

**Q: Is this mod multiplayer-friendly?**<br>
A: **Yes**, but currently the mod must be installed on both the server and the client. Standalone server-side support (via Polymer) is in active development.

**Q: What happens if I die multiple times before visiting the Altar?**<br>
A: The mod stores up to `maxSavedItemSnapshots` (3 by default) in history. This means the white hole will save your full inventory for the maximum 3 snapshots of death.

**Q: Does this save items that I drop manually with 'Q' (default drop key)**<br>
A: **No.** The despawn tracker specifically tags items dropped upon a player's death. Normal manual drops, thrown items, and automated farm drops will despawn naturally without being saved.

---

## ℹ️ Future Updates

* Polymer integration to enable multiplayer servers to use the mod without requiring every player to install the mod on their client.
* Snapshot selection menu to allowing players to view and select which specific death snapshot they want to restore instead of automatically recovering only the latest one.
* In-game configuration UI using `cloth_config_api` for both **Fabric and NeoForge**.
* Porting to additional Minecraft versions (especially 1.21.11, 26.1.2).

---

## ⚙️ Default Configuration

`minecraft/config/whitehole.json`

```json
{
  "voidRecovery": {
    "overworld": false,
    "nether": false,
    "end": true
  },
  "despawnRecovery": {
    "enabled": true
  },
  "maxSavedItemSnapshots": 3,
  "altarCooldown": false,
  "defaultAltarCooldown": 1200
}
```

`voidRecovery`: per-dimension toggles for capturing your inventory when you die by falling out of the world. Defaults to The End only, since void deaths elsewhere are rare — server owners can enable the other dimensions if their playerbase wants that extra safety net.

`despawnRecovery`: universal safety net for items dropped on any death that nobody retrieves before they'd naturally despawn. Items still drop and can be looted normally in the meantime by anyone nearby — only items that would otherwise be lost to despawn are captured, regardless of how long your server is configured to keep items on the ground.

`maxSavedItemSnapshots`: determines how many full player-inventory snapshots can be saved by the White Hole.

`defaultAltarCooldown`: determines the default cooldown duration for the Altar.

> Upgrading from an older version? If your existing whitehole.json still uses the old flat keys (recoverItemsFromEndVoid, etc.), it will be automatically migrated into the nested shape above the first time the server loads — your existing settings are preserved.