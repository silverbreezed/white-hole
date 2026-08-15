package org.silverbreezed.whitehole.config;

/**
 * Per-dimension toggles for capturing a player's inventory when they die by falling out
 * of the world (RecoveryReason.VOID_DEATH). Kept separate from despawn recovery since the
 * two are independent capture triggers with independent settings - including their own
 * snapshot capacity, so one reason filling up never blocks the other.
 */
public class VoidRecoveryConfig {
    public boolean overworld = false;
    public boolean nether = false;
    public boolean end = true;

    /** How many DESPAWN snapshots a player can have stored at once
     *  */
    public int maxSnapshots = 3;
}