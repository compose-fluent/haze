// Copyright 2026, Christopher Banes and the Haze project contributors
// SPDX-License-Identifier: Apache-2.0

package dev.chrisbanes.haze

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import assertk.assertThat
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlin.test.Test
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface

class SkiaBlurMingwTest {
  @Test
  fun gaussianBlur_spreadsPixelsAcrossAnEdge() {
    val effect = createBlurRenderEffect(12f, 12f, TileMode.Clamp)
    assertThat(effect).isNotNull()
    val pixels = render(effect)
    assertThat(pixels[28, 16].red).isGreaterThan(0.1f)
    assertThat(pixels[35, 16].red).isLessThan(0.9f)
  }

  @Test
  fun zeroRadius_doesNotCreateABlurFilter() {
    assertThat(createBlurRenderEffect(0f, 0f, TileMode.Clamp)).isNull()
  }

  @Test
  fun progressiveBlur_usesTheMaskToVaryBlurIntensity() {
    val effect = createProgressiveBlurRenderEffect(
      blurRadiusPx = 16f,
      size = Size(64f, 32f),
      offset = Offset.Zero,
      mask = LinearGradientShader(
        from = Offset.Zero,
        to = Offset(0f, 32f),
        colors = listOf(Color.Transparent, Color.Black),
      ),
    )
    val pixels = render(effect)
    assertThat(pixels[35, 2].red).isGreaterThan(0.9f)
    assertThat(pixels[35, 29].red).isLessThan(0.9f)
  }

  @Test
  fun fractalNoiseShader_rendersVaryingPixels() {
    val pixels = render(
      createShaderRenderEffect(createFractalNoiseShader(0.45f, 0.45f, 4, 2f)),
    )
    val samples = (4..28 step 4).map { x -> pixels[x, 16].value }.toSet()
    assertThat(samples.size).isGreaterThan(1)
  }

  @Test
  fun mutableRuntimeShader_updatesRenderedUniforms() {
    val effect = createRuntimeEffect(
      """
      uniform shader content;
      uniform float opacity;
      half4 main(float2 position) {
        return content.eval(position) * opacity;
      }
      """.trimIndent(),
    )
    val mutable = createMutableRuntimeShaderRenderEffect(effect, arrayOf("content"), arrayOf(null))
    val low = render(mutable.updateUniforms { setFloatUniform("opacity", 0.25f) })
    val high = render(mutable.updateUniforms { setFloatUniform("opacity", 0.75f) })
    assertThat(low[48, 16].alpha).isLessThan(0.3f)
    assertThat(high[48, 16].alpha).isGreaterThan(0.7f)
  }

  private fun render(effect: PlatformRenderEffect?): PixelMap {
    val source = Surface.makeRasterN32Premul(64, 32)
    val output = Surface.makeRasterN32Premul(64, 32)
    val sourcePaint = Paint().apply { color = 0xFF000000.toInt() }
    val filterPaint = Paint().apply { imageFilter = effect }
    try {
      source.canvas.clear(0xFFFFFFFF.toInt())
      source.canvas.drawRect(Rect.makeLTRB(0f, 0f, 32f, 32f), sourcePaint)
      val image = source.makeImageSnapshot()
      try {
        output.canvas.clear(0x00000000)
        output.canvas.drawImage(image, 0f, 0f, filterPaint)
        val rendered = output.makeImageSnapshot()
        try {
          return rendered.toComposeImageBitmap().toPixelMap()
        } finally {
          rendered.close()
        }
      } finally {
        image.close()
      }
    } finally {
      sourcePaint.close()
      filterPaint.close()
      source.close()
      output.close()
    }
  }
}
