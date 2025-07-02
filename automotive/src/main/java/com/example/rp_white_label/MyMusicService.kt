package com.example.rp_white_label

import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.media.MediaBrowserServiceCompat
import androidx.media.utils.MediaConstants
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET


/**
 * This class provides a MediaBrowser through a service. It exposes the media library to a browsing
 * client, through the onGetRoot and onLoadChildren methods. It also creates a MediaSession and
 * exposes it through its MediaSession.Token, which allows the client to create a MediaController
 * that connects to and send control commands to the MediaSession remotely. This is useful for user
 * interfaces that need to interact with your media session, like Android Auto. You can (should)
 * also use the same service from your app's UI, which gives a seamless playback experience to the
 * user.
 *
 * To implement a MediaBrowserService, you need to:
 *
 * * Extend [MediaBrowserServiceCompat], implementing the media browsing related methods
 * [MediaBrowserServiceCompat.onGetRoot] and [MediaBrowserServiceCompat.onLoadChildren];
 *
 * * In onCreate, start a new [MediaSessionCompat] and notify its parent with the session"s token
 * [MediaBrowserServiceCompat.setSessionToken];
 *
 * * Set a callback on the [MediaSessionCompat.setCallback]. The callback will receive all the
 * user"s actions, like play, pause, etc;
 *
 * * Handle all the actual music playing using any method your app prefers (for example,
 * [android.media.MediaPlayer])
 *
 * * Update playbackState, "now playing" metadata and queue, using MediaSession proper methods
 * [MediaSessionCompat.setPlaybackState] [MediaSessionCompat.setMetadata] and
 * [MediaSessionCompat.setQueue])
 *
 * * Declare and export the service in AndroidManifest with an intent receiver for the action
 * android.media.browse.MediaBrowserService
 *
 * To make your app compatible with Android Auto, you also need to:
 *
 * * Declare a meta-data tag in AndroidManifest.xml linking to a xml resource with a
 * &lt;automotiveApp&gt; root element. For a media app, this must include an &lt;uses
 * name="media"/&gt; element as a child. For example, in AndroidManifest.xml: &lt;meta-data
 * android:name="com.google.android.gms.car.application"
 * android:resource="@xml/automotive_app_desc"/&gt; And in res/values/automotive_app_desc.xml:
 * &lt;automotiveApp&gt; &lt;uses name="media"/&gt; &lt;/automotiveApp&gt;
 */
interface MediaItemProvider {
    suspend fun getMediaItems(): List<MediaItem>
}

data class RemoteConfig(
    val isHomeEnabled: Boolean,
    val isStationsEnabled: Boolean,
    val isPodcastEnabled: Boolean,
    val isFavouriteEnabled: Boolean,
    val isSearchSupported: Boolean
)

// Retrofit and networking for Remote Config
interface RemoteConfigApi {
    @GET("config")
    suspend fun getRemoteConfig(): RemoteConfigDto
}

data class RemoteConfigDto(
    @SerializedName("home") val home: FeatureConfigDto,
    @SerializedName("stations") val stations: FeatureConfigDto,
    @SerializedName("podcast") val podcast: FeatureConfigDto,
    @SerializedName("favourite") val favourite: FeatureConfigDto,
    @SerializedName("search") val search: FeatureConfigDto,
    @SerializedName("enabled") val enabled: Boolean
) {
    fun toDomain(): RemoteConfig {
        return RemoteConfig(
            isHomeEnabled = home.enabled,
            isStationsEnabled = stations.enabled,
            isPodcastEnabled = podcast.enabled,
            isFavouriteEnabled = favourite.enabled,
            isSearchSupported = search.enabled
        )
    }
}

data class FeatureConfigDto(
    @SerializedName("enabled") val enabled: Boolean
)

object RetrofitClient {
    private const val BASE_URL = " http://10.0.2.2:3000/"

    val api: RemoteConfigApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RemoteConfigApi::class.java)
    }
}

object RemoteConfigProvider {

    private val defaultConfig = RemoteConfig(
        isHomeEnabled = true,
        isStationsEnabled = true,
        isPodcastEnabled = true,
        isFavouriteEnabled = true,
        isSearchSupported = true
    )

    suspend fun get(): RemoteConfig {
    // @TODO Uncomment the following lines to retrieve remote config from the server
        return try {
            RetrofitClient.api.getRemoteConfig().toDomain()
        } catch (e: Exception) {
            Log.e("RemoteConfigProvider", "Error fetching remote config", e)
            defaultConfig
        }
    }
}

class MyMusicService : MediaBrowserServiceCompat() {

    private lateinit var session: MediaSessionCompat
    private var config: RemoteConfig = RemoteConfig(
        isHomeEnabled = true,
        isStationsEnabled = true,
        isPodcastEnabled = true,
        isFavouriteEnabled = true,
        isSearchSupported = true
    )
    private val homeContentProvider = HomeContentProvider()
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    companion object {
        private const val BROWSER_ROOT_ID = "root"
    }

    private val menuItems =
        listOf(
            MenuItem("home", "Home", true, "", MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM),
            MenuItem("stations", "Stations", true),
            MenuItem("podcast", "Podcast", true),
            MenuItem("favourite", "Favourite", true)

        )

    private val stationsContent =
        listOf(
            MenuItem("station_classic_rock", "Classic Rock", true),
            MenuItem("station_80s", "80s Hits", true),
            MenuItem("station_jazz", "Jazz FM", true)
        )

    private val podcastContent =
        listOf(
            MenuItem("podcast_tech", "Tech Weekly", false, "One More Time",MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM),
            MenuItem("podcast_comedy", "Comedy Now", false, "One More Time", MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM)
        )

    private val favouriteContent = listOf<MenuItem>(
        MenuItem("favourite_tech", "Favourite Tech Media", true, "Tech", MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM),
        MenuItem("favourite_hobby", "Hobby  Media", true, "Tech", MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM),
        MenuItem("favourite_music", "Music  Media", true, "Tech", MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM)
    )

    data class MenuItem(val mediaId: String, val title: String, val browsable: Boolean, val groupTitle: String = "", val style: Int = MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM, val image: Uri = Uri.EMPTY)

    private val callback =
        object : MediaSessionCompat.Callback() {
            override fun onPlay() {}

            override fun onSkipToQueueItem(queueId: Long) {}

            override fun onSeekTo(position: Long) {}

            override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {}

            override fun onPause() {}

            override fun onStop() {}

            override fun onSkipToNext() {}

            override fun onSkipToPrevious() {}

            override fun onCustomAction(action: String?, extras: Bundle?) {}

            override fun onPlayFromSearch(query: String?, extras: Bundle?) {}
        }

    override fun onCreate() {
        super.onCreate()

        session = MediaSessionCompat(this, "MyMusicService")
        sessionToken = session.sessionToken
        session.setCallback(callback)
        session.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        serviceScope.launch {
            config = RemoteConfigProvider.get()
            Log.d("onCreate", "config: ${config}")
            notifyChildrenChanged(BROWSER_ROOT_ID)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        session.release()
        serviceJob.cancel()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): MediaBrowserServiceCompat.BrowserRoot? {
        Log.d("onGetRoot", "config: ${config}")
        val extras =
            Bundle().apply {
                //Enable or disable Search
                putBoolean(MediaConstants.BROWSER_SERVICE_EXTRAS_KEY_SEARCH_SUPPORTED, config.isSearchSupported)

                //Style PLAYABLE and BROWSABLE MediaItems using /** STYLE_GRID_ITEM **/
                putInt(MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_PLAYABLE, MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM)
                putInt(MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM)

            }
        return MediaBrowserServiceCompat.BrowserRoot(BROWSER_ROOT_ID, extras)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaItem>>) {

        val children: List<MenuItem> =
            when (parentId) {
                "root" -> {
                    menuItems.filter { menuItem ->
                        when (menuItem.mediaId) {
                            "home" -> config.isHomeEnabled
                            "stations" -> config.isStationsEnabled
                            "podcast" -> config.isPodcastEnabled
                            "favourite" -> config.isFavouriteEnabled
                            else -> false
                        }
                    }
                }
                "home" -> {
                    val mediaItems = mutableListOf<MediaItem>()
                    val carousels = homeContentProvider.getHomeCarousels()
                    carousels.forEach { carousel ->
                        carousel.items.forEach { playableItem ->
                            val imageUri = if (playableItem.image.isNotEmpty()) Uri.parse(playableItem.image) else Uri.EMPTY
                            Log.d("onLoadChildren", "imageUri: ${imageUri}")
                            if(playableItem.browsable)
                                mediaItems.add(createBrowsableItem(playableItem.mediaId,playableItem.title,MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,"TEST",imageUri))
                            else
                                mediaItems.add(createPlayableItem(playableItem.mediaId, playableItem.title, playableItem.subtitle, carousel.title))
                        }
                    }
                    result.sendResult(mediaItems)
                    return
                }
                "stations" -> stationsContent
                "podcast" -> podcastContent
                "favourite" -> favouriteContent
                else -> emptyList()
            }

        val mediaItems =
            children
                .map { child ->
                    if (child.browsable)
                        createBrowsableItem(child.mediaId, child.title,child.style, child.groupTitle,child.image )
                    else
                        createPlayableItem(child.mediaId, child.title,"")
                }
                .toMutableList()

        result.sendResult(mediaItems)
    }

    private fun createBrowsableItem(
        mediaId: String,
        title: String,
        style: Int = MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
        groupTitle: String  = "",
        image: Uri = Uri.EMPTY
    ): MediaBrowserCompat.MediaItem {
        val extras = Bundle()
        // extras.putInt(BROWSABLE_HINT, style)
        extras.putString(MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE,groupTitle)
        extras.putInt(MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, style)
        val desc =
            MediaDescriptionCompat.Builder()
                .setMediaId(mediaId)
                .setTitle(title)
                .setMediaUri(image)
                .setIconUri(image)
                .build()

        return MediaBrowserCompat.MediaItem(desc, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
    }

    private fun createPlayableItem(mediaId: String, title: String, subtitle: String, groupTitle: String? = null): MediaBrowserCompat.MediaItem {
        val extras = Bundle()
        groupTitle?.let {
            extras.putString(
                MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE,
                "TEST")
            extras.putDouble(
                MediaConstants.DESCRIPTION_EXTRAS_KEY_COMPLETION_PERCENTAGE,0.9)

        }
        val desc = MediaDescriptionCompat.Builder()
            .setMediaId(mediaId)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setExtras(extras)
            .build()

        return MediaBrowserCompat.MediaItem(desc, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
    }
}