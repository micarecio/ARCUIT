package com.sd.arcuit.logic

class UnionFind(size: Int) {

    private val parent = IntArray(size)

    init {
        for (i in 0 until size) {
            parent[i] = i
        }
    }

    fun find(x: Int): Int {
        if (parent[x] != x) {
            parent[x] = find(parent[x])
        }
        return parent[x]
    }

    fun union(a: Int, b: Int) {
        val rootA = find(a)
        val rootB = find(b)

        if (rootA != rootB) {
            parent[rootB] = rootA
        }
    }
}