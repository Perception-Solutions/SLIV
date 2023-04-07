package solve.parsers.structures

data class Point(val uid: Long, val x: Double, val y: Double) {
    override fun toString() = "$uid,$x,$y"
}
