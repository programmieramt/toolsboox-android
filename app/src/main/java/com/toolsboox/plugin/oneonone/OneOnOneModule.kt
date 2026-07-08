package com.toolsboox.plugin.oneonone

import com.toolsboox.plugin.oneonone.ui.OneOnOneFragment
import com.toolsboox.plugin.oneonone.ui.OneOnOneListPresenter
import com.toolsboox.plugin.oneonone.ui.OneOnOneMetaPresenter
import com.toolsboox.plugin.oneonone.ui.OneOnOnePresenter
import com.toolsboox.ui.plugin.FragmentPresenter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@Module
@InstallIn(ActivityComponent::class)
abstract class OneOnOneModule {

    @Binds
    abstract fun bindOneOnOneListPresenter(presenter: OneOnOneListPresenter): FragmentPresenter

    @Binds
    abstract fun bindOneOnOneMetaPresenter(presenter: OneOnOneMetaPresenter): FragmentPresenter

    @Binds
    abstract fun bindOneOnOnePresenter(presenter: OneOnOnePresenter): FragmentPresenter
}
