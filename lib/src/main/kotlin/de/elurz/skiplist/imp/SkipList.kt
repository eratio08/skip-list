package de.elurz.skiplist.imp

import java.lang.IllegalStateException
import kotlin.math.*

sealed class Node<V>(val index: Int, val next: MutableList<Node<V>>) {
    class ValueNode<V>(index: Int, var value: V, next: MutableList<Node<V>>) : Node<V>(index, next) {
        override fun hasLowerIndexAs(givenIndex: Int): Boolean = index < givenIndex
    }

    class HeadNode<V>(levels: Int) : Node<V>(Int.MIN_VALUE, next = MutableList(levels) { TailNode as Node<V> }) {
        override fun hasLowerIndexAs(givenIndex: Int): Boolean = true
    }

    object TailNode : Node<Nothing>(Int.MAX_VALUE, next = ArrayList(0)) {
        override fun hasLowerIndexAs(givenIndex: Int): Boolean = false
    }

    abstract fun hasLowerIndexAs(givenIndex: Int): Boolean
}

class SkipList<V>(size: Int = 100, private val p: Double = 0.5) {
    private val maxLevel: Int = floor(log(size.toDouble(), 1 / p)).toInt()
    private val head: Node<V> = Node.HeadNode(maxLevel)

    fun search(searchedIndex: Int): V? {
        var node: Node<V> = head
        val topLevel = node.next.size - 1
        // search levels
        for (i in (topLevel downTo 0)) {
            // search within a level
            while (node.next[i].hasLowerIndexAs(searchedIndex)) {
                node = node.next[i]
            }
        }
        node = node.next[0]
        return if (node.index == searchedIndex && node is Node.ValueNode) {
            node.value as V
        } else {
            null
        }
    }

    fun insert(index: Int, value: V) {
        val update: MutableList<Node<V>> = MutableList(maxLevel) { Node.TailNode as Node<V> }
        var node = head
        val topLevel = node.next.size - 1
        // search levels
        for (i in (topLevel downTo 0)) {
            // search within a level
            while (node.next[i].hasLowerIndexAs(index)) {
                node = node.next[i]
            }
            update[i] = node
        }
        node = node.next[0]
        if (node.index == index && node is Node.ValueNode) {
            node.value = value
        } else {
            val newLevel = randomLevel()
            if (newLevel > head.next.size) {
                // extends levels
                for (i in head.next.size until newLevel) {
                    update[i] = head
                }
                // increase level count by 1
                head.next.add(newLevel - 1, Node.TailNode as Node<V>)
            }
            node = Node.ValueNode(index, value, MutableList(newLevel) { Node.TailNode as Node<V> })
            for (i in 0 until newLevel) {
                // splice
                node.next[i] = update[i].next[i]
                update[i].next[i] = node
            }
        }
    }

    fun delete(index: Int): V? {
        val update: MutableList<Node<V>> = MutableList(maxLevel) { Node.TailNode as Node<V> }
        var node = head
        val topLevel = node.next.size - 1
        // search levels
        for (i in (topLevel downTo 0)) {
            // search within a level
            while (node.next[i].hasLowerIndexAs(index)) {
                node = node.next[i]
            }
            update[i] = node
        }
        node = node.next[0]
        return if (node is Node.ValueNode && node.index == index) {
            // splice
            for (i in 0 until head.next.size) {
                if (update[i].next[i] != node) {
                    break
                }
                update[i].next[i] = node.next[i]
            }
            while (head.next.size > 1 && head.next[head.next.size - 1] == Node.TailNode) {
                head.next.removeAt(head.next.size - 1)
            }
            node.value
        } else {
            null
        }
    }

    private fun randomLevel(): Int {
        var lvl = 1
        while (Math.random() < p && lvl < maxLevel) {
            lvl += 1
        }
        return lvl
    }

    override fun toString(): String {
        val stringBuffer = StringBuilder().append("SkipList [maxLevel=$maxLevel, p=$p,\n")
        for (i in (0 until maxLevel)) {
            stringBuffer.append("\t[HEAD] -> ")
            var node = head.next[i]
            while (node != Node.TailNode) {
                when (node) {
                    is Node.ValueNode -> stringBuffer.append("[${node.value}] -> ")
                    else -> throw IllegalStateException("The use of several HEAD and TAIL nodes is forbidden.")
                }
                node = node.next[i]
            }
            stringBuffer.append("[TAIL]\n")
        }
        stringBuffer.append("]")
        return stringBuffer.toString()
    }
}

fun main() {
    val l = SkipList<Int>()
    for (i in 0 until 20) {
        l.insert(i, (Math.random() * 10).toInt())
    }

    val search1 = l.search(100)
    val search2 = l.search(0)
    check(search1 == null)
    check(search2 != null)
    println(l.toString())

    val deleted1 = l.delete(0)
    val deleted2 = l.delete(21)
    check(deleted1 != null)
    check(deleted2 == null)
    println(l.toString())
}