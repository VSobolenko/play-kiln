# String keys — Lumintra: Light Planner

Key-by-key index of the slot's string resources. It describes exactly two files and must
agree with them character for character:

| File | Role |
|---|---|
| `app/src/main/res/values/strings.xml` | the default (`en-US`) catalogue — the source of truth |
| `app/src/main/res/values-ru/strings.xml` | the `ru-RU` mirror, **translatable keys only** |
| `app/src/main/res/values/strings_platform.xml` | the manifest / `appwidget-provider` strings |
| `app/src/main/res/values-ru/strings_platform.xml` | the `ru-RU` mirror of the platform strings |

## How to read this index

- **`[NT]`** — the string is `translatable="false"` and is declared **only** in
  `values/strings.xml`. A licence credit, a licence URL, the brand mark, the version, the
  support address and the privacy-policy URL are not translation units; Android resolves them
  from the default resources for every locale. A copy under `values-ru/` is a duplicate that
  `scripts/check-locales.mjs` fails.
- **`[plurals: one/other]`** — a `<plurals>` block. English carries `one` and `other`; Russian
  carries `one`, `few`, `many` and `other`. Every format argument of the default repeats in
  `values-ru/` in the **same order**.
- Everything else is a plain translatable `<string>`. Format placeholders (`%1$s`, `%1$d`,
  `%1$.1f`) are named where the key carries one.
- `\u2019`-style quotes and apostrophes are Android-escaped (`\'`) in the XML, as required.

Counts (measured): **590 strings + 5 plurals = 595 entries** in `values/strings.xml`, of which
**23 are `[NT]`**; **567 strings + 5 plurals = 572 translatable entries** in each of `values/`
and `values-ru/`.

---

## App identity

| Key | Purpose |
|---|---|
| `app_name` | The store/launcher title: `Lumintra: Light Planner`. |
| `app_idea` | One-sentence statement of what the app is, used where the slot describes itself. |
| `app_name_short` `[NT]` | The brand as it appears inside a shared payload; a brand is not translated. |

## Navigation and the shared control vocabulary

| Key | Purpose |
|---|---|
| `nav_night` | Section label for the Night scene. |
| `nav_brief` | Section label for the session Brief. |
| `nav_targets` | Section label for the Targets catalogue. |
| `nav_spots` | Section label for the Spots list. |
| `nav_settings` | Section label for Settings. |
| `nav_back` | Generic back action label. |
| `nav_close` | Generic close/dismiss action label. |
| `nav_day_previous` | Move the chosen night one night back. |
| `nav_day_next` | Move the chosen night one night forward. |
| `action_cancel` | Cancel a dialog or discard a draft. |
| `action_save` | Commit the current edits. |
| `action_delete` | Delete the current record. |
| `action_edit` | Open the current record for editing. |
| `action_retry` | Retry a failed load. |
| `action_refresh` | Refresh data from its source. |
| `action_done` | Dismiss after a completed action. |
| `action_confirm` | Confirm a consequential action. |
| `state_loading` | Generic in-progress indicator. |
| `state_unavailable` | Generic "not available" state. |
| `state_offline` | Offline state marker. |
| `label_forecast` | Provenance label: the value shown is a forecast. |
| `label_prediction` | Provenance label: the value shown is a prediction. |
| `label_measured` | Provenance label: the value shown is measured. |

## Shared components — compass points

The only user-visible words the components in `ui/components/` own. A cardinal direction is a
word, not a glyph, and it genuinely differs in Russian (`С / СВ / В …`).

| Key | Purpose |
|---|---|
| `component_compass_n` | Cardinal direction North. |
| `component_compass_ne` | Intercardinal direction North-east. |
| `component_compass_e` | Cardinal direction East. |
| `component_compass_se` | Intercardinal direction South-east. |
| `component_compass_s` | Cardinal direction South. |
| `component_compass_sw` | Intercardinal direction South-west. |
| `component_compass_w` | Cardinal direction West. |
| `component_compass_nw` | Intercardinal direction North-west. |

## Welcome — first run

Shown only while the spot store holds no record.

| Key | Purpose |
|---|---|
| `welcome_title` | Headline: plan the light before you leave. |
| `welcome_body` | Explains that the plan is computed from a spot and the clock. |
| `welcome_compass_line` | **The required line**: the app never asks to point the phone at the sky and requests no location permission. |
| `welcome_action_set_spot` | Primary action: set the first spot. |
| `welcome_action_content_description` | TalkBack name for the set-a-spot control (map, paste or type). |
| `welcome_spot_subline` | Explains why a spot is the one thing needed first. |
| `welcome_first_run_note` | States that nothing is computed until a spot exists. |
| `welcome_notes_header` | Header for the "before you start" notes. |
| `welcome_no_network_note` | Typing or pasting a coordinate works entirely offline. |
| `welcome_no_account_note` | No account, no sign-in, no permission prompt. |

## Night scene

The scrubbable sky, its layers, its lens frame, its red mode and its mandatory ESO credit.

| Key | Purpose |
|---|---|
| `night_title` | Screen title: Night. |
| `night_scrub_label` | Label for the time-of-night scrubber. |
| `night_scrub_content_description` | TalkBack name for the scrubber, naming the two bounds (`%1$s`–`%2$s`). |
| `night_scrub_hint` | Hint: drag to move through the night. |
| `night_scrub_now` | Control to jump the scrubber to now. |
| `night_named` | Names the displayed night (`%1$s`). |
| `night_layers_header` | Header for the layer toggles. |
| `night_layer_milky_way` | Toggle: Milky Way layer. |
| `night_layer_constellations` | Toggle: constellation-figure layer. |
| `night_layer_horizon` | Toggle: the user's drawn horizon layer. |
| `night_layer_star_trails` | Toggle: star-trails layer. |
| `night_layers_shown` | Count of layers shown (`%1$d` of `%2$d`). |
| `night_lens_frame` | Toggle: the lens-field-of-view frame overlay. |
| `night_lens_frame_content_description` | TalkBack name for the frame: focal length and FOV (`%1$s`,`%2$s`,`%3$s`). |
| `night_lens_preset_title` | Title for the lens preset selector. |
| `night_lens_preset_apply` | Apply the chosen lens preset to this session. |
| `night_lens_preset_current` | States the active preset (`%1$s` mm). |
| `night_satellite_streak` | Toggle: the satellite-streak flag. |
| `night_satellite_streak_on` | States which satellite is crossing the frame (`%1$s`). |
| `night_red_mode_toggle` | Control label for red night mode. |
| `night_red_mode_on` | Names what red mode does (blue zero, green ≤ 8 of 255). |
| `night_red_mode_off` | States red night mode is off. |
| `night_single_tap_hint` | Hint: tap the scene to select the nearest object. |
| `night_objects_header` | Header for the "in this frame" object list. |
| `night_object_row` | One object row: name, altitude, azimuth (`%1$s`,`%2$s`,`%3$s`). |
| `night_no_objects` | Empty state: nothing is above the horizon in this frame. |
| `night_sky_grade_label` | Label for the sky-darkness row. |
| `night_sky_grade_value` | Rendered darkness value with its Bortle class (`%1$s`,`%2$s`). |
| `night_sky_grade_unknown` | States that darkness has not been estimated. |
| `night_user_estimate_label` | The "user estimate" label placed beside the computed value. |
| `night_user_estimate_sqm` | The user's SQM reading, labelled (`%1$s` mag/arcsec²). |
| `night_user_estimate_bortle` | The user's Bortle class, labelled (`%1$s`). |
| `night_horizon_flat_note` | States a flat horizon is used because no profile exists. |
| `night_horizon_blocked_note` | States the drawn horizon blocks the whole sky. |
| `night_horizon_blocked_confirm` | Action: keep the all-blocking horizon. |
| `night_horizon_blocked_redraw` | Action: redraw the horizon. |
| `night_polar_day_note` | States the Sun does not set, so the daylight palette is kept. |
| `night_eso_credit` | The mandatory `ESO/S. Brunier` panorama credit rendered beside the backdrop. |

## Brief — the Tonight summary

States: loading, empty, no-spot, offline and the four no-darkness outcomes; sections: Tonight,
dark window, moon, targets, passes, cloud.

| Key | Purpose |
|---|---|
| `brief_title` | Screen title: Tonight. |
| `brief_spot_label` | Label for the active spot row. |
| `brief_night_of` | Names the displayed night's two bounds (`%1$s`→`%2$s`). |
| `brief_no_spot_title` | Empty-state title: no spot saved. |
| `brief_no_spot_body` | Explains nothing is shown until a real place is saved. |
| `brief_no_spot_action` | Action: go set a spot. |
| `brief_loading` | Loading state, naming the spot (`%1$s`). |
| `brief_offline_note` | Offline state: local answers are present; forecast/passes show the last reading. |
| `brief_dark_window_header` | Header for the dark-window section. |
| `brief_dark_window_value` | The dark window with its kind (`%1$s`–`%2$s`, `%3$s`). |
| `brief_dark_window_second` | States a second dark window (`%1$s`–`%2$s`). |
| `brief_dark_kind_dark` | Dark-window kind: astronomical darkness. |
| `brief_dark_kind_polar_day` | Dark-window kind: the Sun does not set. |
| `brief_dark_kind_polar_night` | Dark-window kind: the Sun does not rise. |
| `brief_dark_kind_no_astro` | Dark-window kind: no astronomical darkness. |
| `brief_dark_kind_no_dark` | Dark-window kind: no darkness. |
| `brief_no_sunset_tonight` | `POLAR_DAY` outcome sentence (`%1$s`). |
| `brief_no_sunrise_today` | `POLAR_NIGHT` outcome sentence (`%1$s`). |
| `brief_no_astro_dark` | `NO_ASTRO_DARK` outcome sentence (`%1$s`–`%4$s`). |
| `brief_no_dark` | `NO_DARK` outcome sentence (`%1$s`–`%3$s`). |
| `brief_twilight_civil` | Twilight level name: civil. |
| `brief_twilight_nautical` | Twilight level name: nautical. |
| `brief_sun_altitude` | States the Sun's altitude at an instant (`%1$s`,`%2$s`). |
| `brief_twilight_table_header` | Header for the twilight table. |
| `brief_twilight_row` | One twilight row: level and interval (`%1$s`–`%3$s`). |
| `brief_moon_header` | Header for the Moon section. |
| `brief_moon_value` | Moon fraction with its state (`%1$s`%%, `%2$s`). |
| `brief_moon_up_all_night` | Moon state: up all night. |
| `brief_moon_below_all_night` | Moon state: below the horizon all night. |
| `brief_moon_rises_sets` | Moon state: rises and sets (`%1$s`,`%2$s`). |
| `brief_moon_rises` | Moon state: rises only (`%1$s`). |
| `brief_moon_sets` | Moon state: sets only (`%1$s`). |
| `brief_moonless_window` | States the moonless window (`%1$s`–`%2$s`). |
| `brief_moonless_none` | States there is no moonless window tonight. |
| `brief_targets_header` | Header for the targets section of the brief. |
| `brief_targets_none` | States no target is chosen for the session. |
| `brief_passes_header` | Header for the satellite-passes section. |
| `brief_cloud_header` | Header for the cloud-outlook section. |
| `brief_zone_label` | Names the spot's time zone (`%1$s`). |
| `brief_zone_other` | Names the device's time zone when it differs (`%1$s`). |
| `brief_clock_warning` | Warns the device clock is off by an interval (`%1$s`). |
| `brief_saved_spot_unreadable` | Note: one saved spot could not be read. |
| `brief_saved_target_missing` | Note: one saved reference no longer exists in the catalogue. |
| `brief_horizon_flat_note` | Note: a flat horizon is used for this spot. |
| `brief_share_action` | Action: share the brief. |
| `brief_open_night_action` | Action: open the night scene. |

## Targets — the catalogue and its detail

Search, filters, the magnitude limit, aliases and the `TargetWindow` outcomes.

| Key | Purpose |
|---|---|
| `target_title` | Screen title: Targets. |
| `target_back` | Back action on the Targets surface. |
| `target_search_field` | Placeholder naming what the search matches. |
| `target_search_hint` | Short hint for the search field. |
| `target_search_clear` | Action: clear the search. |
| `target_filter_magnitude` | Filter label: magnitude limit. |
| `target_filter_type` | Filter label: object type. |
| `target_filter_any` | Type-filter value: any. |
| `target_filter_magnitude_value` | Rendered magnitude limit (`%1$s` mag). |
| `target_filter_favourites` | Filter: favourites only. |
| `target_filter_visible_tonight` | Filter: up tonight. |
| `target_filter_clear` | Action: clear all filters. |
| `target_magnitude_unknown` | States the magnitude is unknown. |
| `target_magnitude_limit_any` | States there is no magnitude limit. |
| `target_mag_line` | Magnitude line: value, kind, note (`%1$s`–`%3$s`). |
| `target_mag_kind_integrated` | Magnitude kind: integrated. |
| `target_mag_kind_peak` | Magnitude kind: peak. |
| `target_mag_kind_unknown` | Magnitude kind: unknown. |
| `target_magnitude_disputed` | Note: the magnitude is disputed between sources. |
| `target_size_unknown` | States the apparent size is unknown. |
| `target_size_format` | Single-size value (`%1$s`′). |
| `target_size_pair` | Two-axis size value (`%1$s`′ × `%2$s`′). |
| `target_row_metrics` | List row metric pair (`%1$s` · `%2$s`). |
| `target_favourite_mark` | Favourite marker label. |
| `target_favourite_add` | TalkBack name to add a favourite (`%1$s`). |
| `target_favourite_remove` | TalkBack name to remove a favourite (`%1$s`). |
| `target_up_tonight` | Badge: up tonight. |
| `target_type_galaxy` | Object type: galaxy. |
| `target_type_galaxy_group` | Object type: group of galaxies. |
| `target_type_globular_cluster` | Object type: globular cluster. |
| `target_type_open_cluster` | Object type: open cluster. |
| `target_type_emission_nebula` | Object type: emission nebula. |
| `target_type_reflection_nebula` | Object type: reflection nebula. |
| `target_type_planetary_nebula` | Object type: planetary nebula. |
| `target_type_supernova_remnant` | Object type: supernova remnant. |
| `target_type_dark_nebula` | Object type: dark nebula. |
| `target_type_asterism` | Object type: asterism. |
| `target_type_double_star` | Object type: double star. |
| `target_type_planet` | Object type: planet. |
| `target_type_moon` | Object type: Moon. |
| `target_type_sun` | Object type: Sun. |
| `target_list_unavailable` | Error title: the catalogue could not be read. |
| `target_list_unavailable_body` | Error body: no targets listed; the rest of the brief still works. |
| `target_no_result` | Empty search result, naming the query (`%1$s`). |
| `target_no_result_body` | Advice for an empty search result. |
| `target_favourites_empty` | Empty state: no favourites yet. |
| `target_favourites_empty_body` | Explains how to mark a favourite. |
| `target_none_visible_tonight` | Empty state for a night with nothing up (`%1$s`). |
| `target_none_visible_tonight_body` | Explains no filtered target clears the horizon that night. |
| `target_needs_spot` | Gate title: no spot yet. |
| `target_needs_spot_body` | Explains a spot is needed to compute what is up. |
| `target_needs_night` | Gate title: no night chosen. |
| `target_needs_night_body` | Explains a night is needed to compute what is up. |
| `catalogue_count_label` | Catalogue size label (`%1$d` targets). |
| `target_results_summary` | Result count (`%1$d` results). |
| `target_results_summary_filters` | Result count with active filters (`%1$d`, `%2$s`). |
| `target_filters_none` | States no filters are active. |
| `target_night_named` | Names the chosen night (`%1$s`). |
| `milky_way_core_name` | The Milky Way core's display name in the target list. |
| `target_detail_select_prompt` | Detail-pane prompt: select a target. |
| `target_detail_select_prompt_body` | Explains what a selected row shows. |
| `target_detail_needs_spot` | Detail gate: no spot active. |
| `target_detail_needs_spot_body` | Explains rise/transit/set need a saved spot. |
| `target_detail_needs_night` | Detail gate: no night chosen. |
| `target_detail_needs_night_body` | Explains rise/transit/set need a chosen night. |
| `target_detail_close` | TalkBack name to close the target detail. |
| `target_detail_catalogue_facts` | Detail section header: catalogue facts. |
| `target_detail_designations` | Detail section header: designations. |
| `target_detail_aliases` | Detail section header: also known as. |
| `target_detail_sources` | Detail section header: verified against. |
| `target_detail_sources_value` | Cross-check result with the two sources and the delta (`%1$s`–`%3$s`). |
| `target_detail_window` | Detail section header: this night. |
| `target_detail_fit` | Detail section header: lens fit. |
| `target_detail_position` | Detail section header: position now. |
| `target_window_rise` | Window outcome: rises at (`%1$s`). |
| `target_window_transit` | Window outcome: highest at (`%1$s`,`%2$s`°). |
| `target_window_set` | Window outcome: sets at (`%1$s`). |
| `target_circumpolar_short` | Short outcome: up all night. |
| `target_never_rises_short` | Short outcome: never rises from this spot. |
| `target_behind_horizon` | States a target rises behind the drawn horizon (`%1$s`). |
| `target_occluded_short` | Short outcome: behind your drawn horizon. |
| `target_never_rises` | Full outcome: never rises from this spot (`%1$s`). |
| `target_circumpolar` | Full outcome: up all night with its lowest point (`%1$s`–`%3$s`). |
| `target_partial_window` | Full outcome: window clipped by the dark window (`%1$s`–`%3$s`). |
| `target_at_horizon` | Full outcome: clears the drawn horizon at an instant (`%1$s`–`%3$s`). |
| `target_graze` | Full outcome: grazes the horizon with peak and duration (`%1$s`–`%3$s`). |
| `target_reference_missing` | States a saved reference was dropped from the catalogue. |
| `target_interval_line` | Interval line with peak and transit (`%1$s`–`%4$s`). |
| `target_interval_no_transit` | Interval line with peak only (`%1$s`–`%3$s`). |
| `target_multiple_intervals` | States how many windows exist tonight (`%1$d`). |
| `target_fit_fits` | Lens-fit verdict: fits the frame. |
| `target_fit_marginal` | Lens-fit verdict: marginal for the frame. |
| `target_fit_too_large` | Lens-fit verdict: too large for the frame. |
| `target_fit_point_source` | Lens-fit verdict: no stated size, so no fit statement. |
| `target_fit_numbers` | Lens-fit arithmetic line (`%1$s`–`%5$s`). |
| `target_fit_pixels` | Lens-fit pixel count across the short side (`%1$s`). |
| `target_visible_yes` | States the target is up tonight at this spot. |
| `target_visible_no` | States the target is not up tonight at this spot. |
| `target_alt_az` | Position line: altitude and azimuth (`%1$s`,`%2$s`). |
| `target_aliases_none` | States no aliases are recorded. |

## Satellite passes

| Key | Purpose |
|---|---|
| `pass_title` | Section title: satellite passes. |
| `pass_empty` | States no observable pass on this night. |
| `pass_no_crossing` | States no horizon crossing is predicted and that it is a prediction. |
| `pass_prediction_note` | Reminder that a pass is predicted from elements, not observed. |
| `pass_age_days` | Warns the elements are old and times may shift (`%1$d` days). |
| `pass_stale_badge` | Badge showing the element age (`%1$d` days). |
| `pass_expired` | States elements are too old to predict a pass (`%1$s`,`%2$d`). |
| `pass_malformed` | States satellite data could not be read. |
| `pass_shadow_note` | Pass note: the satellite is in the Earth's shadow. |
| `pass_bright_note` | Pass note: the sky is too bright. |
| `pass_row` | One pass row: object, interval, elevation, note (`%1$s`–`%5$s`). |
| `pass_refresh` | Action: refresh the orbital elements. |
| `pass_refresh_offline` | States elements cannot be refreshed offline (`%1$s`). |
| `pass_object_picker` | Label for the satellite picker. |
| `pass_no_elements_for_object` | States no elements exist for the chosen object (`%1$s`). |
| `pass_behind_horizon` | States a pass rises behind the drawn horizon (`%1$s`). |
| `pass_count` `[plurals: one/other]` | Count of passes tonight (`%1$d`). |
| `pass_element_age_days` `[plurals: one/other]` | Element age in days (`%1$d`). |

## Cloud forecast — MET Norway

| Key | Purpose |
|---|---|
| `cloud_title` | Section title: cloud outlook. |
| `cloud_total_cover_label` | Label for total cloud cover. |
| `cloud_clear` | Cloud descriptor: clear. |
| `cloud_partly` | Cloud descriptor: partly cloudy. |
| `cloud_overcast` | Cloud descriptor: overcast. |
| `cloud_hour_row` | One forecast row: time, descriptor, percent (`%1$s`–`%3$s`). |
| `cloud_hour_label` | Compact hourly label: time, percent, descriptor (`%1$s`–`%3$s`). |
| `cloud_source_line` | Credits the forecast source and retrieval time (`%1$s`,`%2$s`). |
| `cloud_source_reading` | States the reading's age (`%1$s`,`%2$s`). |
| `cloud_mean_over_window` | Mean cloud over the dark window (`%1$s`%%). |
| `cloud_no_forecast` | States no forecast is available. |
| `cloud_offline_from` | Offline fallback with the last reading (`%1$s`,`%2$s`). |
| `cloud_unavailable` | States the forecast is unavailable. |
| `cloud_temporarily_unavailable` | States the forecast is temporarily unavailable. |
| `cloud_no_hours` | States the forecast does not cover the window. |
| `cloud_forecast_disclosure` | Discloses that coordinates are sent truncated to 4 decimals when the forecast is on. |
| `cloud_forecast_attribution` `[NT]` | The MET Norway attribution, `Data from MET Norway (met.no) — CC BY 4.0`, kept beside the forecast. |

## Spots

The list, the editor, the coordinate routes, the horizon editor and the lens presets.

| Key | Purpose |
|---|---|
| `spot_title` | Screen title: Spots. |
| `spot_empty_title` | Empty-state title: no spot saved. |
| `spot_empty_body` | Explains everything is computed from a real place. |
| `spot_add` | Action: add a spot. |
| `spot_list_header` | Header for the saved-spot list. |
| `spot_active_label` | Marks the active spot. |
| `spot_row_subtitle` | Spot row subtitle: coordinates and elevation (`%1$s`–`%3$s`). |
| `spot_count` `[plurals: one/other]` | Count of saved spots (`%1$d`). |
| `spot_edit_title_new` | Editor title when creating. |
| `spot_edit_title_edit` | Editor title when editing. |
| `spot_edit_name` | Field label: name. |
| `spot_edit_note` | Field label: note. |
| `spot_edit_elevation` | Field label: elevation. |
| `spot_edit_timezone` | Field label: time zone. |
| `spot_edit_save` | Action: save the spot. |
| `spot_edit_delete` | Action: delete the spot. |
| `spot_edit_delete_confirm` | Delete confirmation, naming the spot (`%1$s`). |
| `spot_edit_discard_confirm` | Discard confirmation for an unsaved draft. |
| `spot_edit_coordinates_header` | Header for the coordinates group. |
| `spot_edit_latitude` | Field label: latitude. |
| `spot_edit_longitude` | Field label: longitude. |
| `spot_edit_latitude_hint` | Hint for the latitude field. |
| `spot_edit_longitude_hint` | Hint for the longitude field. |
| `spot_edit_use_decimal` | Coordinate entry mode: decimal degrees. |
| `spot_edit_use_dms` | Coordinate entry mode: degrees, minutes, seconds. |
| `spot_edit_paste` | Action: paste coordinates. |
| `spot_edit_paste_hint` | Example formats for pasting coordinates. |
| `spot_edit_paste_invalid` | Error: pasted coordinates could not be read. |
| `spot_edit_paste_out_of_range` | Error: coordinates are outside their valid range. |
| `spot_edit_picker` | Action: open the coordinate picker. |
| `spot_edit_picker_content_description` | TalkBack name for the coordinate picker control. |
| `spot_edit_picker_hint` | Picker hint: pan to move the crosshair, no location permission. |
| `spot_edit_picker_zoom_in` | Picker control: zoom in. |
| `spot_edit_picker_zoom_out` | Picker control: zoom out. |
| `spot_edit_lookup_title` | Title for the place-name lookup. |
| `spot_edit_lookup_action` | Action: look up the place name. |
| `spot_edit_lookup_hint` | Hint for the place-name field. |
| `spot_edit_lookup_unavailable` | States address lookup is unavailable. |
| `spot_edit_lookup_approximate_only` | Explains a place name gives an approximate coordinate. |
| `spot_edit_approximate_label` | The `approximate` label shown on a looked-up spot. |
| `spot_edit_lookup_result` | Lookup result line: name and coordinate (`%1$s`–`%3$s`). |
| `coord_hemisphere_n` | Hemisphere letter N. |
| `coord_hemisphere_s` | Hemisphere letter S. |
| `coord_hemisphere_e` | Hemisphere letter E. |
| `coord_hemisphere_w` | Hemisphere letter W. |
| `coord_hemisphere_label` | Label for the hemisphere selector. |
| `coord_degrees` | Field label: degrees. |
| `coord_minutes` | Field label: minutes. |
| `coord_seconds` | Field label: seconds. |
| `coord_decimal_pair` | Decimal coordinate pair (`%1$s`, `%2$s`). |
| `coord_dms_pair` | DMS coordinate pair (`%1$s`–`%8$s`). |
| `coord_copy` | Action: copy the coordinate. |
| `coord_copied` | Confirmation: the coordinate is on the clipboard. |
| `spot_edit_horizon_field_label` | Field label: horizon. |
| `spot_edit_horizon_hint` | Hint for drawing the horizon silhouette. |
| `spot_edit_horizon_description` | TalkBack description of the horizon editor's geometry. |
| `spot_edit_horizon_axis_range` | States the editor's azimuth and elevation range. |
| `spot_edit_horizon_selected_azimuth` | Names the selected azimuth (`%1$s`°). |
| `spot_edit_horizon_field_value` | Elevation value for the selected azimuth (`%1$s`°). |
| `spot_edit_horizon_elevation_value` | Bare elevation value (`%1$s`°). |
| `spot_edit_horizon_select_azimuth_description` | TalkBack name for selecting an azimuth. |
| `spot_edit_horizon_set_elevation_description` | TalkBack name for setting an elevation. |
| `spot_edit_horizon_step_up` | Action: raise the horizon at this azimuth. |
| `spot_edit_horizon_step_down` | Action: lower the horizon at this azimuth. |
| `spot_edit_horizon_reset` | Action: reset the horizon. |
| `spot_edit_horizon_no_profile` | States a flat horizon is used until one is drawn. |
| `spot_edit_horizon_full_sky_note` | Warns this profile blocks the whole sky. |
| `spot_edit_horizon_flat_note` | States an all-zero profile is a legal flat horizon. |
| `spot_edit_lens_header` | Header for the lens and sensor group. |
| `spot_edit_lens_preset` | Label for the lens preset selector. |
| `spot_edit_lens_custom` | Lens preset value: custom. |
| `spot_edit_lens_focal` | Field label: focal length in mm. |
| `spot_edit_lens_sensor_width` | Field label: sensor width in mm. |
| `spot_edit_lens_sensor_height` | Field label: sensor height in mm. |
| `spot_edit_lens_field_of_view` | Computed field of view (`%1$s`° × `%2$s`°). |
| `spot_edit_lens_equipment_profile` | Label for the equipment profile. |
| `spot_export` | Action: export every spot. |
| `spot_export_content_description` | TalkBack name for the export action. |
| `spot_export_done` | Confirmation naming the written file (`%1$s`). |
| `spot_export_failed` | States the scout file could not be written. |
| `spot_export_nothing` | States there is no spot to export. |
| `spot_import` | Action: import a scout file. |
| `spot_import_content_description` | TalkBack name for the import action. |
| `spot_import_cancelled` | States no file was chosen, so nothing changed. |
| `spot_import_not_a_scout_file` | Error: the file is not a Lumintra scout file. |
| `spot_import_unknown_version` | Import outcome: unknown declared version (`%1$s`). |
| `spot_import_invalid_coordinate` | Import outcome: out-of-range coordinate (`%1$s`,`%2$s`). |
| `spot_import_invalid_horizon` | Import outcome: wrong horizon sample count (`%1$s`,`%2$d`). |
| `spot_import_invalid_sample` | Import outcome: an out-of-range horizon sample (`%1$s`). |
| `spot_import_preview` | Import preview: counts to add, replacing nothing (`%1$s`,`%2$s`). |
| `spot_import_done` | Import result: added and skipped counts (`%1$s`,`%2$s`). |
| `spot_import_added_count` `[plurals: one/other]` | Count of records added (`%1$d`). |
| `spot_import_skipped_count` `[plurals: one/other]` | Count of records skipped (`%1$d`). |
| `spot_store_quarantined` | Note: one saved spot could not be read and was set aside. |
| `spot_saved_confirm` | Confirmation naming the saved spot (`%1$s`). |

## Share — the brief payload and the postcard

| Key | Purpose |
|---|---|
| `share_title` | Screen title: share the brief. |
| `share_back` | Back action on the Share surface. |
| `share_mode_text` | Mode: text brief. |
| `share_mode_postcard` | Mode: postcard. |
| `share_mode_selected` | Marks the selected mode. |
| `share_preview_header` | Header for the preview. |
| `share_preview_text_description` | TalkBack name for the text preview and its length (`%1$d`). |
| `share_preview_postcard_description` | TalkBack name for the postcard preview (`%1$s`–`%5$s`). |
| `share_postcard_preview` | Label for the postcard preview. |
| `share_send` | Action: send the brief. |
| `share_save` | Action: save the postcard. |
| `share_loading` | Loading state while composing the brief. |
| `share_no_spot_title` | Empty-state title: no spot saved. |
| `share_no_spot_body` | Explains the brief is composed for a real place only. |
| `share_no_spot_action` | Action: open spots. |
| `share_postcard_unavailable` | States the postcard could not be composed. |
| `share_no_target_app` | States no app can receive the share, so it was copied. |
| `share_copy_text` | Action: copy the text brief. |
| `share_copied` | Confirmation: the brief is on the clipboard. |
| `share_save_failed` | States the postcard could not be saved. |
| `share_saved` | Confirmation: the postcard was saved. |
| `share_text_header` | Payload header naming the app and the session (`%1$s`). |
| `share_text_spot` | Payload line: spot name (`%1$s`). |
| `share_text_position` | Payload line: coordinates (`%1$s`,`%2$s`). |
| `share_text_zone` | Payload line: time zone (`%1$s`). |
| `share_text_night` | Payload line: night (`%1$s`). |
| `share_text_dark_window` | Payload line: astronomical darkness interval (`%1$s`–`%2$s`). |
| `share_text_dark_window_second` | Payload line: second dark window (`%1$s`–`%2$s`). |
| `share_text_dark_kind` | Payload line: dark-window kind (`%1$s`). |
| `share_dark_kind_dark` | Payload kind: astronomical dark. |
| `share_dark_kind_polar_day` | Payload kind: the Sun does not set. |
| `share_dark_kind_polar_night` | Payload kind: the Sun does not rise. |
| `share_dark_kind_no_astro` | Payload kind: no astronomical darkness. |
| `share_dark_kind_no_dark` | Payload kind: no darkness at all. |
| `share_text_polar_day` | Payload sentence for a polar-day night. |
| `share_text_no_astro_dark` | Payload sentence for a no-astronomical-darkness night (`%1$s`–`%3$s`). |
| `share_text_no_dark` | Payload sentence for a night with no darkness (`%1$s`,`%2$s`). |
| `share_text_polar_night` | Payload sentence for a polar-night day (`%1$s`–`%2$s`). |
| `share_text_civil` | Payload twilight line: civil (`%1$s`–`%2$s`). |
| `share_text_nautical` | Payload twilight line: nautical (`%1$s`–`%2$s`). |
| `share_text_astronomical` | Payload twilight line: astronomical (`%1$s`–`%2$s`). |
| `share_text_twilight_level` | Payload twilight line, generic (`%1$s`–`%3$s`). |
| `share_text_twilight_civil_short` | Short twilight word: civil. |
| `share_text_twilight_nautical_short` | Short twilight word: nautical. |
| `share_text_twilight_astronomical_short` | Short twilight word: astronomical. |
| `share_text_moon` | Payload line: Moon fraction, state, times (`%1$s`–`%3$s`). |
| `share_text_moon_up_all_night` | Payload Moon state: up all night. |
| `share_text_moon_below_all_night` | Payload Moon state: below the horizon all night. |
| `share_text_moon_rises` | Payload Moon line: rises at (`%1$s`). |
| `share_text_moon_sets` | Payload Moon line: sets at (`%1$s`). |
| `share_text_moon_lowest` | Payload Moon line: lowest altitude (`%1$s`,`%2$s`). |
| `share_text_targets_header` | Payload header: targets. |
| `share_text_no_target` | Payload: no target is chosen. |
| `share_text_target_window` | Payload target line with its window and peak (`%1$s`–`%5$s`). |
| `share_text_target_never_rises` | Payload target line: never rises (`%1$s`,`%2$s`). |
| `share_text_target_circumpolar` | Payload target line: up all night (`%1$s`–`%4$s`). |
| `share_text_target_partial` | Payload target line: clipped by the dark window (`%1$s`–`%5$s`). |
| `share_text_passes_header` | Payload header: satellite passes. |
| `share_text_no_pass` | Payload: no observable pass. |
| `share_text_pass` | Payload pass line (`%1$s`–`%5$s`). |
| `share_text_pass_visible` | Payload pass visibility word: visible. |
| `share_text_pass_shadow` | Payload pass note: occurs but not visible, in Earth's shadow. |
| `share_text_pass_bright` | Payload pass note: occurs but not visible, sky too bright. |
| `share_text_cloud_header` | Payload header: cloud outlook (forecast, not a measurement). |
| `share_text_cloud_source` | Payload cloud source line (`%1$s`–`%3$s`). |
| `share_text_cloud_hour` | Payload cloud hour line (`%1$s`,`%2$s`). |
| `share_text_cloud_mean` | Payload mean cloud line (`%1$s`%%). |
| `share_text_no_forecast` | Payload: no forecast available. |
| `share_text_no_forecast_last` | Payload: no forecast, with the last reading (`%1$s`,`%2$s`). |
| `share_text_lens_header` | Payload header: lens. |
| `share_text_lens` | Payload lens line (`%1$s`–`%5$s`). |
| `share_text_and_more` | Payload truncation note (`%1$d` more). |
| `share_text_credit` | Payload credit line: MET Norway and ESO/S. Brunier, both CC BY 4.0. |
| `share_phase_new_moon` | Moon phase name: new moon. |
| `share_phase_waxing_crescent` | Moon phase name: waxing crescent. |
| `share_phase_first_quarter` | Moon phase name: first quarter. |
| `share_phase_waxing_gibbous` | Moon phase name: waxing gibbous. |
| `share_phase_full_moon` | Moon phase name: full moon. |
| `share_phase_waning_gibbous` | Moon phase name: waning gibbous. |
| `share_phase_last_quarter` | Moon phase name: last quarter. |
| `share_phase_waning_crescent` | Moon phase name: waning crescent. |

## Widget — the night-glance surfaces

Rendered by `ui/widget/NightGlanceWidget.kt`. Every line is resolved from this table, never
concatenated, and the cloud line's **age** is never dropped — a percentage without the age of its
reading reads as current.

| Key | Purpose |
|---|---|
| `widget_title` | Widget title: Tonight. |
| `widget_dark_window_line` | Dark-window line with its kind (`%1$s`–`%3$s`). |
| `widget_dark_window_second` | Second dark-window line (`%1$s`,`%2$s`). |
| `widget_no_darkness` | States there is no darkness tonight. |
| `widget_cloud_line` | Cloud line: percent, forecast marker, age (`%1$s`,`%2$s`). |
| `widget_cloud_none` | Cloud state: no forecast. |
| `widget_stale_badge` | Badge: stale. |
| `widget_not_ready` | Invitation to open the app for tonight's dark window. |
| `widget_aged` | States the last update time (`%1$s`). |
| `widget_content_description` | The one TalkBack name for the whole tile (`%1$s`). |
| `widget_moon_up_all_night` | Moon state: up all night. |
| `widget_moon_down_all_night` | Moon state: below the horizon all night. |
| `widget_moon_rises` | Moon state: rises during the night. |
| `widget_moon_sets` | Moon state: sets during the night. |
| `widget_moon_unknown` | Moon state: up/down unknown. |
| `widget_open_app` | Action text: open Lumintra. |
| `widget_not_ready_other_night` | Invitation when the snapshot is for another night. |
| `widget_no_spot` | Invitation when no spot is saved. |
| `widget_no_dark_polar_day` | Honest line: the Sun does not set tonight. |
| `widget_no_dark_polar_night` | Honest line: the Sun does not rise tonight. |
| `widget_no_dark_no_astro` | Honest line: no astronomical darkness tonight. |
| `widget_moon_line` | Moon fraction with its typed form (`%1$d`%%, `%2$s`). |
| `widget_cloud_age_now` | Cloud age: just now. |
| `widget_cloud_age_minutes` | Cloud age in minutes (`%1$d` min). |
| `widget_cloud_age_hours` | Cloud age in hours (`%1$d` h). |
| `widget_a11y` | Composite TalkBack sentence (`%1$s`, `%2$s`). |
| `widget_a11y_not_ready` | TalkBack sentence when the plan is not ready. |
| `widget_a11y_no_spot` | TalkBack sentence when no spot is saved. |
| `widget_preview_spot` | Placeholder spot name shown in the widget preview. |

## Reminder — the one local notification

Rendered by `notify/DarkWindowReminder.kt`. None of these lines is an instruction and none promises
clear skies; the offline line keeps an absent forecast from reading as a fresh one.

| Key | Purpose |
|---|---|
| `reminder_title` | Notification title naming the dark window's start (`%1$s`). |
| `reminder_body` | Body stating the spot and both bounds (`%1$s`–`%3$s`). |
| `reminder_body_forecast` | Body adding the forecast cloud as of retrieval (`%1$s`–`%5$s`). |
| `reminder_channel_name` | Notification channel name. |
| `reminder_channel_description` | Notification channel description. |
| `reminder_explainer` | Pre-permission explainer for the one reminder. |
| `reminder_denied` | States notifications are off and nothing else is affected. |
| `reminder_open_settings` | Action: open the system notification settings. |
| `reminder_body_spot` | Body line: the spot (`%1$s`). |
| `reminder_body_window` | Body line: the dark window (`%1$s`–`%3$s`). |
| `reminder_body_moon` | Body line: Moon fraction with its typed form (`%1$d`%%, `%2$s`). |
| `reminder_body_cloud` | Body line: cloud as of retrieval (`%1$d`%%, `%2$s`). |
| `reminder_body_no_cloud` | Body line: no cached forecast for this spot. |
| `reminder_body_offline` | Body line: planned offline, so the forecast could not refresh. |

## Settings

Every row changes observable behaviour.

| Key | Purpose |
|---|---|
| `settings_title` | Screen title: Settings. |
| `settings_back` | Back action on the Settings surface. |
| `settings_appearance_header` | Header for the appearance group. |
| `settings_theme_supporting` | Supporting text for the theme choice. |
| `settings_theme_system` | Theme value: follow system. |
| `settings_theme_light` | Theme value: light. |
| `settings_theme_dark` | Theme value: dark. |
| `settings_dynamic_color_title` | Row title: use wallpaper colours. |
| `settings_dynamic_color_supported` | Explains dynamic colour when supported. |
| `settings_dynamic_color_unsupported` | States dynamic colour is unavailable. |
| `settings_red_mode_title` | Row title: red night mode. |
| `settings_red_mode_summary` | States what red night mode holds (blue 0, green ≤ 8). |
| `settings_language_header` | Header for the language group. |
| `settings_language_title` | Row title: app language. |
| `settings_language_summary` | Supporting text for the language row. |
| `settings_language_system` | Language value: system default. |
| `settings_language_system_hint` | Explains the app follows the platform's per-app language picker. |
| `settings_units_header` | Header for the units and formats group. |
| `settings_units_title` | Row title: units. |
| `settings_units_summary` | States what units affect and that mm stays mm. |
| `settings_units_metric` | Units value: metric. |
| `settings_units_imperial` | Units value: imperial. |
| `settings_time_format_title` | Row title: time format. |
| `settings_time_format_summary` | States where the time format applies. |
| `settings_time_format_24` | Time-format value: 24-hour. |
| `settings_time_format_12` | Time-format value: 12-hour. |
| `settings_coordinate_format_title` | Row title: coordinate format. |
| `settings_coordinate_format_summary` | Explains either form can be entered; the stored coordinate is unchanged. |
| `settings_coordinate_format_decimal` | Coordinate-format value: decimal degrees. |
| `settings_coordinate_format_dms` | Coordinate-format value: degrees, minutes, seconds. |
| `settings_data_header` | Header for the Tonight group. |
| `settings_reminder_title` | Row title: remind me tonight. |
| `settings_reminder_summary` | States permission is requested on enable, never at launch. |
| `settings_cloud_title` | Row title: cloud forecast. |
| `settings_cloud_summary` | States requests go to api.met.no and that off means no request. |
| `settings_lens_preset_title` | Row title: default lens profile. |
| `settings_lens_preset_summary` | States the preset the frame opens and fits against. |
| `settings_sky_brightness_title` | Row title: sky brightness estimate. |
| `settings_sky_brightness_summary` | States the reading is a user estimate and never changes the computed value. |
| `settings_sky_brightness_none` | Sky-brightness value: none. |
| `settings_sky_brightness_sqm` | Sky-brightness value: SQM. |
| `settings_sky_brightness_bortle` | Sky-brightness value: Bortle class. |
| `settings_sky_brightness_sqm_value` | Field label: SQM reading. |
| `settings_sky_brightness_bortle_value` | Field label: Bortle class. |
| `settings_behaviour_header` | Header for the behaviour group. |
| `settings_haptics_title` | Row title: vibration feedback. |
| `settings_haptics_summary` | States when the app vibrates. |
| `settings_about_header` | Header for the about group. |
| `settings_rate_title` | Row title: Rate Lumintra. |
| `settings_rate_summary` | Supporting text for the rate row. |
| `settings_rate_unavailable` | Disabled state: neither the Store nor a browser is reachable. |
| `settings_credits_title` | Row title: credits and licences. |
| `settings_credits_summary` | Supporting text for the credits row. |

## Credits — every licence and attribution

The attribution lines, the licence URLs, the version, the support address and the privacy-policy
address are `[NT]`; the section headers and the modification statements are translated around them.

| Key | Purpose |
|---|---|
| `credits_title` | Screen title: Credits. |
| `credits_back` | Back action on the Credits surface. |
| `credits_section_data` | Section header: data and imagery. |
| `credits_section_code` | Section header: libraries. |
| `credits_section_modifications` | Section header: modifications. |
| `credits_section_links` | Section header: licence text. |
| `credit_met_norway` `[NT]` | MET Norway attribution, `Data from MET Norway (met.no) — CC BY 4.0`. |
| `credit_eso_credit` `[NT]` | The ESO panorama credit, `ESO/S. Brunier`. |
| `credit_eso_what` | Wraps the ESO credit with the panorama id (`%1$s`). |
| `credit_nasa_svs` `[NT]` | NASA SVS Moon Kit acknowledgement. |
| `credit_iau_constellations` `[NT]` | IAU / Sky & Telescope constellation-figure credit with authors and CC BY 4.0. |
| `credit_fk6` `[NT]` | FK6 star-position credit with GAVO and CC0 1.0. |
| `credit_fk6_short` `[NT]` | Short FK6 form, `FK6 (CC0 1.0)`. |
| `credit_play_review` `[NT]` | In-app review library notice (Play Core SDK Terms of Service). |
| `credit_orekit` `[NT]` | Orekit + Hipparchus Apache-2.0 notice. |
| `credit_astronomy_kt` `[NT]` | astronomy.kt Astronomy Engine (Kotlin) MIT notice. |
| `credit_met_norway_source` `[NT]` | Weather-forecast source line naming api.met.no and MET Norway. |
| `credit_iau_source` `[NT]` | Short constellation-figure source line naming IAU and Sky & Telescope. |
| `credit_open_cc_by` | Link label: licence text (CC BY 4.0). |
| `credit_open_met` | Link label: met.no. |
| `credit_open_api_met` | Link label: api.met.no. |
| `credit_open_eso` | Link label: ESO panorama eso0932a. |
| `credit_open_iau` | Link label: IAU. |
| `credit_link_cc_by` `[NT]` | CC BY 4.0 licence URL. |
| `credit_link_met` `[NT]` | www.met.no URL. |
| `credit_link_api_met` `[NT]` | api.met.no URL. |
| `credit_link_eso` `[NT]` | ESO panorama eso0932a URL. |
| `credit_link_iau` `[NT]` | IAU URL. |
| `credit_link_cc0` `[NT]` | CC0 1.0 licence URL. |
| `credit_mod_met_norway` | Modification statement: coordinates truncated to 4 decimals before sending. |
| `credit_mod_imagery` | Modification statement: backdrop and postcard composed through the app's palette. |
| `credits_about_header` | Header for the about-this-app group. |
| `credits_about_version` | Version line (`%1$s`,`%2$s`). |
| `credits_about_contact` | Support line (`%1$s`). |
| `credits_about_no_account` | States there is no account and no personal data is collected. |
| `credits_about_forecast` | States when a coordinate is sent and how it is truncated. |
| `credits_about_elements` | States elements come from CelesTrak and carry no coordinates. |
| `credits_about_no_ads` | States the app has no ads and no subscription. |
| `credits_about_no_location` | States the app requests no location permission and never uses the compass. |
| `credits_privacy` | Action/label: privacy policy. |
| `credits_privacy_unpublished` | States the policy address is unpublished. |
| `credits_privacy_failed` | States no browser could open the address (`%1$s`). |
| `credits_version_name` `[NT]` | The app's version name value. |
| `credits_version_code` `[NT]` | The app's version code value. |
| `credits_contact_email` `[NT]` | The public support address. |
| `privacy_policy_url` `[NT]` | The in-app privacy-policy address; a marked placeholder until `scripts/publish-privacy.mjs` records the real one. |

## Scaffold-legacy keys

Retained only because `ui/home/` and `ui/about/` still render them; removed with those two screens.
They are not part of the product's copy.

| Key | Purpose |
|---|---|
| `home_about_action` | Legacy Home action: About. |
| `home_intro` | Legacy Home intro sentence. |
| `home_idea_label` | Legacy Home label: the idea. |
| `home_pick_section` | Legacy Home prompt to open a section. |
| `home_primary_action` | Legacy Home starter-action button. |
| `home_primary_action_again` | Legacy Home repeat-action button. |
| `home_status_ready` | Legacy Home ready state. |
| `home_status_complete` | Legacy Home complete state. |
| `home_snackbar_completed` | Legacy Home completion snackbar. |
| `home_snackbar_already_completed` | Legacy Home already-complete snackbar. |
| `about_title` | Legacy About title. |
| `about_back` | Legacy About back action. |
| `about_body` | Legacy About body, naming the app (`%1$s`). |
| `about_privacy_note` | Legacy About privacy note (no location permission; records stay on device). |

## Platform strings — `strings_platform.xml`

Read by the manifest and `res/xml/night_glance_widget_info.xml`; mirrored in `values-ru/`.

| Key | Purpose |
|---|---|
| `widget_label` | The widget's name in the launcher's widget picker. |
| `widget_description` | The widget's one-line description in the widget picker. |

---

## Invariants this index relies on

- `values/` carries **595** entries (590 strings + 5 plurals); **23** are `[NT]`.
- `values/` and `values-ru/` each carry the **same 572 translatable entries** (567 strings +
  5 plurals), with identical names, identical format arguments in identical order, and no
  sentinel text.
- A `[NT]` key appears **only** in `values/strings.xml`.
- The word `Yr` appears in neither file; no Solar System Scope credit appears; no
  `en-XA`/`ar-XB` pseudolocale exists anywhere.
