/**
 * **************************************************************************
 * PlaylistFragment.kt
 * ****************************************************************************
 * Copyright © 2018 VLC authors and VideoLAN
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
 * ***************************************************************************
 */
package org.videolan.vlc.gui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.launch
import org.videolan.medialibrary.interfaces.media.AbstractMediaWrapper
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.vlc.BuildConfig
import org.videolan.vlc.R
import org.videolan.vlc.databinding.PlaylistsFragmentBinding
import org.videolan.vlc.gui.audio.AudioBrowserAdapter
import org.videolan.vlc.gui.audio.AudioBrowserFragment
import org.videolan.vlc.gui.audio.BaseAudioBrowser
import org.videolan.vlc.gui.view.FastScroller
import org.videolan.vlc.gui.view.RecyclerSectionItemGridDecoration
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.providers.medialibrary.MedialibraryProvider
import org.videolan.vlc.reloadLibrary
import org.videolan.vlc.util.CTX_PLAY_ALL
import org.videolan.vlc.util.getScreenWidth
import org.videolan.vlc.viewmodels.mobile.PlaylistsViewModel
import org.videolan.vlc.viewmodels.mobile.getViewModel

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
class PlaylistFragment : BaseAudioBrowser<PlaylistsViewModel>(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var binding: PlaylistsFragmentBinding
    private lateinit var playlists: RecyclerView
    private lateinit var playlistAdapter: AudioBrowserAdapter
    private lateinit var fastScroller: FastScroller

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = getViewModel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = PlaylistsFragmentBinding.inflate(inflater, container, false)
        playlists = binding.swipeLayout.findViewById(R.id.audio_list)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.swipeLayout.setOnRefreshListener(this)
        val nbColumns = 2
        val gridLayoutManager = GridLayoutManager(view.context, nbColumns)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {

                if (position == playlistAdapter.itemCount - 1) {
                    return 1
                }
                if (viewModel.provider.isFirstInSection(position + 1)) {

                    //calculate how many cell it must take
                    val firstSection = viewModel.provider.getPositionForSection(position)
                    val nbItems = position - firstSection
                    if (BuildConfig.DEBUG)
                        Log.d("SongsBrowserFragment", "Position: " + position + " nb items: " + nbItems + " span: " + nbItems % nbColumns)

                    return nbColumns - nbItems % nbColumns
                }

                return 1
            }
        }

        //size of an item
        val spacing = resources.getDimension(R.dimen.kl_half).toInt()
        val itemSize = RecyclerSectionItemGridDecoration.getItemSize(requireActivity().getScreenWidth() - spacing * 2, nbColumns, spacing)

        playlistAdapter = AudioBrowserAdapter(MediaLibraryItem.TYPE_PLAYLIST, this, itemSize)
        adapter = playlistAdapter

        playlists.layoutManager = gridLayoutManager
        playlists.adapter = playlistAdapter
        playlists.addItemDecoration(RecyclerSectionItemGridDecoration(resources.getDimensionPixelSize(R.dimen.recycler_section_header_height), spacing, true, nbColumns, viewModel.provider))
        fastScroller = view.rootView.findViewById(R.id.songs_fast_scroller) as FastScroller
        fastScroller.attachToCoordinator(view.rootView.findViewById(R.id.appbar) as AppBarLayout, view.rootView.findViewById(R.id.coordinator) as CoordinatorLayout, view.rootView.findViewById(R.id.fab) as FloatingActionButton)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.provider.pagedList.observe(requireActivity(), Observer {
            playlistAdapter.submitList(it as PagedList<MediaLibraryItem>)
            binding.empty.visibility = if (it.isNullOrEmpty()) View.VISIBLE else View.GONE
        })
        viewModel.provider.loading.observe(this, Observer<Boolean> { loading ->
            launch {
                binding.swipeLayout.isRefreshing = loading == true
                (activity as? MainActivity)?.refreshing = loading
            }
        })

        fastScroller.setRecyclerView(getCurrentRV(), viewModel.provider)

    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.action_mode_audio_browser, menu)
        menu.findItem(R.id.action_mode_audio_add_playlist).isVisible = false
        return true
    }

    override fun onClick(v: View, position: Int, item: MediaLibraryItem) {
        if (actionMode == null) {
            val i = Intent(activity, PlaylistActivity::class.java)
            i.putExtra(AudioBrowserFragment.TAG_ITEM, item)
            startActivity(i)
        } else super.onClick(v, position, item)
    }

    override fun onCtxAction(position: Int, option: Int) {
        if (option == CTX_PLAY_ALL) MediaUtils.playAll(requireContext(), viewModel.provider as MedialibraryProvider<AbstractMediaWrapper>, position, false)
        else super.onCtxAction(position, option)
    }

    override fun onRefresh() {
        activity?.reloadLibrary()
    }

    override fun getTitle(): String = getString(R.string.playlists)

    override fun getCurrentRV(): RecyclerView = playlists

    override fun hasFAB() = false
}
