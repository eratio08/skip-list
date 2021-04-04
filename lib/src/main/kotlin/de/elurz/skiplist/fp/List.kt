package de.elurz.skiplist.fp

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed class List<out A> {
    object Nil : List<Nothing>() {
        override fun toString(): String = "NIL"
    }

    data class Cons<A>(val head: A, val tail: List<A>) : List<A>()

    companion object {
        fun <A> of(vararg xs: A): List<A> {
            val head = xs[0]
            val tail = xs.sliceArray(1 until xs.size)
            return when {
                tail.isEmpty() -> Cons(head, Nil)
                else -> Cons(head, of(*tail))
            }
        }

        fun <A> empty(): List<A> = Nil

        @ExperimentalContracts
        fun <A> isEmpty(xs: List<A>): Boolean {
            contract { returns(false) implies (xs is Cons) }
            return when (xs) {
                is Nil -> true
                is Cons -> false
            }
        }

        fun length(list: List<*>): Int =
            foldLeft({ acc, _ -> acc + 1 }, 0, list)

        tailrec fun <A, B> foldLeft(f: (B, A) -> B, acc: B, xs: List<A>): B =
            when (xs) {
                is Nil -> acc
                is Cons -> foldLeft(f, f(acc, xs.head), xs.tail)
            }

        fun <A, B> foldRight(f: (B, A) -> B, acc: B, xs: List<A>): B =
            foldLeft({ g: Identity<B>, a: A ->
                { b: B -> g(f(b, a)) }
            }, { b: B -> b }, xs)(acc)

        fun <A, B> map(f: (A) -> B, xs: List<A>): List<B> =
            foldRight({ acc, x -> Cons(f(x), acc) }, empty(), xs)

        fun <A> head(xs: List<A>): A? =
            when (xs) {
                is Nil -> null
                is Cons -> xs.head
            }

        fun <A> tail(xs: List<A>): List<A> =
            when (xs) {
                is Nil -> Nil
                is Cons -> xs.tail
            }

        fun <A> filter(p: (A) -> Boolean, xs: List<A>): List<A> =
            foldLeft({ acc, x -> if (p(x)) Cons(x, acc) else acc }, empty(), xs)

        fun <A> get(index: Int, xs: List<A>): A? {
            tailrec fun rec(index: Int, xs: List<A>, currentIndex: Int = 0): A? =
                when (xs) {
                    is Nil -> null
                    is Cons -> if (index == currentIndex) xs.head else rec(index, xs.tail, currentIndex + 1)
                }
            return rec(index, xs)
        }

        fun <A> reverse(xs: List<A>): List<A> =
            foldLeft({ acc, x -> Cons(x, acc) }, empty(), xs)

        fun <A> takeWhile(p: (A) -> Boolean, xs: List<A>): List<A> {
            tailrec fun rec(p: (A) -> Boolean, xs: List<A>, acc: List<A> = empty()): List<A> =
                when (xs) {
                    is Nil -> acc
                    is Cons -> if (p(xs.head)) {
                        rec(p, xs.tail, Cons(xs.head, acc))
                    } else {
                        acc
                    }
                }

            return rec(p, xs)
        }

        fun <A, B> zip(xs: List<A>, ys: List<B>): List<Pair<A, B>> {
            tailrec fun <A, B> rec(xs: List<A>, ys: List<B>, acc: List<Pair<A, B>> = empty()): List<Pair<A, B>> =
                when {
                    xs is Nil -> acc
                    ys is Nil -> acc
                    xs is Cons && ys is Cons -> {
                        rec(xs.tail, ys.tail, Cons(xs.head to ys.head, acc))
                    }
                    else -> throw IllegalStateException("Just for the type system")
                }
            return reverse(rec(xs, ys))
        }

        fun <A> index(xs: List<A>): List<Pair<Int, A>> =
            zip(of(*(0 until length(xs)).toArray()), xs)

        fun <A> take(n: Int, xs: List<A>): List<A> {
            tailrec fun rec(n: Int, xs: List<A>, acc: List<A> = empty()): List<A> = if (n <= 0) {
                acc
            } else {
                when (xs) {
                    is Nil -> acc
                    is Cons -> rec(n - 1, xs.tail, Cons(xs.head, acc))
                }
            }
            return reverse(rec(n, xs))
        }

        fun <A> drop(n: Int, xs: List<A>): List<A> {
            tailrec fun rec(n: Int, xs: List<A>): List<A> =
                if (n <= 0) {
                    xs
                } else {
                    when (xs) {
                        is Nil -> xs
                        is Cons -> rec(n - 1, xs.tail)
                    }
                }

            return rec(n, xs)
        }

        fun <A> splitAt(n: Int, xs: List<A>): Pair<List<A>, List<A>> {
            return ((take(n, xs)) to (drop(n, xs)))
        }

        fun <A> append(xs: List<A>, ys: List<A>): List<A> =
            foldRight({ acc, x -> Cons(x, acc) }, ys, xs)

        fun <A> add(i: Int, x: A, xs: List<A>): List<A> {
            if (i >= length(xs)) {
                return xs
            }
            val (head, tail) = splitAt(i, xs)
            return append(head, Cons(x, tail))
        }
    }
}

fun IntProgression.toArray(): Array<Int> =
    this.toList().toTypedArray()

typealias Identity<A> = (A) -> A

// Tests
fun main() {
    val l = List.of(1, 2, 3, 4, 5, 6)

    // length
    val length = List.length(l)
    check(length == 6) { "Not expected length, but was $length" }

    val emptyLength = List.length(List.Nil)
    check(emptyLength == 0) { "Not empty, but was $length" }

    val reduced = List.foldLeft({ acc, x -> "$acc$x" }, "", l)
    check(reduced.length == 6) { "Has not expected size, but was ${reduced.length}" }

    val mapped = List.map({ it.toString() }, l)
    check(List.head(mapped) == "1")

    val atIndex = List.get(2, l)
    check(atIndex == 3) { "Was not expected value, was $atIndex" }

    val taken = List.take(2, l)
    check(List.length(taken) == 2)

    val zipped = List.zip(l, List.of("a", "b", "c", "d", "e", "f"))
    check(List.length(zipped) == 6)
    check(List.head(zipped) == 1 to "a")

    val indexed = List.index(List.of("a", "b"))
    check(List.head(indexed) == 0 to "a")

    val (l1, l2) = List.splitAt(3, l)
    check(List.length(l1) == 3)
    check(List.length(l2) == 3)

    val newL = List.add(2, 23, l)
    check(List.get(2, newL) == 23)
}