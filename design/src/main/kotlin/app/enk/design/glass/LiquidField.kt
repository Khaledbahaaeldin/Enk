package app.enk.design.glass

import kotlin.math.abs
import kotlin.math.hypot

data class LiquidShape(
    val cx: Float,
    val cy: Float,
    val halfW: Float,
    val halfH: Float,
    val radius: Float
)

const val LIQUID_FIELD_MAX_SHAPES = 6

object LiquidFieldMath {
    fun sdRoundRect(x: Float, y: Float, s: LiquidShape): Float {
        val dx = abs(x - s.cx) - (s.halfW - s.radius)
        val dy = abs(y - s.cy) - (s.halfH - s.radius)
        val outside = hypot(maxOf(dx, 0f), maxOf(dy, 0f)) - s.radius
        val inside = minOf(maxOf(dx, dy), 0f)
        return outside + inside
    }

    fun smoothMin(a: Float, b: Float, k: Float): Float {
        if (k <= 0f) return minOf(a, b)
        val h = (0.5f + 0.5f * (b - a) / k).coerceIn(0f, 1f)
        return (b * (1f - h) + a * h) - k * h * (1f - h)
    }

    fun field(x: Float, y: Float, shapes: List<LiquidShape>, k: Float): Float {
        if (shapes.isEmpty()) return Float.MAX_VALUE
        var d = sdRoundRect(x, y, shapes[0])
        for (i in 1 until shapes.size) {
            d = smoothMin(d, sdRoundRect(x, y, shapes[i]), k)
        }
        return d
    }

    fun gap(a: LiquidShape, b: LiquidShape): Float {
        val minAx = a.cx - (a.halfW - a.radius)
        val maxAx = a.cx + (a.halfW - a.radius)
        val minAy = a.cy - (a.halfH - a.radius)
        val maxAy = a.cy + (a.halfH - a.radius)

        val minBx = b.cx - (b.halfW - b.radius)
        val maxBx = b.cx + (b.halfW - b.radius)
        val minBy = b.cy - (b.halfH - b.radius)
        val maxBy = b.cy + (b.halfH - b.radius)

        val dx = maxOf(0f, maxOf(minAx - maxBx, minBx - maxAx))
        val dy = maxOf(0f, maxOf(minAy - maxBy, minBy - maxAy))
        val boxDist = hypot(dx, dy)
        val dist = boxDist - (a.radius + b.radius)
        return maxOf(0f, dist)
    }

    fun shouldMerge(shapes: List<LiquidShape>, mergeDistancePx: Float): Boolean {
        for (i in 0 until shapes.size) {
            for (j in i + 1 until shapes.size) {
                if (gap(shapes[i], shapes[j]) < mergeDistancePx) {
                    return true
                }
            }
        }
        return false
    }
}

val LiquidFieldShader: String = """
    uniform shader content;

    uniform float4 shapes[6];
    uniform float radii[6];
    uniform int count;
    uniform float k;
    uniform float refractionHeight;
    uniform float refractionAmount;
    uniform float depthEffect;
    uniform float chromaticAberration;
    uniform float4 tint;
    uniform float edgeLight;
    uniform float lightAngle;

    float sdRoundRect(float2 p, float4 s, float r) {
        float2 corner = abs(p - s.xy) - (s.zw - float2(r));
        float outside = length(max(corner, 0.0)) - r;
        float inside = min(max(corner.x, corner.y), 0.0);
        return outside + inside;
    }

    float smin(float a, float b, float k) {
        if (k <= 0.0) return min(a, b);
        float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
        return mix(b, a, h) - k * h * (1.0 - h);
    }

    float liquidField(float2 p) {
        if (count <= 0) return 1000.0;
        float d = sdRoundRect(p, shapes[0], radii[0]);
        for (int i = 1; i < 6; ++i) {
            if (i >= count) break;
            d = smin(d, sdRoundRect(p, shapes[i], radii[i]), k);
        }
        return d;
    }

    float2 nearestCentre(float2 p) {
        float best = sdRoundRect(p, shapes[0], radii[0]);
        float2 c = shapes[0].xy;
        for (int i = 1; i < 6; ++i) {
            if (i >= count) break;
            float d = sdRoundRect(p, shapes[i], radii[i]);
            if (d < best) {
                best = d;
                c = shapes[i].xy;
            }
        }
        return c;
    }

    float2 gradField(float2 p) {
        float eps = 1.0;
        float dx = liquidField(float2(p.x + eps, p.y)) - liquidField(float2(p.x - eps, p.y));
        float dy = liquidField(float2(p.x, p.y + eps)) - liquidField(float2(p.x, p.y - eps));
        float len = length(float2(dx, dy));
        return len > 0.0 ? float2(dx, dy) / len : float2(0.0, 1.0);
    }

    float circleMap(float x) {
        return 1.0 - sqrt(max(0.0, 1.0 - x * x));
    }

    half4 main(float2 coord) {
        float sd = liquidField(coord);
        if (sd > 0.5) {
            return half4(0.0);
        }

        float2 grad = gradField(coord);
        float2 refractedCoord = coord;
        float2 dispersedCoord = float2(0.0);

        if (-sd < refractionHeight && refractionHeight > 0.0) {
            float d = circleMap(1.0 - -sd / refractionHeight) * refractionAmount;
            float2 n = grad;
            if (depthEffect > 0.0) {
                float2 c = nearestCentre(coord);
                float2 toCoord = coord - c;
                if (length(toCoord) > 0.001) {
                    n = normalize(grad + depthEffect * normalize(toCoord));
                }
            }
            refractedCoord = coord + d * n;

            float dispersionIntensity = chromaticAberration * 0.1;
            dispersedCoord = d * n * dispersionIntensity;
        }

        half4 color = half4(0.0);
        if (chromaticAberration > 0.0) {
            half4 red = content.eval(refractedCoord + dispersedCoord);
            color.r += red.r / 3.5;
            color.a += red.a / 7.0;

            half4 orange = content.eval(refractedCoord + dispersedCoord * (2.0 / 3.0));
            color.r += orange.r / 3.5;
            color.g += orange.g / 7.0;
            color.a += orange.a / 7.0;

            half4 yellow = content.eval(refractedCoord + dispersedCoord * (1.0 / 3.0));
            color.r += yellow.r / 3.5;
            color.g += yellow.g / 3.5;
            color.a += yellow.a / 7.0;

            half4 green = content.eval(refractedCoord);
            color.g += green.g / 3.5;
            color.a += green.a / 7.0;

            half4 cyan = content.eval(refractedCoord - dispersedCoord * (1.0 / 3.0));
            color.g += cyan.g / 3.5;
            color.b += cyan.b / 3.0;
            color.a += cyan.a / 7.0;

            half4 blue = content.eval(refractedCoord - dispersedCoord * (2.0 / 3.0));
            color.b += blue.b / 3.0;
            color.a += blue.a / 7.0;

            half4 purple = content.eval(refractedCoord - dispersedCoord);
            color.r += purple.r / 7.0;
            color.b += purple.b / 3.0;
            color.a += purple.a / 7.0;
        } else {
            color = content.eval(refractedCoord);
        }

        if (tint.a > 0.0) {
            color.rgb = mix(color.rgb, tint.rgb, tint.a);
        }

        if (edgeLight > 0.0 && sd <= 0.0 && -sd < 3.0) {
            float rimAlpha = (1.0 - (-sd / 3.0)) * edgeLight;
            float rad = lightAngle * 0.0174532925;
            float2 lightDir = float2(cos(rad), sin(rad));
            float lightIntensity = clamp(dot(grad, lightDir), 0.0, 1.0);
            float highlight = rimAlpha * (0.4 + 0.6 * lightIntensity);
            color.rgb += float3(highlight);
            color.a = max(color.a, rimAlpha);
        }

        float edgeAlpha = clamp(0.5 - sd, 0.0, 1.0);
        color *= edgeAlpha;

        return color;
    }
""".trimIndent()
