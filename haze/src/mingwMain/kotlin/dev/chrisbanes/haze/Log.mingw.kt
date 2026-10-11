// Copyright 2026, Christopher Banes and the Haze project contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("NOTHING_TO_INLINE")

package dev.chrisbanes.haze

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.wcstr
import platform.windows.OutputDebugStringW

@OptIn(ExperimentalForeignApi::class)
internal actual inline fun platformLog(tag: String, message: String) {
  memScoped {
    OutputDebugStringW("[$tag] $message\n".wcstr.getPointer(this))
  }
}
