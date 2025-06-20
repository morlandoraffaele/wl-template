package com.example.rp_white_label

data class PlayableItem(
    val mediaId: String,
    val title: String,
    val subtitle: String,
    val browsable: Boolean = false,
    val image: String = ""
)

data class Carousel(
    val id: String,
    val title: String,
    val items: List<PlayableItem>
)

class HomeContentProvider {
    private val carousels: List<Carousel> by lazy { createHomeContent() }

    fun getHomeCarousels(): List<Carousel> {
        return carousels
    }

    fun getCarouselItems(carouselId: String): List<PlayableItem>? {
        return carousels.find { it.id == carouselId }?.items
    }

    private fun createHomeContent(): List<Carousel> {
        return listOf(
            Carousel(
                id = "home_recent",
                title = "Recent",
                items = listOf(
                    PlayableItem("recent_1", "Gilded Pleasure", "The Growlers"),
                    PlayableItem("recent_2", "True Affection", "The Blow"),
                    PlayableItem("recent_3", "Live From Electric Lady", "Julian Casablancas"),
                )
            ),
            Carousel(
                id = "home_latest",
                title = "Latest",
                items = listOf(
                    PlayableItem("latest_1", "Perfect Left", "The Growlers"),
                    PlayableItem("latest_2", "Lost My Mind", "Froth"),
                    PlayableItem("latest_3", "More Recommended", "",true, "android.resource://com.example.rp_white_label/drawable/more"),
                )
            )
        )
            .filter { it.items.isNotEmpty() }
    }
}