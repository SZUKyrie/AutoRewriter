package org.autorewriter.rewriter.rule.match;

/**
 * Represents a linearized (left-deep) join tree for join permutation matching.
 *
 * <p>Currently a placeholder. The core join matching (direct + flip) is handled
 * in {@link Match#matchJoin}. Full LinearJoinTree permutation matching can be
 * added later for multi-way join trees where more than two orderings need to
 * be considered.
 */
public class LinearJoinTree {
    // TODO: Full implementation for multi-way join permutation
    // For now, Match.matchJoin handles direct + flipped matching
}
