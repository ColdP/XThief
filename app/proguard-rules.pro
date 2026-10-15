# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 btm_m

# Keep the X video extraction models reachable from reflection-free entry points.
-keep class btm.m.xthief.** { *; }
-dontwarn org.jetbrains.annotations.**
