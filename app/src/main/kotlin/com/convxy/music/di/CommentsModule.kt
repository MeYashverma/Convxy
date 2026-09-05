/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convxy.music.di

import android.content.Context
import com.convxy.music.comments.CommentCacheStorage
import com.convxy.music.comments.CommentsCache
import com.convxy.music.comments.FileCommentCacheStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

/**
 * Bindings for the timestamped-comments layer.
 *
 * Only the cache needs a module: [com.convxy.music.comments.soundcloud.SoundCloudApi],
 * [com.convxy.music.comments.soundcloud.SoundCloudCredentials] and
 * [com.convxy.music.comments.CommentsRepository] all have `@Inject` constructors and are provided by
 * Hilt directly, the same way `LyricsHelper` and `EQProfileRepository` are.
 *
 * Singleton scope matters here for a functional reason, not just an efficiency one — the OAuth access
 * token, the in-memory LRU and the request gate all live for the process, so a per-injection instance
 * would re-authenticate on every player open.
 */
@Module
@InstallIn(SingletonComponent::class)
object CommentsModule {

    /** Cache directory name under `Context.cacheDir`. */
    private const val CACHE_DIR = "comments"

    @Provides
    @Singleton
    fun provideCommentCacheStorage(
        @ApplicationContext context: Context,
    ): CommentCacheStorage = FileCommentCacheStorage(File(context.cacheDir, CACHE_DIR))

    @Provides
    @Singleton
    fun provideCommentsCache(
        storage: CommentCacheStorage,
    ): CommentsCache = CommentsCache(storage)
}
