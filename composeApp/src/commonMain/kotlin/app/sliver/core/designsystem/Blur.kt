package app.sliver.core.designsystem

/** True where RenderEffect blur exists (Android 12+). Below it chips use a plain 70% black plate. */
expect fun blurSupported(): Boolean
