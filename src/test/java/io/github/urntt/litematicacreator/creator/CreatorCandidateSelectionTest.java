package io.github.urntt.litematicacreator.creator;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CreatorCandidateSelectionTest
{
    @Test
    void mergesHitAndWriteCandidatesByIdentityInStableOrder()
    {
        Object first = new Object();
        Object second = new Object();
        Object equalButDistinct = new String("same");
        Object otherEqualButDistinct = new String("same");
        List<Object> merged = CreatorCandidateSelection.mergeByIdentity(
                List.of(first, equalButDistinct),
                List.of(first, second, otherEqualButDistinct)
        );

        assertEquals(4, merged.size());
        assertSame(first, merged.get(0));
        assertSame(equalButDistinct, merged.get(1));
        assertSame(second, merged.get(2));
        assertSame(otherEqualButDistinct, merged.get(3));
    }
}
