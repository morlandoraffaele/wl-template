package com.example.rp_white_label

import android.util.Log

data class PlayableItem(
    val mediaId: String,
    val title: String,
    val subtitle: String,
    val browsable: Boolean = false,
    val image: String = "",
    val mediaUri: String = ""
)

data class Carousel(
    val id: String,
    val title: String,
    val items: List<PlayableItem>
)

class HomeContentProvider {
    fun getHomeCarousels(config: RemoteConfig): List<Carousel> {
        return createHomeContent(config)
    }

    fun getCarouselItems(carouselId: String, config: RemoteConfig): List<PlayableItem>? {
        return createHomeContent(config).find { it.id == carouselId }?.items
    }

    fun getPlayableItem(mediaId: String, config: RemoteConfig): PlayableItem? {
        return createHomeContent(config).flatMap { it.items }.find { it.mediaId == mediaId }
    }

    private fun createHomeContent(config: RemoteConfig): List<Carousel> {
        val allCarousels = mutableListOf<Carousel>()

        if (config.isHomeEnabled) {
            allCarousels.add(Carousel(
                id = "home",
                title = "Home",
                items = listOf(
                    PlayableItem("recent_1", "Gilded Pleasure", "The Growlers"),
                    PlayableItem("recent_2", "True Affection", "The Blow"),
                    PlayableItem("recent_3", "Live From Electric Lady", "Julian Casablancas"),
                )
            ))
        }

        if(config.isStationsEnabled) {
            allCarousels.add(Carousel(
                id = "stations",
                title = "Stations",
                items = listOf(
                    PlayableItem(
                        mediaId = "station_47_fm",
                        title = "STATION 1 - WORKING",
                        subtitle = "It's rock baby!",
                        mediaUri = "https://4c4b867c89244861ac216426883d1ad0.msvdn.net/radiocapital/radiocapital/master_ma.m3u8?rp_source=1"
                    )
                )
            ))
        }

        if (config.isPodcastEnabled) {
            allCarousels.add(Carousel(
                id = "podcast",
                title = "Podcast",
                items = listOf(
                    PlayableItem("podcast_tech", "Tech Weekly", "One More Time"),
                    PlayableItem("podcast_comedy", "Comedy Now", "One More Time")
                )
            ))
        }

        if (config.isFavouriteEnabled) {
            allCarousels.add(Carousel(
                id = "favourite",
                title = "Favourite",
                items = listOf(
                    PlayableItem("favourite_tech", "Favourite Tech Media", "Tech", browsable = true),
                    PlayableItem("favourite_hobby", "Hobby  Media", "Tech", browsable = true),
                    PlayableItem("favourite_music", "Music  Media", "Tech", browsable = true)
                )
            ))
        }
        return allCarousels.filter { it.items.isNotEmpty() }
    }
}