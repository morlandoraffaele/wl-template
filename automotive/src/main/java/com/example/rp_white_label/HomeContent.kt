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
                    PlayableItem(
                        "204",
                        "Radio Deejay",
                        "One Nation One Station",
                        image = "https://assets.radioplayer.org/380/380204/128/128/kyd7f7ow.png",
                        mediaUri = "https://nmcdn-4c4b867c89244861ac216426883d1ad0.msvdn.net/icecastRelay/S46519745/BKrF6NPEd0z1/icecast?rp_source=1"),
                    PlayableItem("favourite_tech", "Favourite Tech Media", "Tech", browsable = true),
                )
            ))
        }

        if(config.isStationsEnabled) {
            allCarousels.add(Carousel(
                id = "stations",
                title = "Stations",
                items = listOf(
                    PlayableItem(
                        "204",
                        "Radio Deejay",
                        "One Nation One Station",
                        image = "https://assets.radioplayer.org/380/380204/128/128/kyd7f7ow.png",
                        browsable = false,
                        mediaUri = "https://nmcdn-4c4b867c89244861ac216426883d1ad0.msvdn.net/icecastRelay/S46519745/BKrF6NPEd0z1/icecast?rp_source=1"),
                    PlayableItem(
                        "224",
                        "Radiofreccia",
                        "Libera come noi",
                        image = "",
                        browsable = false,
                        mediaUri = "https://streamingp.shoutcast.com/radiofreccia_48.aac?rp_source=1"),
                    PlayableItem(
                        "230",
                        "Radio Ibiza",
                        "Dance Station",
                        image = "",
                        browsable = false,
                        mediaUri = "https://ice02.fluidstream.net/ibiza.aac?rp_source=1"),
                )
            ))
        }

        if (config.isPodcastEnabled) {
            allCarousels.add(Carousel(
                id = "podcast",
                title = "Podcast",
                items = listOf(
                    PlayableItem("podcast_tech", "Tech Weekly", "One More Time",browsable = true),
                    PlayableItem("podcast_comedy", "Comedy Now", "It's fun!",browsable = true)
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