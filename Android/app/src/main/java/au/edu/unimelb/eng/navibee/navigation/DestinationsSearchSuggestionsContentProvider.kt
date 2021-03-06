package au.edu.unimelb.eng.navibee.navigation

import android.Manifest
import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Intent
import android.content.SearchRecentSuggestionsProvider
import android.content.UriMatcher
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.BaseColumns
import androidx.core.content.ContextCompat
import au.edu.unimelb.eng.navibee.NaviBeeApplication
import au.edu.unimelb.eng.navibee.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.places.AutocompleteFilter
import com.google.android.gms.location.places.GeoDataClient
import com.google.android.gms.location.places.Places
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.tasks.Tasks


class DestinationsSearchSuggestionsContentProvider: SearchRecentSuggestionsProvider() {

    @SuppressLint("MissingPermission")
    companion object {
        // Match ID for URI Matcher
        private const val SEARCH_SUGGESTIONS = 0

        const val AUTHORITY = "au.edu.unimelb.eng.navibee.navigation.DestinationsSearchSuggestionsContentProvider"
        const val MODE = DATABASE_MODE_QUERIES

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "", SEARCH_SUGGESTIONS)
        }

        private val mGeoDataClient: GeoDataClient by lazy {
            Places.getGeoDataClient(NaviBeeApplication.instance)
        }

        private val fusedLocationClient: FusedLocationProviderClient by lazy {
            LocationServices.getFusedLocationProviderClient(NaviBeeApplication.instance)
        }

        private const val LAT_LNG_BOUND_RADIUS = 0.009 * 10
    }

    init {
        setupSuggestions(AUTHORITY, MODE)
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
                       selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
//        return super.query(uri, projection, selection, selectionArgs, sortOrder)

        val cursor = MatrixCursor(arrayOf(
                BaseColumns._ID,
                SearchManager.SUGGEST_COLUMN_TEXT_1,
                SearchManager.SUGGEST_COLUMN_TEXT_2,
                SearchManager.SUGGEST_COLUMN_ICON_1,
                SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA,
                SearchManager.SUGGEST_COLUMN_QUERY,
                SearchManager.SUGGEST_COLUMN_INTENT_ACTION))

        val query = selectionArgs?.get(0)

        val suggestionCursor = super.query(uri, projection, selection, selectionArgs, sortOrder)


        suggestionCursor?.run {
            if (moveToFirst()) {
                do {
                    cursor.newRow()
                        .add(BaseColumns._ID,
                            "-" + getString(getColumnIndex(BaseColumns._ID)))
                        .add(SearchManager.SUGGEST_COLUMN_TEXT_1, getString(getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1)))
                        .add(SearchManager.SUGGEST_COLUMN_INTENT_ACTION, Intent.ACTION_SEARCH)
                        .add(SearchManager.SUGGEST_COLUMN_QUERY, getString(getColumnIndex(SearchManager.SUGGEST_COLUMN_QUERY)))
                        .add(SearchManager.SUGGEST_COLUMN_ICON_1, R.drawable.ic_history_black_24dp)
                } while (moveToNext())
            }
            close()
        }

        if (ContextCompat.checkSelfPermission(NaviBeeApplication.instance,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return cursor
        }


        val loc = fusedLocationClient.lastLocation.apply {
            Tasks.await(this)
        }.result ?: return cursor


        val topLeft = LatLng(loc.latitude - LAT_LNG_BOUND_RADIUS,
                loc.longitude - LAT_LNG_BOUND_RADIUS)
        val bottomRight = LatLng(loc.latitude + LAT_LNG_BOUND_RADIUS,
                loc.longitude + LAT_LNG_BOUND_RADIUS)

        val results = mGeoDataClient.getAutocompletePredictions(
                query,
                LatLngBounds(topLeft, bottomRight),
                AutocompleteFilter.Builder().setTypeFilter(AutocompleteFilter.TYPE_FILTER_NONE).build()
        )

        val suggestions = results.apply {
            Tasks.await(this)
        }.result ?: return cursor

        for ((i, suggestion) in suggestions.withIndex()) {
            cursor.newRow()
                    .add(BaseColumns._ID, "$i")
                    .add(SearchManager.SUGGEST_COLUMN_TEXT_1, suggestion.getPrimaryText(null))
                    .add(SearchManager.SUGGEST_COLUMN_TEXT_2, suggestion.getSecondaryText(null))
                    .add(SearchManager.SUGGEST_COLUMN_ICON_1, R.drawable.ic_search_black_24dp)
                    .add(SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA, suggestion.placeId)
                    .add(SearchManager.SUGGEST_COLUMN_INTENT_ACTION, Intent.ACTION_VIEW)
        }

        return cursor
    }
}