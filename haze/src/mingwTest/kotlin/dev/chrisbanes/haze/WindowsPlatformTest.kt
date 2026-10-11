// Copyright 2026, Christopher Banes and the Haze project contributors
// SPDX-License-Identifier: Apache-2.0

package dev.chrisbanes.haze

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isLessThan
import kotlin.math.abs
import kotlin.test.Test
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.time

class WindowsPlatformTest {
  @OptIn(ExperimentalForeignApi::class)
  @Test
  fun epochTimeMillis_matchesTheUnixClock() {
    assertThat(abs(epochTimeMillis() - time(null) * 1000)).isLessThan(2000L)
  }

  @Test
  fun trimMemoryCallback_stopsWithItsWindowLifecycleAndDisposes() {
    val lifecycle = TestLifecycle()
    val events = mutableListOf<TrimMemoryLevel>()
    val registration = registerTrimMemoryCallback(PlatformContext.INSTANCE, lifecycle, events::add)

    lifecycle.dispatch(Lifecycle.Event.ON_STOP)
    assertThat(events).containsExactly(TrimMemoryLevel.MODERATE)
    registration.dispose()
    assertThat(lifecycle.observers).isEmpty()
    lifecycle.dispatch(Lifecycle.Event.ON_STOP)
    assertThat(events).containsExactly(TrimMemoryLevel.MODERATE)
  }

  private class TestLifecycle : Lifecycle() {
    val observers = mutableListOf<LifecycleObserver>()
    override val currentState: State = State.STARTED
    override fun addObserver(observer: LifecycleObserver) {
      observers += observer
    }

    override fun removeObserver(observer: LifecycleObserver) {
      observers -= observer
    }

    fun dispatch(event: Event) {
      val owner = object : LifecycleOwner {
        override val lifecycle: Lifecycle = this@TestLifecycle
      }
      observers.toList().forEach { (it as LifecycleEventObserver).onStateChanged(owner, event) }
    }
  }
}
