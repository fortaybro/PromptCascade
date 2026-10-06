package com.forrest.titanlauncher.calendar

import android.content.Context

class CalendarSelectionStore(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            "calendar_routing_preferences",
            Context.MODE_PRIVATE
        )

    fun saveSelections(
        personalCalendarId: String,
        personalCalendarName: String,
        familyCalendarId: String,
        familyCalendarName: String
    ) {

        preferences
            .edit()
            .putString(
                KEY_PERSONAL_ID,
                personalCalendarId
            )
            .putString(
                KEY_PERSONAL_NAME,
                personalCalendarName
            )
            .putString(
                KEY_FAMILY_ID,
                familyCalendarId
            )
            .putString(
                KEY_FAMILY_NAME,
                familyCalendarName
            )
            .apply()
    }

    fun getPersonalCalendarId():
            String? {

        return preferences
            .getString(
                KEY_PERSONAL_ID,
                null
            )
            ?.takeIf {
                it.isNotBlank()
            }
    }

    fun getFamilyCalendarId():
            String? {

        return preferences
            .getString(
                KEY_FAMILY_ID,
                null
            )
            ?.takeIf {
                it.isNotBlank()
            }
    }

    fun getPersonalCalendarName():
            String? {

        return preferences
            .getString(
                KEY_PERSONAL_NAME,
                null
            )
    }

    fun getFamilyCalendarName():
            String? {

        return preferences
            .getString(
                KEY_FAMILY_NAME,
                null
            )
    }

    fun isConfigured():
            Boolean {

        return !getPersonalCalendarId()
            .isNullOrBlank() &&
                !getFamilyCalendarId()
                    .isNullOrBlank()
    }

    /*
     * Where events come from and go to: the phone's own calendars
     * (no Google sign-in) or a Google account. Before anything is
     * chosen: Google only if a Google calendar was already picked
     * (people upgrading), otherwise the phone.
     */
    fun usesGoogle(): Boolean {

        preferences
            .getString(KEY_SOURCE, null)
            ?.let { return it == SOURCE_GOOGLE }

        val picked =
            getPersonalCalendarId()

        return picked != null &&
                !DeviceCalendarRepository.isDeviceId(picked)
    }

    fun setUsesGoogle(
        google: Boolean
    ) {
        preferences
            .edit()
            .putString(
                KEY_SOURCE,
                if (google) SOURCE_GOOGLE else SOURCE_PHONE
            )
            .apply()
    }

    fun clear() {

        preferences
            .edit()
            .clear()
            .apply()
    }

    companion object {

        private const val KEY_PERSONAL_ID =
            "personal_calendar_id"

        private const val KEY_PERSONAL_NAME =
            "personal_calendar_name"

        private const val KEY_FAMILY_ID =
            "family_calendar_id"

        private const val KEY_FAMILY_NAME =
            "family_calendar_name"

        private const val KEY_SOURCE =
            "calendar_source"

        private const val SOURCE_GOOGLE =
            "google"

        private const val SOURCE_PHONE =
            "phone"
    }
}