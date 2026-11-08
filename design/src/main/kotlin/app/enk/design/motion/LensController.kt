package app.enk.design.motion

import kotlin.math.abs

data class ItemBounds(val left: Float, val width: Float)

data class LensFrame(val left: Float, val width: Float, val highlighted: Int, val dragging: Boolean)

data class LensRelease(val targetIndex: Int, val velocity: Float, val spring: ShellSpring)

class LensController(private val items: List<ItemBounds>) {

    private data class TouchSample(val timeMs: Long, val x: Float)

    private var startX: Float = 0f
    private var initialLensLeft: Float = 0f
    private var currentLensWidth: Float = 0f
    private var grabOffset: Float = 0f
    private var isDragging: Boolean = false
    private val samples = ArrayDeque<TouchSample>()

    fun down(x: Float, lensLeft: Float, lensWidth: Float) {
        startX = x
        initialLensLeft = lensLeft
        currentLensWidth = lensWidth
        isDragging = false
        samples.clear()
        grabOffset = if (x < lensLeft || x > lensLeft + lensWidth) {
            lensWidth / 2f
        } else {
            x - lensLeft
        }
    }

    fun move(x: Float, timeMs: Long): LensFrame {
        addSample(timeMs, x)
        if (!isDragging && abs(x - startX) >= ShellMotion.DRAG_SLOP_DP) {
            isDragging = true
        }

        if (!isDragging) {
            val center = initialLensLeft + currentLensWidth / 2f
            return LensFrame(
                left = initialLensLeft,
                width = currentLensWidth,
                highlighted = nearestIndex(center),
                dragging = false
            )
        }

        val effectiveLeft = computeEffectiveLeft(x)
        val center = effectiveLeft + currentLensWidth / 2f
        return LensFrame(
            left = effectiveLeft,
            width = currentLensWidth,
            highlighted = nearestIndex(center),
            dragging = true
        )
    }

    fun up(x: Float, timeMs: Long): LensRelease {
        addSample(timeMs, x)
        val velocity = computeVelocity()

        val currentLeft = if (isDragging) {
            computeEffectiveLeft(x)
        } else {
            initialLensLeft
        }
        val currentCenter = currentLeft + currentLensWidth / 2f
        val projectedCenter = currentCenter + Physics.project(velocity)
        val targetIndex = nearestIndex(projectedCenter)
        val spring = if (abs(velocity) > ShellMotion.FLICK_THRESHOLD) {
            ShellMotion.flick
        } else {
            ShellMotion.default
        }
        return LensRelease(targetIndex = targetIndex, velocity = velocity, spring = spring)
    }

    fun tapIndex(x: Float): Int? {
        for (i in items.indices) {
            val item = items[i]
            val isLast = (i == items.lastIndex)
            val inBounds = if (isLast) {
                x >= item.left && x <= item.left + item.width
            } else {
                x >= item.left && x < item.left + item.width
            }
            if (inBounds) return i
        }
        return null
    }

    private fun addSample(timeMs: Long, x: Float) {
        if (samples.size >= 6) {
            samples.removeFirst()
        }
        samples.addLast(TouchSample(timeMs, x))
    }

    private fun computeVelocity(): Float {
        if (samples.size < 2) return 0f
        val first = samples.first()
        val last = samples.last()
        val dt = (last.timeMs - first.timeMs) / 1000f
        return if (dt > 0f) {
            (last.x - first.x) / dt
        } else {
            0f
        }
    }

    private fun computeEffectiveLeft(x: Float): Float {
        val unconstrainedLeft = x - grabOffset
        if (items.isEmpty()) return unconstrainedLeft

        val minLeft = items.first().left
        val maxRight = items.last().left + items.last().width
        val maxLeft = maxRight - currentLensWidth

        return when {
            unconstrainedLeft < minLeft -> {
                val overshoot = unconstrainedLeft - minLeft
                minLeft + Physics.rubber(overshoot, items.first().width)
            }
            unconstrainedLeft > maxLeft -> {
                val overshoot = unconstrainedLeft - maxLeft
                maxLeft + Physics.rubber(overshoot, items.last().width)
            }
            else -> unconstrainedLeft
        }
    }

    private fun nearestIndex(center: Float): Int {
        if (items.isEmpty()) return 0
        var bestIndex = 0
        var minDiff = Float.MAX_VALUE
        for (i in items.indices) {
            val itemCenter = items[i].left + items[i].width / 2f
            val diff = abs(center - itemCenter)
            if (diff < minDiff) {
                minDiff = diff
                bestIndex = i
            }
        }
        return bestIndex
    }
}
