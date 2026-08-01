package io.github.urntt.litematicacreator.creator;

final class CreatorInventorySaveGate
{
    private int depth;
    private boolean pending;

    void begin()
    {
        ++this.depth;
    }

    boolean markChanged()
    {
        if (this.depth > 0)
        {
            this.pending = true;
            return false;
        }

        return true;
    }

    boolean end()
    {
        if (this.depth <= 0)
        {
            throw new IllegalStateException("Creator inventory transaction underflow");
        }

        --this.depth;

        if (this.depth == 0 && this.pending)
        {
            this.pending = false;
            return true;
        }

        return false;
    }
}
