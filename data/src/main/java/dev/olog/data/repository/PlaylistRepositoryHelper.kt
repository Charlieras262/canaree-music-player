package dev.olog.data.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.BaseColumns
import android.provider.MediaStore.Audio.Playlists
import dev.olog.contentresolversql.querySql
import dev.olog.core.dagger.ApplicationContext
import dev.olog.core.entity.AutoPlaylist
import dev.olog.core.entity.favorite.FavoriteType
import dev.olog.core.entity.id
import dev.olog.core.gateway.FavoriteGateway
import dev.olog.core.gateway.track.PlaylistOperations
import dev.olog.data.db.dao.AppDatabase
import dev.olog.data.utils.assertBackgroundThread
import dev.olog.data.utils.getLong
import dev.olog.data.utils.handleRecoverableSecurityException
import javax.inject.Inject

internal class PlaylistRepositoryHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    appDatabase: AppDatabase,
    private val favoriteGateway: FavoriteGateway

) : PlaylistOperations {

    private val historyDao = appDatabase.historyDao()

    override suspend fun createPlaylist(playlistName: String): Long {
        assertBackgroundThread()

        val added = System.currentTimeMillis()

        val contentValues = ContentValues()
        contentValues.put(Playlists.NAME, playlistName)
        contentValues.put(Playlists.DATE_ADDED, added)
        contentValues.put(Playlists.DATE_MODIFIED, added)
        val uri = context.contentResolver.insert(Playlists.EXTERNAL_CONTENT_URI, contentValues)
        requireNotNull(uri)
        return ContentUris.parseId(uri)
    }

    override suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>){
        assertBackgroundThread()

        val uri = Playlists.Members.getContentUri("external", playlistId)

        val cursor = context.contentResolver.querySql("""
            SELECT ${Playlists.Members.PLAY_ORDER}
            FROM $uri
            ORDER BY ${Playlists.Members.PLAY_ORDER} DESC
            LIMIT 1
        """.trimIndent())

        var lastPlayOrder = cursor.use {
            if (it.moveToFirst()){
                it.getInt(0) + 1
            } else {
                1
            }
        }

        val arrayOf = mutableListOf<ContentValues>()
        for (songId in songIds) {
            val values = ContentValues(2)
            values.put(Playlists.Members.PLAY_ORDER, lastPlayOrder++)
            values.put(Playlists.Members.AUDIO_ID, songId)
            arrayOf.add(values)
        }

        handleRecoverableSecurityException {
            context.contentResolver.bulkInsert(uri, arrayOf.toTypedArray())
            context.contentResolver.notifyChange(Playlists.EXTERNAL_CONTENT_URI, null)
        }
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        context.contentResolver.delete(Playlists.EXTERNAL_CONTENT_URI, "${BaseColumns._ID} = ?", arrayOf("$playlistId"))
    }

    override suspend fun clearPlaylist(playlistId: Long) {
        if (AutoPlaylist.isAutoPlaylist(playlistId)) {
            when (playlistId) {
                AutoPlaylist.FAVORITE.id -> return favoriteGateway.deleteAll(FavoriteType.TRACK)
                AutoPlaylist.HISTORY.id -> return historyDao.deleteAll()
            }
        }
        val uri = Playlists.Members.getContentUri("external", playlistId)
        context.contentResolver.delete(uri, null, null)
    }

    override suspend fun removeFromPlaylist(playlistId: Long, idInPlaylist: Long) {
        assertBackgroundThread()

        if (AutoPlaylist.isAutoPlaylist(playlistId)) {
            removeFromAutoPlaylist(playlistId, idInPlaylist)
        } else {
            val uri = Playlists.Members.getContentUri("external", playlistId)
            context.contentResolver.delete(uri, "${Playlists.Members._ID} = ?", arrayOf("$idInPlaylist"))
        }
    }

    private suspend fun removeFromAutoPlaylist(playlistId: Long, songId: Long) {
        return when (playlistId) {
            AutoPlaylist.FAVORITE.id -> favoriteGateway.deleteSingle(FavoriteType.TRACK, songId)
            AutoPlaylist.HISTORY.id -> historyDao.deleteSingle(songId)
            else -> throw IllegalArgumentException("invalid auto playlist id: $playlistId")
        }
    }

    override suspend fun renamePlaylist(playlistId: Long, newTitle: String) {
        val values = ContentValues(1)
        values.put(Playlists.NAME, newTitle)

        val rowsUpdated = context.contentResolver.update(Playlists.EXTERNAL_CONTENT_URI,
            values, "${BaseColumns._ID} = ?", arrayOf("$playlistId"))
    }

    override fun moveItem(playlistId: Long, from: Int, to: Int): Boolean {
        return Playlists.Members.moveItem(context.contentResolver, playlistId, from, to)
    }

    override suspend fun removeDuplicated(playlistId: Long){
        val uri = Playlists.Members.getContentUri("external", playlistId)
        val cursor = context.contentResolver.query(uri, arrayOf(
            Playlists.Members._ID,
            Playlists.Members.AUDIO_ID
        ), null, null, Playlists.Members.DEFAULT_SORT_ORDER)

        val distinctTrackIds = mutableSetOf<Long>()

        while (cursor != null && cursor.moveToNext()) {
            val trackId = cursor.getLong(Playlists.Members.AUDIO_ID)
            distinctTrackIds.add(trackId)
        }
        cursor?.close()

        context.contentResolver.delete(uri, null, null)
        addSongsToPlaylist(playlistId, distinctTrackIds.toList())
    }

    override suspend fun insertSongToHistory(songId: Long) {
        return historyDao.insert(songId)
    }

}