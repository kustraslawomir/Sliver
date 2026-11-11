package app.sliver.di

import org.koin.core.module.Module
import org.koin.dsl.module

val commonModule: Module = module { }

expect val platformModule: Module
