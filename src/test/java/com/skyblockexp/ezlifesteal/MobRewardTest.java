package com.skyblockexp.ezlifesteal;

import com.skyblockexp.ezlifesteal.model.MobReward;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobRewardTest {

    @Test
    void constructorRejectsNullEntityType() {
        assertThrows(
                NullPointerException.class,
                () -> new MobReward(null, 1.0, Set.of(), Set.of(), null)
        );
    }

    @Test
    void gettersReturnConstructorValues() {
        MobReward reward = new MobReward(EntityType.BLAZE, 2.5, Set.of(), Set.of(), "ezlifesteal.mob.blaze");

        assertEquals(EntityType.BLAZE, reward.getEntityType());
        assertEquals(2.5, reward.getHearts());
        assertEquals("ezlifesteal.mob.blaze", reward.getPermission());
    }

    @Test
    void constructorCreatesDefensiveCopiesOfInputSets() {
        Set<String> allowed = new HashSet<>(Set.of("arena"));
        Set<String> blocked = new HashSet<>(Set.of("nether"));
        MobReward reward = new MobReward(EntityType.ZOMBIE, 1.0, allowed, blocked, null);

        allowed.add("mines");
        blocked.clear();

        assertTrue(reward.isWorldAllowed("arena"));
        assertFalse(reward.isWorldAllowed("nether"));
        assertFalse(reward.isWorldAllowed("mines"));
    }

    @Test
    void nullAndEmptyWorldSetsBehaveEquivalently() {
        MobReward nullWorldSets = new MobReward(EntityType.ZOMBIE, 1.0, null, null, null);
        MobReward emptyWorldSets = new MobReward(EntityType.ZOMBIE, 1.0, Set.of(), Set.of(), null);

        for (String worldName : new String[]{null, "", "spawn", "NETHER"}) {
            assertTrue(nullWorldSets.isWorldAllowed(worldName));
            assertTrue(emptyWorldSets.isWorldAllowed(worldName));
        }
    }

    @Test
    void nullAndBlankWorldNamesAreOnlyAllowedWhenAllowedWorldsIsEmpty() {
        MobReward unrestricted = new MobReward(EntityType.ZOMBIE, 1.0, Set.of(), Set.of(), null);
        assertTrue(unrestricted.isWorldAllowed(null));
        assertTrue(unrestricted.isWorldAllowed("   "));

        MobReward restricted = new MobReward(EntityType.ZOMBIE, 1.0, Set.of("world"), Set.of(), null);
        assertFalse(restricted.isWorldAllowed(null));
        assertFalse(restricted.isWorldAllowed("\t"));
    }

    @Test
    void blockedWorldRejectsWhenAllowedWorldsIsEmpty() {
        MobReward reward = new MobReward(EntityType.ZOMBIE, 1.0, Set.of(), Set.of("world_nether"), null);

        assertFalse(reward.isWorldAllowed("WORLD_NETHER"));
        assertTrue(reward.isWorldAllowed("world"));
    }

    @Test
    void nonEmptyAllowedWorldsRejectsWorldsNotPresent() {
        MobReward reward = new MobReward(EntityType.ZOMBIE, 1.0, Set.of("spawn", "arena"), Set.of(), null);

        assertTrue(reward.isWorldAllowed("spawn"));
        assertFalse(reward.isWorldAllowed("mines"));
    }

    @Test
    void blockedWorldWinsWhenWorldExistsInBothAllowedAndBlocked() {
        MobReward reward = new MobReward(EntityType.ZOMBIE, 1.0, Set.of("arena"), Set.of("arena"), null);

        assertFalse(reward.isWorldAllowed("arena"));
    }

    @Test
    void worldNameInputIsNormalizedToLowercase() {
        MobReward reward = new MobReward(EntityType.ZOMBIE, 1.0, Set.of("skyworld"), Set.of("darkzone"), null);

        assertTrue(reward.isWorldAllowed("SkYwOrLd"));
        assertFalse(reward.isWorldAllowed("DaRkZoNe"));
    }

    @Test
    void caseVariantAndDuplicateWorldNamesAreCollapsedByNormalization() {
        MobReward reward = new MobReward(
                EntityType.ZOMBIE,
                1.0,
                Set.of("Arena", "ARENA", "arEna"),
                Set.of("NETHER", "nether"),
                null
        );

        assertTrue(reward.isWorldAllowed("arena"));
        assertFalse(reward.isWorldAllowed("NETHER"));
        assertFalse(reward.isWorldAllowed("overworld"));
    }

    @Test
    void nullAndBlankEntriesInWorldSetsAreIgnoredDuringNormalization() {
        Set<String> allowed = new HashSet<>();
        allowed.add(null);
        allowed.add("   ");

        Set<String> blocked = new HashSet<>();
        blocked.add(null);
        blocked.add("\t");

        MobReward reward = new MobReward(EntityType.ZOMBIE, 1.0, allowed, blocked, null);

        assertTrue(reward.isWorldAllowed(null));
        assertTrue(reward.isWorldAllowed("world"));
    }
}
