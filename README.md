# Recover your Despawned Items or Items Lost to the End Void

This mod is designed to cure two of Minecraft's ultimate frustrations:
- **Dying and losing hard-earned items** when they **despawn** before you can reach them
- **Dying in the Void** and lost the items with absolutely zero chance to recover it

With the **White Hole Altar** and **Cosmic Eye**, you can recover despawned items after dying and your lost inventory after falling into the Void.

![White Hole Altar](https://raw.githubusercontent.com/silverbreezed/assets/refs/heads/main/altar.gif)

> **Not literally a *white hole* physics object.** Instead, it taps into an invisible cosmic force that expels what time or the Void has swallowed.

[![Available for Fabric](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.3.1/assets/cozy/supported/fabric_vector.svg)](https://modrinth.com/mod/white-hole/versions?l=fabric) [![Available for NeoForge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.3.1/assets/cozy/supported/neoforge_vector.svg)](https://modrinth.com/mod/white-hole/versions?l=neoforge) [![Available for Forge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.3.1/assets/cozy/supported/forge_vector.svg)](https://modrinth.com/mod/white-hole/versions?l=forge)

---

## 🟥 The Frustration of Vanilla Minecraft

* **Despawn Timer:** You die far from spawn. You sprint and navigate back to your death point, but your items have already despawned and literally gone forever.
* **The End Void:** You slip while bridging across **The End**, and your hard-earned gear lost into the void with zero chance of recovery.

Vanilla Minecraft offers no second chances when time runs out or the void takes your loot. **White Hole gives you a lore-friendly way to get it back.**

## 🟩 The Solution: The White Hole Altar

This mod introduces a high-risk, high-reward mechanic that allows you to recover items from most recent despawned items after dying, and recent death into the Void.

* **The White Hole Altar:** A structure naturally generated at the center of **Ancient Cities** in the Overworld.
* **The Cosmic Eye:** A rare artifact to open the white hole hidden in **Ancient City chests**

## 🔎 Finding the Cosmic Eye

The Cosmic Eye can be found naturally in **Ancient City chests**, with a rarity comparable to an **Enchanted Golden Apple**.

![Cosmic Eye Item](https://cdn.modrinth.com/data/cached_images/4979d97829a49d152340f00b7f0ebbdc399b1117.png)


## 🛠️ Crafting the Cosmic Eye

![Cosmic Eye Recipe](https://cdn.modrinth.com/data/cached_images/f2163e4c09a9aff3d5a99c9175087fabc59e4fcb.png)

---

## ❓ Frequently Asked Questions (FAQ)

<details>
  <summary>Can this mod recover items that I lost BEFORE installing it?</summary>

  **No**. The mod can only track and save your inventory data for deaths that occur after the mod has been successfully installed and activated.
</details>

<details> 
  <summary>Why are my items not recorded immediately if I die very far away?</summary>
  
  The mod only records items when they actually despawn. The vanilla despawn timer depends on the player's simulation distance, chunks that are too far away will freeze. The mod will only record and save your items once you return to the death location and the despawn timer finishes counting down.
</details>

<details>
  <summary>Is this mod multiplayer-friendly?</summary>

  **Yes**, but currently the mod must be installed on both the server and the client. Standalone server-side support (via Polymer) is in active development.
</details>

<details>
  <summary>What happens if I die multiple times before visiting the Altar?</summary>

  The mod stores up to `maxSavedItemSnapshots` (3 by default) in history. This means the white hole will save your full inventory for the maximum 3 snapshots of death for each category.
</details>

<details>
  <summary>Does this save items that I drop manually with 'Q' (default drop key)?</summary>

  **No**. The despawn tracker specifically tags items dropped upon a player's death. Normal manual drops, thrown items, and automated farm drops will despawn naturally without being saved.
</details>

---

## ℹ️ Future Updates

* **Polymer Integration:** Enable multiplayer servers to use the mod without requiring every player to install it on their client (**FABRIC ONLY**).
* **Snapshot Selection Menu:** Allow players to view and select which specific death snapshot they want to restore, instead of automatically recovering only the latest one.
* **In-Game Configuration UI:** Adding a config screen using `cloth_config_api` for both **Fabric and NeoForge**.
* **Command**: `/wh reload` command to reload configuration directly.

---

## ⚙️ Default Configuration

`minecraft/config/whitehole.json`

```json
{
  "voidRecovery": {
    "overworld": false,
    "nether": false,
    "end": true,
    "maxSnapshots": 3
  },
  "despawnRecovery": {
    "enabled": true,
    "maxSnapshots": 3
  },
  "altarCooldown": false,
  "defaultAltarCooldown": 1200
}
```

`voidRecovery`: per-dimension toggles for capturing your inventory when you die by falling out of the world. Defaults to The End only, since void deaths elsewhere are rare — server owners can enable the other dimensions if their playerbase wants that extra safety net.

`despawnRecovery`: universal safety net for items dropped on any death that nobody retrieves before they'd naturally despawn. Items still drop and can be looted normally in the meantime by anyone nearby — only items that would otherwise be lost to despawn are captured, regardless of how long your server is configured to keep items on the ground.

`maxSavedItemSnapshots`: determines how many full player-inventory snapshots can be saved by the White Hole.

`defaultAltarCooldown`: determines the default cooldown duration for the Altar.

> Upgrading from an older version? If your existing whitehole.json still uses the old flat keys (recoverItemsFromEndVoid, etc.), it will be automatically migrated into the nested shape above the first time the server loads — your existing settings are preserved.
