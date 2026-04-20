package com.skyblockexp.ezlifesteal;

import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LifestealProfileTest {

    @Test
    void setHeartsMarksProfileAsDirty() {
        LifestealProfile profile = new LifestealProfile(UUID.randomUUID(), 10.0);

        profile.setHearts(12.0);

        assertEquals(12.0, profile.getHearts());
        assertTrue(profile.isDirty());
    }

    @Test
    void addHeartsClampsAtMaxAndMarksProfileAsDirty() {
        LifestealProfile profile = new LifestealProfile(UUID.randomUUID(), 9.0);

        profile.addHearts(5.0, 12.0);

        assertEquals(12.0, profile.getHearts());
        assertTrue(profile.isDirty());
    }

    @Test
    void removeHeartsClampsAtMinAndMarksProfileAsDirty() {
        LifestealProfile profile = new LifestealProfile(UUID.randomUUID(), 6.0);

        profile.removeHearts(10.0, 2.0);

        assertEquals(2.0, profile.getHearts());
        assertTrue(profile.isDirty());
    }

    @Test
    void overwriteHeartsFromStorageSetsHeartsAndClearsDirty() {
        LifestealProfile profile = new LifestealProfile(UUID.randomUUID(), 10.0);
        profile.addHearts(1.0, 20.0);
        assertTrue(profile.isDirty());

        profile.overwriteHeartsFromStorage(7.0);

        assertEquals(7.0, profile.getHearts());
        assertFalse(profile.isDirty());
    }

    @Test
    void dirtyFlagTogglesAcrossMutateOverwriteMutateSequence() {
        LifestealProfile profile = new LifestealProfile(UUID.randomUUID(), 10.0);
        assertFalse(profile.isDirty());

        profile.removeHearts(2.0, 1.0);
        assertEquals(8.0, profile.getHearts());
        assertTrue(profile.isDirty());

        profile.overwriteHeartsFromStorage(15.0);
        assertEquals(15.0, profile.getHearts());
        assertFalse(profile.isDirty());

        profile.addHearts(3.0, 20.0);
        assertEquals(18.0, profile.getHearts());
        assertTrue(profile.isDirty());
    }
}
