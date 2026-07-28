package dev.chunkdoctor.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ChunkKeyTest {
    @Test
    void worldRenameDoesNotChangeIdentity() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        assertEquals(new ChunkKey(id, "old_name", 4, -7), new ChunkKey(id, "new_name", 4, -7));
    }

    @Test
    void coordinatesParticipateInIdentity() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        assertNotEquals(new ChunkKey(id, "world", 4, -7), new ChunkKey(id, "world", 5, -7));
    }
}
