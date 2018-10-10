package be.intimals.freqt.core;

import java.util.Objects;

public class Extension {
    private int pos;
    private String label;
    private boolean isRightmost;
    private int prevSibling;

    /**
     * Represents a possible extension by 1 vertex of a tree.
     * @param pos
     * @param label
     * @param isRightmost
     * @param prevSibling
     */
    public Extension(int pos, String label, boolean isRightmost, int prevSibling) {
        this.pos = pos;
        this.label = label;
        this.isRightmost = isRightmost;
        this.prevSibling = prevSibling;

    }

    public Extension(int pos, String label, boolean isRightmost) {
        this(pos, label, isRightmost, -1);
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isRightmost() {
        return isRightmost;
    }

    public void setRightmost(boolean rightmost) {
        isRightmost = rightmost;
    }

    public int getPreviousSibling() {
        return prevSibling;
    }

    public void setPreviousSibling(int previousSibling) {
        this.prevSibling = previousSibling;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Extension extension = (Extension) o;
        return pos == extension.pos
                && label.equals(extension.label)
                && prevSibling == extension.prevSibling;
    }

    @Override
    public int hashCode() {
        // Do not use isRightmost in comparison
        return Objects.hash(pos, label, prevSibling);
    }

    @Override
    public String toString() {
        return "{" + pos
                + ", " + label
                + ", " + isRightmost
                + ", " + prevSibling
                + '}';
    }
}
