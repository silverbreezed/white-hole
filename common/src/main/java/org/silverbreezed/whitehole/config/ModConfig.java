package org.silverbreezed.whitehole.config;

public class ModConfig {
    public VoidRecoveryConfig voidRecovery = new VoidRecoveryConfig();
    public DespawnRecoveryConfig despawnRecovery = new DespawnRecoveryConfig();

    public int maxSavedItemSnapshots = 3;

    public boolean altarCooldown = false;
    public int defaultAltarCooldown = 1200;
}