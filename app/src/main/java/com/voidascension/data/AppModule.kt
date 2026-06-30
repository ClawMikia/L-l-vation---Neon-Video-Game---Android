package com.voidascension.data

import android.content.Context
import androidx.room.Room
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
    fun provideDatabase(@ApplicationContext ctx: Context): VoidDatabase =
        Room.databaseBuilder(ctx, VoidDatabase::class.java, "void_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideHighScoreDao(db: VoidDatabase) = db.highScoreDao()

    @Provides
    fun providePermanentUpgradeDao(db: VoidDatabase) = db.permanentUpgradeDao()

    @Provides
    @Singleton
    fun provideSaveManager(@ApplicationContext ctx: Context, db: VoidDatabase) = SaveManager(ctx, db)

    @Provides
    @Singleton
    fun provideCheatManager() = CheatManager()

    @Provides
    @Singleton
    fun provideAudioManager(@ApplicationContext ctx: Context) = com.voidascension.utils.AudioManager(ctx)
}
