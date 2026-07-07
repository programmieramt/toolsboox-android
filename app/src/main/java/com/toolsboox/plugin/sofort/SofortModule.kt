package com.toolsboox.plugin.sofort

import com.toolsboox.plugin.sofort.ui.SofortListPresenter
import com.toolsboox.plugin.sofort.ui.SofortMetaPresenter
import com.toolsboox.plugin.sofort.ui.SofortPresenter
import com.toolsboox.ui.plugin.FragmentPresenter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@Module
@InstallIn(ActivityComponent::class)
abstract class SofortModule {

    @Binds
    abstract fun bindSofortListPresenter(presenter: SofortListPresenter): FragmentPresenter

    @Binds
    abstract fun bindSofortMetaPresenter(presenter: SofortMetaPresenter): FragmentPresenter

    @Binds
    abstract fun bindSofortPresenter(presenter: SofortPresenter): FragmentPresenter
}
