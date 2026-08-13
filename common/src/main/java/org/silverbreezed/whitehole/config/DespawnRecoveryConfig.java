package org.silverbreezed.whitehole.config;

/**
 * Settings for the despawn-recovery feature (see README "Future Updates").
 *
 * Deliberately has no tick-threshold or radius setting: DespawnCaptureTrigger hooks the
 * actual vanilla despawn decision point (per-item, wherever it drifts to) instead of
 * guessing a fixed time/area, so it stays correct no matter what despawn duration a server
 * configures. Also deliberately has no death-cause exception (e.g. PvP) - it's universal:
 * any death's items are eligible, the only condition is whether anyone retrieves them
 * before they'd naturally despawn.
 */
public class DespawnRecoveryConfig {
    public boolean enabled = true;
}