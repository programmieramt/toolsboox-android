package com.toolsboox.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Provide main shared preferences.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@Module
@InstallIn(SingletonComponent::class)
object MainSharedPreferencesModule {

    /**
     * Provides main shared preferences.
     */
    @Provides
    @Reusable
    internal fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("MAIN", Context.MODE_PRIVATE)
    }
}
