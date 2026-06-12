package com.toolsboox.di

import com.squareup.moshi.Moshi
import com.toolsboox.ot.DateJsonAdapter
import com.toolsboox.ot.LocaleJsonAdapter
import com.toolsboox.ot.UUIDJsonAdapter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

/**
 * Retrofit DI module of network services.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@Module
@InstallIn(ActivityComponent::class)
object NetworkModule {

    /**
     * Provides the Moshi JSON instance.
     *
     * @return the moshi instance
     */
    @Provides
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(LocaleJsonAdapter())
            .add(DateJsonAdapter())
            .add(UUIDJsonAdapter())
            .build()
    }
}
