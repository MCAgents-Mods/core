package io.github.mcagentsmods.core.common.hologram;

/**
 * Which part of a longer column of rows or lines shows, and the scrollbar beside it. The
 * window holds {@code visible} items, starts at {@link #offset()}, and never runs past either
 * end.
 *
 * <p>A window can follow the end: while it shows the last item, growing the content keeps it on
 * the last item, as a conversation does when a reply arrives. Scrolling up stops the following
 * and scrolling back to the end restarts it. One instance belongs to one open panel and is
 * used on the client thread only.
 */
public final class ScrollWindow {

    private final int visible;
    private final boolean followEnd;
    private int total;
    private int offset;

    /**
     * An empty window.
     *
     * @param visible   how many items show at once, at least 1
     * @param followEnd whether the window stays on the end while the content grows
     */
    public ScrollWindow(int visible, boolean followEnd) {
        if (visible < 1) {
            throw new IllegalArgumentException("visible must be at least 1");
        }
        this.visible = visible;
        this.followEnd = followEnd;
    }

    /** @return how many items show at once */
    public int visible() {
        return visible;
    }

    /** @return how many items there are */
    public int total() {
        return total;
    }

    /** @return the first item showing */
    public int offset() {
        return offset;
    }

    /** @return the furthest the window scrolls */
    public int maxOffset() {
        return Math.max(0, total - visible);
    }

    /** @return whether the last item shows */
    public boolean atEnd() {
        return offset == maxOffset();
    }

    /**
     * Sets how many items there are, keeping the window inside them, and on the end if it
     * follows the end and was there.
     *
     * @param total the new count, not negative
     */
    public void setTotal(int total) {
        if (total < 0) {
            throw new IllegalArgumentException("total must not be negative");
        }
        boolean stay = followEnd && atEnd();
        this.total = total;
        offset = stay ? maxOffset() : Math.min(offset, maxOffset());
    }

    /**
     * Scrolls by whole items, stopping at either end.
     *
     * @param items how far, positive towards the end
     * @return whether the window moved
     */
    public boolean scrollBy(int items) {
        int before = offset;
        scrollTo((long) offset + items);
        return offset != before;
    }

    /**
     * Scrolls so {@code item} is the first showing, as near as the ends allow.
     *
     * @param item the item wanted first
     */
    public void scrollTo(long item) {
        offset = (int) Math.max(0, Math.min(maxOffset(), item));
    }

    /**
     * Scrolls the least that brings {@code item} into the window.
     *
     * @param item the item to show; one outside the content is ignored
     */
    public void reveal(int item) {
        if (item < 0 || item >= total) {
            return;
        }
        if (item < offset) {
            offset = item;
        } else if (item >= offset + visible) {
            offset = item - visible + 1;
        }
    }

    /**
     * The thumb's length on a scrollbar track: the share of the content that shows, and the
     * whole track when everything fits.
     *
     * @param track    the track's length
     * @param minThumb the shortest the thumb gets
     * @return the thumb's length
     */
    public int thumbLength(int track, int minThumb) {
        int shown = Math.max(total, visible);
        return Math.min(track, Math.max(minThumb, (int) ((long) track * visible / shown)));
    }

    /**
     * Where the thumb starts on the track.
     *
     * @param track    the track's length
     * @param minThumb the shortest the thumb gets
     * @return the distance from the track's start
     */
    public int thumbStart(int track, int minThumb) {
        int travel = track - thumbLength(track, minThumb);
        return maxOffset() == 0 ? 0 : (int) ((long) travel * offset / maxOffset());
    }

    /**
     * Scrolls so the thumb's middle sits at a point on the track, as a click on it does.
     *
     * @param position the point, as a distance from the track's start
     * @param track    the track's length
     * @param minThumb the shortest the thumb gets
     */
    public void scrollToTrack(double position, int track, int minThumb) {
        int thumb = thumbLength(track, minThumb);
        int travel = track - thumb;
        if (travel <= 0 || maxOffset() == 0) {
            return;
        }
        double share = (position - thumb / 2.0) / travel;
        scrollTo(Math.round(Math.max(0, Math.min(1, share)) * maxOffset()));
    }
}
