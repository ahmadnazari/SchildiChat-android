/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package im.vector.app.features.roomprofile

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.android.material.appbar.MaterialToolbar
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.viewModel
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.extensions.addFragmentToBackstack
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.ToolbarConfigurable
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding
import im.vector.app.features.home.room.detail.RoomDetailPendingActionStore
import im.vector.app.features.room.RequireActiveMembershipViewEvents
import im.vector.app.features.room.RequireActiveMembershipViewModel
import im.vector.app.features.room.RequireActiveMembershipViewState
import im.vector.app.features.roomprofile.banned.RoomBannedMemberListFragment
import im.vector.app.features.roomprofile.members.RoomMemberListFragment
import im.vector.app.features.roomprofile.settings.RoomSettingsFragment
import im.vector.app.features.roomprofile.alias.RoomAliasFragment
import im.vector.app.features.roomprofile.notifications.RoomNotificationSettingsFragment
import im.vector.app.features.roomprofile.permissions.RoomPermissionsFragment
import im.vector.app.features.roomprofile.uploads.RoomUploadsFragment
import javax.inject.Inject

open class RoomProfileActivity :
        VectorBaseActivity<ActivitySimpleBinding>(),
        ToolbarConfigurable,
        RequireActiveMembershipViewModel.Factory {

    companion object {

        private const val EXTRA_DIRECT_ACCESS = "EXTRA_DIRECT_ACCESS"

        const val EXTRA_DIRECT_ACCESS_ROOM_ROOT = 0
        const val EXTRA_DIRECT_ACCESS_ROOM_SETTINGS = 1
        const val EXTRA_DIRECT_ACCESS_ROOM_MEMBERS = 2

        fun newIntent(context: Context, roomId: String, directAccess: Int?): Intent {
            val roomProfileArgs = RoomProfileArgs(roomId)
            return Intent(context, RoomProfileActivity::class.java).apply {
                putExtra(MvRx.KEY_ARG, roomProfileArgs)
                putExtra(EXTRA_DIRECT_ACCESS, directAccess)
            }
        }
    }

    private lateinit var sharedActionViewModel: RoomProfileSharedActionViewModel
    protected lateinit var roomProfileArgs: RoomProfileArgs

    private val requireActiveMembershipViewModel: RequireActiveMembershipViewModel by viewModel()

    @Inject
    lateinit var requireActiveMembershipViewModelFactory: RequireActiveMembershipViewModel.Factory

    @Inject
    lateinit var roomDetailPendingActionStore: RoomDetailPendingActionStore

    override fun create(initialState: RequireActiveMembershipViewState): RequireActiveMembershipViewModel {
        return requireActiveMembershipViewModelFactory.create(initialState)
    }

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun getBinding(): ActivitySimpleBinding {
        return ActivitySimpleBinding.inflate(layoutInflater)
    }

    override fun initUiAndData() {
        sharedActionViewModel = viewModelProvider.get(RoomProfileSharedActionViewModel::class.java)
        roomProfileArgs = intent?.extras?.getParcelable(MvRx.KEY_ARG) ?: return
        if (isFirstCreation()) {
            addInitialFragment()
        }
        sharedActionViewModel
                .observe()
                .subscribe { sharedAction ->
                    when (sharedAction) {
                        RoomProfileSharedAction.OpenRoomMembers                 -> openRoomMembers()
                        RoomProfileSharedAction.OpenRoomSettings                -> openRoomSettings()
                        RoomProfileSharedAction.OpenRoomAliasesSettings         -> openRoomAlias()
                        RoomProfileSharedAction.OpenRoomPermissionsSettings     -> openRoomPermissions()
                        RoomProfileSharedAction.OpenRoomUploads                 -> openRoomUploads()
                        RoomProfileSharedAction.OpenBannedRoomMembers        -> openBannedRoomMembers()
                        RoomProfileSharedAction.OpenRoomNotificationSettings -> openRoomNotificationSettings()
                    }.exhaustive
                }
                .disposeOnDestroy()

        requireActiveMembershipViewModel.observeViewEvents {
            when (it) {
                is RequireActiveMembershipViewEvents.RoomLeft -> handleRoomLeft(it)
            }
        }
    }

    open fun addInitialFragment() {
        when (intent?.extras?.getInt(EXTRA_DIRECT_ACCESS, EXTRA_DIRECT_ACCESS_ROOM_ROOT)) {
            EXTRA_DIRECT_ACCESS_ROOM_SETTINGS -> {
                addFragment(R.id.simpleFragmentContainer, RoomProfileFragment::class.java, roomProfileArgs)
                addFragmentToBackstack(R.id.simpleFragmentContainer, RoomSettingsFragment::class.java, roomProfileArgs)
            }
            EXTRA_DIRECT_ACCESS_ROOM_MEMBERS -> {
                addFragment(R.id.simpleFragmentContainer, RoomMemberListFragment::class.java, roomProfileArgs)
            }
            else -> addFragment(R.id.simpleFragmentContainer, RoomProfileFragment::class.java, roomProfileArgs)
        }
    }

    override fun onResume() {
        super.onResume()
        if (roomDetailPendingActionStore.data != null) {
            finish()
        }
    }

    private fun handleRoomLeft(roomLeft: RequireActiveMembershipViewEvents.RoomLeft) {
        if (roomLeft.leftMessage != null) {
            Toast.makeText(this, roomLeft.leftMessage, Toast.LENGTH_LONG).show()
        }
        finish()
    }

    private fun openRoomUploads() {
        addFragmentToBackstack(R.id.simpleFragmentContainer, RoomUploadsFragment::class.java, roomProfileArgs)
    }

    private fun openRoomSettings() {
        addFragmentToBackstack(R.id.simpleFragmentContainer, RoomSettingsFragment::class.java, roomProfileArgs)
    }

    private fun openRoomAlias() {
        addFragmentToBackstack(R.id.simpleFragmentContainer, RoomAliasFragment::class.java, roomProfileArgs)
    }

    private fun openRoomPermissions() {
        addFragmentToBackstack(R.id.simpleFragmentContainer, RoomPermissionsFragment::class.java, roomProfileArgs)
    }

    private fun openRoomMembers() {
        addFragmentToBackstack(R.id.simpleFragmentContainer, RoomMemberListFragment::class.java, roomProfileArgs)
    }

    private fun openBannedRoomMembers() {
        addFragmentToBackstack(R.id.simpleFragmentContainer, RoomBannedMemberListFragment::class.java, roomProfileArgs)
    }

    private fun openRoomNotificationSettings() {
        addFragmentToBackstack(R.id.simpleFragmentContainer, RoomNotificationSettingsFragment::class.java, roomProfileArgs)
    }

    override fun configure(toolbar: MaterialToolbar) {
        configureToolbar(toolbar)
    }
}
