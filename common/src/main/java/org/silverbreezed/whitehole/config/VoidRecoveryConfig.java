package org.silverbreezed.whitehole.config;

/**
 * Per-dimension toggles for capturing a player's inventory when they die by falling out
 * of the world (RecoveryReason.VOID_DEATH). Kept separate from despawn recovery since the
 * two are independent capture triggers with independent settings.
 */
public class VoidRecoveryConfig {
    public boolean overworld = false;
    public boolean nether = false;
    public boolean end = true;
}