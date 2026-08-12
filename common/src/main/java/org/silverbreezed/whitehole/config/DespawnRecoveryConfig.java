package org.silverbreezed.whitehole.config;

/**
 * Reserved for the upcoming despawn-recovery feature (see README "Future Updates").
 *
 * Not yet read by any capture trigger - defined here now so the config shape is already
 * in its final form before the feature lands, avoiding a second breaking config migration.
 */
public class DespawnRecoveryConfig {
    public boolean enabled = true;

    /** Ticks after which a dropped item is captured, just before vanilla's despawn timer. */
    public int thresholdTicks = 5900;

    /** Radius (in blocks) around a player scanned for despawning items. */
    public int radius = 16;
}