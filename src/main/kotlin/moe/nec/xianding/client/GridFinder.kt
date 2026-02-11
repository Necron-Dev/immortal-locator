package moe.nec.xianding.client

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class GridFinder(var originX: Double, var originZ: Double, var ratio: Double, var signX: Int) {
    var x: Double = 0.0
    var z: Double = 0.0
    var distance: Int = -1

    init {
        next()
    }

    fun next(): Stronghold {
        distance++
        x = (((originX / 16).roundToInt() + distance * signX) * 16).toDouble()
        z = (x - originX) * ratio + originZ
        return Stronghold((1 / (abs(alignToGrid(z) - z))).toLong(), alignToGrid(x), alignToGrid(z))
    }

    val isInRing: Boolean
        get() {
            val distance = sqrt(x.pow(2.0) + z.pow(2.0)).roundToInt().toDouble()
            return if (distance < 21248) (distance + 240) % 3072 > 1504 else distance < 24336
        }

    fun alignToGrid(x: Double): Int {
        return ((x / 16).roundToInt() * 16)
    }
}

class Stronghold(var accuracy: Long, var x: Int, var z: Int)