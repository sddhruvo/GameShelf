package com.gamevault.app.di

import android.content.Context
import androidx.room.Room
import com.gamevault.app.data.db.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GameVaultDatabase {
        return Room.databaseBuilder(
            context,
            GameVaultDatabase::class.java,
            "gamevault_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideGameDao(db: GameVaultDatabase): GameDao = db.gameDao()

    @Provides
    fun provideSessionDao(db: GameVaultDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideCollectionDao(db: GameVaultDatabase): CollectionDao = db.collectionDao()

    @Provides
    fun provideUpdateDao(db: GameVaultDatabase): UpdateDao = db.updateDao()
}
