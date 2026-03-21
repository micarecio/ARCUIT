package com.sd.arcuit.logic

class UnionFind(size: Int) {

    private val parent = IntArray(size) { it }
    private val rank = IntArray(size)

    fun find(x: Int): Int {
        if (parent[x] != x) {
            parent[x] = find(parent[x])
        }
        return parent[x]
    }

    fun union(a: Int, b: Int) {
        val rootA = find(a)
        val rootB = find(b)

        if (rootA == rootB) return

        when {
            rank[rootA] < rank[rootB] -> parent[rootA] = rootB
            rank[rootA] > rank[rootB] -> parent[rootB] = rootA
            else -> {
                parent[rootB] = rootA
                rank[rootA]++
            }
        }
    }
}