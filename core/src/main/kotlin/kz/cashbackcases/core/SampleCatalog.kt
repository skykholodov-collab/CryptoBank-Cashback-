package kz.cashbackcases.core

object SampleCatalog {
    val collections: List<OfferCollection> = listOf(
        collection(
            id = "city",
            title = "Город",
            subtitle = "Повседневные покупки",
            accent = CaseAccent.CYAN,
            offers = listOf(
                offer(
                    id = "alma-market",
                    collectionId = "city",
                    merchantName = "Алма Маркет",
                    category = "Продукты",
                    rate = 3,
                    blurb = "Вымышленный магазин продуктов. Образец ставки относится к покупке, а не к плате за участие.",
                ),
                offer(
                    id = "step-apteka",
                    collectionId = "city",
                    merchantName = "Степь Аптека",
                    category = "Аптека",
                    rate = 4,
                    blurb = "Вымышленная аптека. В рабочей модели кешбэк шёл бы из комиссии продавца.",
                ),
                offer(
                    id = "dom-kofe",
                    collectionId = "city",
                    merchantName = "Дом Кофе",
                    category = "Кафе",
                    rate = 5,
                    blurb = "Вымышленная кофейня. Считается только квалифицирующая покупка.",
                ),
            ),
        ),
        collection(
            id = "home",
            title = "Дом и техника",
            subtitle = "Техника и товары для дома",
            accent = CaseAccent.GOLD,
            offers = listOf(
                offer(
                    id = "nomad-tech",
                    collectionId = "home",
                    merchantName = "Номад Тех",
                    category = "Электроника",
                    rate = 7,
                    blurb = "Вымышленный магазин техники. Ставка не обещает доход.",
                ),
                offer(
                    id = "yurt-mebel",
                    collectionId = "home",
                    merchantName = "Юрта Мебель",
                    category = "Мебель",
                    rate = 6,
                    blurb = "Вымышленный мебельный магазин. Покупка в прототипе не создаётся у продавца.",
                ),
                offer(
                    id = "baikal-byt",
                    collectionId = "home",
                    merchantName = "Байкал Быт",
                    category = "Товары для дома",
                    rate = 5,
                    blurb = "Вымышленный магазин товаров для дома.",
                ),
            ),
        ),
        collection(
            id = "service",
            title = "Сервис",
            subtitle = "Поездки, спорт и досуг",
            accent = CaseAccent.SPLIT,
            offers = listOf(
                offer(
                    id = "tulpar-travel",
                    collectionId = "service",
                    merchantName = "Тулпар Тревел",
                    category = "Поездки",
                    rate = 10,
                    blurb = "Вымышленный сервис поездок. 10% — верх образца и не обещание выплаты.",
                ),
                offer(
                    id = "arlan-fit",
                    collectionId = "service",
                    merchantName = "Арлан Фитнес",
                    category = "Спорт",
                    rate = 8,
                    blurb = "Вымышленный спортивный зал. Ставка относится к оплате занятия, если бы покупка была настоящей.",
                ),
                offer(
                    id = "saryarka-kino",
                    collectionId = "service",
                    merchantName = "Сарыарка Кино",
                    category = "Досуг",
                    rate = 6,
                    blurb = "Вымышленный кинотеатр. Билет в прототипе не продаётся.",
                ),
            ),
        ),
    )

    init {
        require(collections.size == 3)
        val offers = collections.flatMap { it.offers }
        require(offers.map { it.id }.distinct().size == offers.size)
        require(collections.map { it.id }.distinct().size == collections.size)
        collections.forEach { collection ->
            require(collection.offers.isNotEmpty())
            collection.offers.forEach { offer ->
                require(offer.collectionId == collection.id)
                require(offer.merchantName.isNotBlank())
                require(offer.sampleRatePercent in 1..CashbackRules.MAX_SAMPLE_RATE_PERCENT) {
                    offer.id
                }
                require(offer.blurb.startsWith("Вымышленн"))
            }
        }
        require(offers.count { it.sampleRatePercent == CashbackRules.MAX_SAMPLE_RATE_PERCENT } == 1)
    }

    fun collection(id: String): OfferCollection? = collections.firstOrNull { it.id == id }

    fun offer(id: String): Offer? = collections.flatMap { it.offers }.firstOrNull { it.id == id }

    fun requireOffer(id: String): Offer = offer(id) ?: error("Unknown sample offer: $id")

    private fun collection(
        id: String,
        title: String,
        subtitle: String,
        accent: CaseAccent,
        offers: List<Offer>,
    ): OfferCollection = OfferCollection(
        id = id,
        title = title,
        subtitle = subtitle,
        accent = accent,
        offers = offers,
    )

    private fun offer(
        id: String,
        collectionId: String,
        merchantName: String,
        category: String,
        rate: Int,
        blurb: String,
    ): Offer = Offer(
        id = id,
        collectionId = collectionId,
        merchantName = merchantName,
        category = category,
        sampleRatePercent = rate,
        blurb = blurb,
        terms = DemoCopy.sampleTerms(rate),
    )
}
