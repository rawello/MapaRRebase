package com.rawello.mapar.di

import android.content.Context
import com.rawello.mapar.data.database.AppDatabase
import com.rawello.mapar.data.database.NodeDao
import com.rawello.mapar.data.database.NodeViewModel
import com.rawello.mapar.data.database.TokenDao
import com.rawello.mapar.data.database.TokenViewModel
import com.rawello.mapar.presentation.scenePage.ArPageViewModel
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule(private val context: Context) {

    @Provides
    @Singleton
    fun provideContext(): Context = context

    @Provides
    @Singleton
    fun provideAppDatabase(context: Context): AppDatabase {
        return AppDatabase.Companion.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideNodeDao(appDatabase: AppDatabase): NodeDao {
        return appDatabase.nodeDao()
    }

    @Provides
    @Singleton
    fun provideTokenDao(appDatabase: AppDatabase): TokenDao {
        return appDatabase.tokenDao()
    }

    @Provides
    @Singleton
    fun provideNodeViewModel(nodeDao: NodeDao): NodeViewModel {
        return NodeViewModel(nodeDao)
    }

    @Provides
    @Singleton
    fun provideTokenViewModel(tokenDao: TokenDao): TokenViewModel {
        return TokenViewModel(tokenDao)
    }

    @Provides
    @Singleton
    fun provideArPageViewModel(nodeViewModel: NodeViewModel): ArPageViewModel {
        return ArPageViewModel(nodeViewModel)
    }
}