package org.silverbreezed.whitehole.enums;

/**
 * New capture sources (e.g. despawn recovery) should add a new constant here instead
 * of introducing a parallel storage/cache pathway alongside ItemSnapshotManager.
 */
public enum RecoveryReason {
    VOID_DEATH,

    // Reserved for the upcoming despawn-recovery feature (see ModConfig.DespawnRecoveryConfig
    // and README "Future Updates"). Not yet produced by any capture trigger.
    DESPAWN
}