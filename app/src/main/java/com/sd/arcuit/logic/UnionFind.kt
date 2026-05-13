package com.sd.arcuit.logic

/**
 * Union-Find (Disjoint Set Union) data structure.
 *
 * Used to efficiently group connected nodes into "nets".
 * Supports:
 * - find(): determine root representative of a set
 * - union(): merge two sets
 */
class UnionFind(size: Int) {

    /**
     * parent[i] points to the parent of node i.
     * Initially, each node is its own parent.
     */
    private val parent = IntArray(size) { it }

    /**
     * rank[i] approximates tree depth for balancing.
     * Used to keep union operations efficient.
     */
    private val rank = IntArray(size)

    /**
     * Finds the root parent of x with path compression.
     *
     * Path compression flattens the structure so future lookups are faster.
     */
    fun find(x: Int): Int {
        if (parent[x] != x) {
            parent[x] = find(parent[x])
        }
        return parent[x]
    }

    /**
     * Merges the sets containing a and b.
     *
     * Uses union by rank to keep trees shallow.
     */
    fun union(a: Int, b: Int) {
        val rootA = find(a)
        val rootB = find(b)

        // Already in the same set
        if (rootA == rootB) return

        // Attach smaller tree under larger tree
        when {
            rank[rootA] < rank[rootB] -> parent[rootA] = rootB
            rank[rootA] > rank[rootB] -> parent[rootB] = rootA

            // Equal rank: choose one root and increase rank
            else -> {
                parent[rootB] = rootA
                rank[rootA]++
            }
        }
    }
}
