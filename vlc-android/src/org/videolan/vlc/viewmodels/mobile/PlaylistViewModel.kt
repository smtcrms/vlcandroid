/*****************************************************************************
 * PlaylistViewModel.kt
 *****************************************************************************
 * Copyright © 2019 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/

package org.videolan.vlc.viewmodels.mobile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.videolan.medialibrary.interfaces.AbstractMedialibrary
import org.videolan.medialibrary.interfaces.media.AbstractAlbum
import org.videolan.medialibrary.interfaces.media.AbstractPlaylist
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.vlc.gui.PlaylistActivity
import org.videolan.vlc.providers.medialibrary.MedialibraryProvider
import org.videolan.vlc.providers.medialibrary.TracksProvider
import org.videolan.vlc.util.EmptyMLCallbacks
import org.videolan.vlc.viewmodels.MedialibraryViewModel


@ExperimentalCoroutinesApi
class PlaylistViewModel(context: Context, val playlist: MediaLibraryItem) : MedialibraryViewModel(context),
        AbstractMedialibrary.MediaCb by EmptyMLCallbacks,
        AbstractMedialibrary.AlbumsCb by EmptyMLCallbacks,
        AbstractMedialibrary.PlaylistsCb by EmptyMLCallbacks {
    val tracksProvider = TracksProvider(playlist, context, this)
    override val providers : Array<MedialibraryProvider<out MediaLibraryItem>> = arrayOf(tracksProvider)

    init {
        when (playlist) {
            is AbstractPlaylist -> medialibrary.addPlaylistCb(this@PlaylistViewModel)
            is AbstractAlbum -> medialibrary.addAlbumsCb(this@PlaylistViewModel)
            else -> medialibrary.addMediaCb(this@PlaylistViewModel)
        }
    }

    override fun onMediaAdded() { refresh() }

    override fun onMediaDeleted() { refresh() }

    override fun onPlaylistsDeleted() { refresh() }

    override fun onPlaylistsModified() { refresh() }

    override fun onAlbumsDeleted() { refresh() }

    override fun onAlbumsModified() { refresh() }

    override fun onCleared() {
        when (playlist) {
            is AbstractPlaylist -> medialibrary.removePlaylistCb(this)
            is AbstractAlbum -> medialibrary.removeAlbumsCb(this)
            else -> medialibrary.removeMediaCb(this)
        }
        super.onCleared()
    }

    class Factory(val context: Context, val playlist: MediaLibraryItem): ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return PlaylistViewModel(context.applicationContext, playlist) as T
        }
    }
}

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
internal fun PlaylistActivity.getViewModel(playlist: MediaLibraryItem) = ViewModelProviders.of(this, PlaylistViewModel.Factory(this, playlist)).get(PlaylistViewModel::class.java)