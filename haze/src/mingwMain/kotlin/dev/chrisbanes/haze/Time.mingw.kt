// Copyright 2026, Christopher Banes and the Haze project contributors
// SPDX-License-Identifier: Apache-2.0

package dev.chrisbanes.haze

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.windows.FILETIME
import platform.windows.GetSystemTimeAsFileTime

@OptIn(ExperimentalForeignApi::class)
internal actual fun epochTimeMillis(): Long = memScoped {
  val time = alloc<FILETIME>()
  GetSystemTimeAsFileTime(time.ptr)
  val ticks = (time.dwHighDateTime.toULong() shl 32) or time.dwLowDateTime.toULong()
  // Windows FILETIME counts 100 ns intervals since 1601, rather than the Unix epoch.
  ((ticks - 116444736000000000uL) / 10000uL).toLong()
}
