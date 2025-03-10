/*
 * Copyright 2021, Lawnchair
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.lawnchair.ui.preferences.destinations

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import app.lawnchair.preferences.getAdapter
import app.lawnchair.preferences.not
import app.lawnchair.preferences.preferenceManager
import app.lawnchair.preferences2.preferenceManager2
import app.lawnchair.ui.preferences.components.NavigationActionPreference
import app.lawnchair.ui.preferences.components.SuggestionsPreference
import app.lawnchair.ui.preferences.components.controls.SliderPreference
import app.lawnchair.ui.preferences.components.controls.SwitchPreference
import app.lawnchair.ui.preferences.components.layout.DividerColumn
import app.lawnchair.ui.preferences.components.layout.ExpandAndShrink
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLayout
import app.lawnchair.ui.preferences.preferenceGraph
import app.lawnchair.ui.preferences.subRoute
import app.lawnchair.util.checkAndRequestFilesPermission
import app.lawnchair.util.contactPermissionGranted
import app.lawnchair.util.filesAndStorageGranted
import app.lawnchair.util.requestContactPermissionGranted
import com.android.launcher3.R

object AppDrawerRoutes {
    const val HIDDEN_APPS = "hiddenApps"
}

fun NavGraphBuilder.appDrawerGraph(route: String) {
    preferenceGraph(route, { AppDrawerPreferences() }) { subRoute ->
        hiddenAppsGraph(route = subRoute(AppDrawerRoutes.HIDDEN_APPS))
    }
}

@Composable
fun AppDrawerPreferences() {
    val prefs = preferenceManager()
    val prefs2 = preferenceManager2()
    val context = LocalContext.current
    val resources = context.resources

    PreferenceLayout(label = stringResource(id = R.string.app_drawer_label)) {
        PreferenceGroup(heading = stringResource(id = R.string.general_label)) {
            SliderPreference(
                label = stringResource(id = R.string.background_opacity),
                adapter = prefs.drawerOpacity.getAdapter(),
                step = 0.1f,
                valueRange = 0F..1F,
                showAsPercentage = true,
            )
            SwitchPreference(
                label = stringResource(id = R.string.pref_all_apps_bulk_icon_loading_title),
                description = stringResource(id = R.string.pref_all_apps_bulk_icon_loading_description),
                adapter = prefs.allAppBulkIconLoading.getAdapter(),
            )
            SuggestionsPreference()
        }

        val showDrawerSearchBar = !prefs2.hideAppDrawerSearchBar.getAdapter()
        PreferenceGroup(heading = stringResource(id = R.string.hidden_apps_label)) {
            val hiddenApps = prefs2.hiddenApps.getAdapter().state.value
            val hideHiddenAppsInSearch = !prefs2.showHiddenAppsInSearch.getAdapter()
            val enableSmartHide = prefs2.enableSmartHide.getAdapter()
            NavigationActionPreference(
                label = stringResource(id = R.string.hidden_apps_label),
                subtitle = resources.getQuantityString(R.plurals.apps_count, hiddenApps.size, hiddenApps.size),
                destination = subRoute(name = AppDrawerRoutes.HIDDEN_APPS),
            )
            ExpandAndShrink(visible = hiddenApps.isNotEmpty() && showDrawerSearchBar.state.value) {
                DividerColumn {
                    SwitchPreference(
                        label = stringResource(id = R.string.hide_hidden_apps_search),
                        adapter = hideHiddenAppsInSearch,
                    )
                    ExpandAndShrink(visible = hideHiddenAppsInSearch.state.value) {
                        SwitchPreference(
                            label = stringResource(id = R.string.show_enable_smart_hide),
                            adapter = enableSmartHide,
                        )
                    }
                }
            }
        }

        val deviceSearchEnabled = false
        PreferenceGroup(heading = stringResource(id = R.string.pref_category_search)) {
            SwitchPreference(
                label = stringResource(id = R.string.show_app_search_bar),
                adapter = showDrawerSearchBar,
            )
            ExpandAndShrink(visible = showDrawerSearchBar.state.value) {
                DividerColumn {
                    SwitchPreference(
                        adapter = prefs2.autoShowKeyboardInDrawer.getAdapter(),
                        label = stringResource(id = R.string.pref_search_auto_show_keyboard),
                    )
                    if (!deviceSearchEnabled) {
                        SwitchPreference(
                            adapter = prefs2.enableFuzzySearch.getAdapter(),
                            label = stringResource(id = R.string.fuzzy_search_title),
                            description = stringResource(id = R.string.fuzzy_search_desc),
                        )
                    }
                    SliderPreference(
                        label = stringResource(id = R.string.max_search_result_count_title),
                        adapter = prefs2.maxSearchResultCount.getAdapter(),
                        step = 1,
                        valueRange = 5..15,
                    )
                }
            }
        }

        val isDeviceSearch = prefs.performWideSearchExperimental.get() && showDrawerSearchBar.state.value
        if (isDeviceSearch) {
            PreferenceGroup(heading = stringResource(id = R.string.pref_advance_search_category)) {
                SwitchPreference(
                    adapter = prefs2.performWideSearch.getAdapter(),
                    label = stringResource(id = R.string.perform_wide_search_title),
                )
                ExpandAndShrink(visible = prefs2.performWideSearch.getAdapter().state.value) {
                    DividerColumn {
                        SwitchPreference(
                            adapter = prefs.searchResultFiles.getAdapter(),
                            label = stringResource(id = R.string.perform_wide_search_file),
                            description = stringResource(
                                id = if (
                                    filesAndStorageGranted(context)
                                ) {
                                    R.string.all_apps_search_result_files_description
                                } else {
                                    R.string.warn_files_permission_content
                                },
                            ),
                            onClick = { checkAndRequestFilesPermission(context, prefs) },
                        )
                        ExpandAndShrink(visible = prefs.searchResultFiles.getAdapter().state.value) {
                            SliderPreference(
                                label = stringResource(id = R.string.max_file_result_count_title),
                                adapter = prefs2.maxFileResultCount.getAdapter(),
                                step = 1,
                                valueRange = 3..10,
                            )
                        }
                        SwitchPreference(
                            adapter = prefs.searchResultPeople.getAdapter(),
                            label = stringResource(id = R.string.search_pref_result_people_title),
                            description = stringResource(
                                id = if (
                                    contactPermissionGranted(context)
                                ) {
                                    R.string.all_apps_search_result_contacts_description
                                } else {
                                    R.string.warn_contact_permission_content
                                },
                            ),
                            onClick = { requestContactPermissionGranted(context, prefs) },
                        )
                        ExpandAndShrink(visible = prefs.searchResultPeople.getAdapter().state.value) {
                            SliderPreference(
                                label = stringResource(id = R.string.max_people_result_count_title),
                                adapter = prefs2.maxPeopleResultCount.getAdapter(),
                                step = 1,
                                valueRange = 3..15,
                            )
                        }
                        SwitchPreference(
                            adapter = prefs.searchResultSettingsEntry.getAdapter(),
                            label = stringResource(id = R.string.search_pref_result_settings_entry_title),
                        )
                        ExpandAndShrink(visible = prefs.searchResultSettingsEntry.getAdapter().state.value) {
                            SliderPreference(
                                label = stringResource(id = R.string.max_settings_entry_result_count_title),
                                adapter = prefs2.maxSettingsEntryResultCount.getAdapter(),
                                step = 1,
                                valueRange = 2..10,
                            )
                        }
                    }
                }
            }
        }

        ExpandAndShrink(visible = showDrawerSearchBar.state.value) {
            PreferenceGroup(heading = stringResource(id = R.string.pref_suggestion_label)) {
                DividerColumn {
                    SwitchPreference(
                        adapter = prefs.searchResultStartPageSuggestion.getAdapter(),
                        label = stringResource(id = R.string.pref_suggestion_title),
                    )
                    ExpandAndShrink(visible = prefs.searchResultStartPageSuggestion.getAdapter().state.value) {
                        DividerColumn {
                            SliderPreference(
                                label = stringResource(id = R.string.max_suggestion_result_count_title),
                                adapter = prefs2.maxSuggestionResultCount.getAdapter(),
                                step = 1,
                                valueRange = 3..10,
                            )
                            SliderPreference(
                                label = stringResource(id = R.string.max_web_suggestion_delay),
                                adapter = prefs2.maxWebSuggestionDelay.getAdapter(),
                                step = 100,
                                valueRange = 200..5000,
                                showUnit = "ms",
                            )
                        }
                    }
                    SwitchPreference(
                        adapter = prefs.searchResulRecentSuggestion.getAdapter(),
                        label = stringResource(id = R.string.pref_recent_suggestion_title),
                    )

                    ExpandAndShrink(visible = prefs.searchResulRecentSuggestion.getAdapter().state.value) {
                        SliderPreference(
                            label = stringResource(id = R.string.max_recent_result_count_title),
                            adapter = prefs2.maxRecentResultCount.getAdapter(),
                            step = 1,
                            valueRange = 1..10,
                        )
                    }
                }
            }
        }
        if (deviceSearchEnabled) {
            ExpandAndShrink(visible = showDrawerSearchBar.state.value) {
                PreferenceGroup(heading = stringResource(id = R.string.show_search_result_types)) {
                    SwitchPreference(
                        adapter = prefs.searchResultShortcuts.getAdapter(),
                        label = stringResource(id = R.string.search_pref_result_shortcuts_title),
                    )
                    SwitchPreference(
                        adapter = prefs.searchResultPeople.getAdapter(),
                        label = stringResource(id = R.string.search_pref_result_people_title),
                    )
                    SwitchPreference(
                        adapter = prefs.searchResultPixelTips.getAdapter(),
                        label = stringResource(id = R.string.search_pref_result_tips_title),
                    )
                }
            }
        }
        PreferenceGroup(heading = stringResource(id = R.string.grid)) {
            SliderPreference(
                label = stringResource(id = R.string.app_drawer_columns),
                adapter = prefs2.drawerColumns.getAdapter(),
                step = 1,
                valueRange = 3..10,
            )
            SliderPreference(
                adapter = prefs2.drawerCellHeightFactor.getAdapter(),
                label = stringResource(id = R.string.row_height_label),
                valueRange = 0.7F..1.5F,
                step = 0.1F,
                showAsPercentage = true,
            )
            SliderPreference(
                adapter = prefs2.drawerLeftRightMarginFactor.getAdapter(),
                label = stringResource(id = R.string.app_drawer_indent_label),
                valueRange = 0.0F..2.0F,
                step = 0.01F,
                showAsPercentage = true,
            )
            SwitchPreference(
                adapter = prefs2.enableDrawerShuffle.getAdapter(),
                label = stringResource(
                    id = R.string.shuffle_drawer,
                ),
            )
        }
        PreferenceGroup(heading = stringResource(id = R.string.icons)) {
            SliderPreference(
                label = stringResource(id = R.string.icon_size),
                adapter = prefs2.drawerIconSizeFactor.getAdapter(),
                step = 0.1f,
                valueRange = 0.5F..1.5F,
                showAsPercentage = true,
            )
            val showDrawerLabels = prefs2.showIconLabelsInDrawer.getAdapter()
            SwitchPreference(
                adapter = showDrawerLabels,
                label = stringResource(id = R.string.show_home_labels),
            )
            ExpandAndShrink(visible = showDrawerLabels.state.value) {
                SliderPreference(
                    label = stringResource(id = R.string.label_size),
                    adapter = prefs2.drawerIconLabelSizeFactor.getAdapter(),
                    step = 0.1F,
                    valueRange = 0.5F..1.5F,
                    showAsPercentage = true,
                )
            }
        }
    }
}
