package com.example.data.model

data class ResonanceOfferTemplate(
    val id: String,
    val title: String,
    val description: String,
    val originalPrice: Double,
    val originalShippingCost: Double,
    val originalSeller: String,
    val category: String,
    val imageUrl: String,
    val resonanceScore: Int,
    val resonanceReason: String,
    val views: Int,
    val solds: Int
)

object ResonanceCatalog {
    val TOP_OFFERS: List<ResonanceOfferTemplate> = listOf(
        ResonanceOfferTemplate(
            id = "EBAY-DE-77401",
            title = "Makita 18V Akku-Kombihammer DHR243Z Brushless Solo",
            description = "Originalverpackter Makita DHR243Z Akku-Bohrhammer mit bürstenlosem Motor. 3 Funktionen: Bohren, Hammerbohren, Meißeln. SDS-Plus Aufnahme. Neuware vom Fachhändler.",
            originalPrice = 169.00,
            originalShippingCost = 6.90,
            originalSeller = "werkzeug_direkt_de",
            category = "Heimwerker & Werkzeuge",
            imageUrl = "https://images.unsplash.com/photo-1504148455328-c376907d081c?auto=format&fit=crop&w=600&q=80",
            resonanceScore = 96,
            resonanceReason = "Extrem hohe Verkaufsrate: 34 Verkäufe in 48 Std. Hohe Preiselastizität bei kostenlosem Expressversand.",
            views = 680,
            solds = 34
        ),
        ResonanceOfferTemplate(
            id = "EBAY-DE-77402",
            title = "Philips Hue White & Color Ambiance E27 Starter-Set (3x Lampen + Bridge)",
            description = "Smartes Beleuchtungsset von Philips Hue. 16 Millionen Farben, stufenlos dimmbar, kompatibel mit Alexa, Apple Home und Google Assistant. OVP Neuware.",
            originalPrice = 119.90,
            originalShippingCost = 4.90,
            originalSeller = "smart_lighting_store",
            category = "Smart Home & Beleuchtung",
            imageUrl = "https://images.unsplash.com/photo-1550524514-9b63488f57f6?auto=format&fit=crop&w=600&q=80",
            resonanceScore = 97,
            resonanceReason = "Top Bestseller im Smart Home Bereich. Mehr als 50 Verkäufe pro Woche. Schneller Umschlag garantiert.",
            views = 910,
            solds = 52
        ),
        ResonanceOfferTemplate(
            id = "EBAY-DE-77403",
            title = "Sony WH-1000XM5 Wireless Noise Cancelling Bluetooth Kopfhörer Schwarz",
            description = "Premium Over-Ear Kopfhörer mit branchenführendem Active Noise Cancelling, bis zu 30h Akkulaufzeit, Hi-Res Audio und Freisprechfunktion. Neu & versiegelt.",
            originalPrice = 289.00,
            originalShippingCost = 0.00,
            originalSeller = "audio_hifi_expert",
            category = "Audio & HiFi",
            imageUrl = "https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=600&q=80",
            resonanceScore = 98,
            resonanceReason = "Höchste Suchanfragen in der Kategorie Audio. Sehr starke Conversion bei Käufern mit Sofort-Kauf Vorliebe.",
            views = 1240,
            solds = 41
        ),
        ResonanceOfferTemplate(
            id = "EBAY-DE-77404",
            title = "AVM FRITZ!Box 7590 AX Wi-Fi 6 WLAN VDSL/ADSL Modem-Router",
            description = "Highspeed Router von AVM mit Wi-Fi 6 bis zu 3.600 MBit/s. Integrierte DECT-Basis für Telefonie und Smart Home. Deutsche Markenware.",
            originalPrice = 224.50,
            originalShippingCost = 5.49,
            originalSeller = "netzwerk_spezi_de",
            category = "Computer & Netzwerk",
            imageUrl = "https://images.unsplash.com/photo-1544197150-b99a580bb7a8?auto=format&fit=crop&w=600&q=80",
            resonanceScore = 95,
            resonanceReason = "Dauerbrenner im deutschen E-Commerce. Sofort-Kauf Rate über 90% im B2C-Segment.",
            views = 780,
            solds = 29
        ),
        ResonanceOfferTemplate(
            id = "EBAY-DE-77405",
            title = "Ninja Foodi DualZone 7,6L Heißluftfritteuse AF300EU 2 Fächer",
            description = "Beliebte 2-Zonen Heißluftfritteuse von Ninja. 6 Zubereitungsfunktionen, unabhängige Garzonen mit SYNC-Funktion, bis zu 75% schneller als Umluftöfen.",
            originalPrice = 148.00,
            originalShippingCost = 6.50,
            originalSeller = "kuechen_deals_direct",
            category = "Küche & Haushalt",
            imageUrl = "https://images.unsplash.com/photo-1584269600464-37b1b58a9fe7?auto=format&fit=crop&w=600&q=80",
            resonanceScore = 99,
            resonanceReason = "Absoluter Trendartikel mit extrem hoher Nachfrage. 62 Verkäufe in 72h. Sehr hohe Wiederverkaufssicherheit.",
            views = 1450,
            solds = 62
        ),
        ResonanceOfferTemplate(
            id = "EBAY-DE-77406",
            title = "Bosch Professional Kreuzlinienlaser GLL 2-15 G Grün + BT 150 Stativ",
            description = "Selbstnivellierender Linienlaser mit grünen Laserlinien für 4x bessere Sichtbarkeit. Arbeitsbereich bis 15m, inklusive Bauteleskopstativ BT 150 und Tasche.",
            originalPrice = 139.99,
            originalShippingCost = 5.90,
            originalSeller = "profi_baudiscount",
            category = "Heimwerker & Werkzeuge",
            imageUrl = "https://images.unsplash.com/photo-1581092160607-ee22621dd758?auto=format&fit=crop&w=600&q=80",
            resonanceScore = 94,
            resonanceReason = "Starke Nachfrage im gewerblichen und privaten Handwerkerbereich. Hervorragende Margenstabilität.",
            views = 520,
            solds = 26
        ),
        ResonanceOfferTemplate(
            id = "EBAY-DE-77407",
            title = "Kärcher K 5 Power Control Hochdruckreiniger 145 bar 2100W",
            description = "Leistungsstarker Kärcher K 5 mit Power Control Pistole und Strahlrohren, Plug 'n' Clean Reinigungsmittelsystem und wassergekühltem Motor. Made in Germany.",
            originalPrice = 249.00,
            originalShippingCost = 7.90,
            originalSeller = "haus_garten_shop",
            category = "Garten & Reinigung",
            imageUrl = "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?auto=format&fit=crop&w=600&q=80",
            resonanceScore = 93,
            resonanceReason = "Top-Saisonartikel für Haus und Garten mit konstant hoher Nachfrage und Spitzenbewertungen.",
            views = 690,
            solds = 28
        ),
        ResonanceOfferTemplate(
            id = "EBAY-DE-77408",
            title = "DeWalt DCF887N 18V XR bürstenloser 3-Stufen Akku-Schlagschrauber",
            description = "Kompakter DeWalt Akku-Schlagschrauber mit 205 Nm Drehmoment, 1/4\" Innensechskant Aufnahme, 3 LED-Ringen für optimale Ausleuchtung. OVP.",
            originalPrice = 94.50,
            originalShippingCost = 4.90,
            originalSeller = "dewalt_outlet_nord",
            category = "Heimwerker & Werkzeuge",
            imageUrl = "https://images.unsplash.com/photo-1572981779307-38b8cabb2407?auto=format&fit=crop&w=600&q=80",
            resonanceScore = 96,
            resonanceReason = "Sehr beliebt bei Handwerkern und ambitionierten Heimwerkern. Mehr als 45 Verkäufe pro Woche.",
            views = 840,
            solds = 38
        ),
        ResonanceOfferTemplate(
            id = "EBAY-DE-77409",
            title = "Ring Video Doorbell Plus HD-Sicherheitskamera mit Akkubetrieb",
            description = "Smarte Türklingel mit 1536p HD-Kopf-bis-Fuß-Video, Gegensprechfunktion, Bewegungserfassung und Farb-Nachtsicht. Einfache Montage ohne Kabel.",
            originalPrice = 129.00,
            originalShippingCost = 4.50,
            originalSeller = "smart_security_de",
            category = "Smart Home & Sicherheit",
            imageUrl = "https://images.unsplash.com/photo-1557597774-9d273605dfa9?auto=format&fit=crop&w=600&q=80",
            resonanceScore = 94,
            resonanceReason = "Stetig wachsendes Sicherheitssegment mit überdurchschnittlicher Kaufabschlussquote.",
            views = 630,
            solds = 31
        ),
        ResonanceOfferTemplate(
            id = "EBAY-DE-77410",
            title = "Gardena smart SILENO city 500 m² Mähroboter Set mit Smart Gateway",
            description = "Vollautomatischer Rasenmäherroboter für Rasenflächen bis 500 m². Flüsterleise mit 57 dB(A), bewältigt Steigungen bis 35 %, App-Steuerung per WLAN.",
            originalPrice = 649.00,
            originalShippingCost = 0.00,
            originalSeller = "garten_profi_center",
            category = "Garten & Rasenpflege",
            imageUrl = "https://images.unsplash.com/photo-1589923188900-85dae523342b?auto=format&fit=crop&w=600&q=80",
            resonanceScore = 97,
            resonanceReason = "Hochpreisiges Premium-Segment mit überragender Gewinnspanne und treuer Käuferschicht.",
            views = 1120,
            solds = 24
        ),
        ResonanceOfferTemplate(
            id = "EBAY-DE-77411",
            title = "Anker 737 Powerbank (PowerCore 24K) 140W 3-Port externer Akku",
            description = "Leistungsstarke 24.000 mAh Powerbank mit intelligenter Digitalanzeige, Power Delivery 3.1 und 140 W Schnellladefunktion für Laptops und Smartphones.",
            originalPrice = 89.90,
            originalShippingCost = 4.90,
            originalSeller = "gadget_store_berlin",
            category = "Elektronik & Zubehör",
            imageUrl = "https://images.unsplash.com/photo-1545454675-3531b543be5d?auto=format&fit=crop&w=600&q=80",
            resonanceScore = 98,
            resonanceReason = "Meistverkaufte High-End Powerbank auf eBay.de. 49 Verkäufe in 48 Stunden.",
            views = 890,
            solds = 49
        ),
        ResonanceOfferTemplate(
            id = "EBAY-DE-77412",
            title = "NOCO Boost HD GB70 2000A 12V UltraSafe Lithium Starthilfe Powerbank",
            description = "Tragbare Starthilfebatterie für Benzinmotoren bis 8 Liter und Dieselmotoren bis 6 Liter. Funkengeschützte Technologie und Verpolungsschutz mit LED-Taschenlampe.",
            originalPrice = 179.95,
            originalShippingCost = 5.95,
            originalSeller = "kfz_profi_teile",
            category = "Auto & Motorrad Zubehör",
            imageUrl = "https://images.unsplash.com/photo-1503376780353-7e6692767b70?auto=format&fit=crop&w=600&q=80",
            resonanceScore = 95,
            resonanceReason = "Hohe Conversion im Automotive-Bereich. Sehr geringe Retourenquote und treue Käufer.",
            views = 710,
            solds = 32
        )
    )

    fun getRandomOffer(): ResonanceOfferTemplate = TOP_OFFERS.random()

    fun generateOffers(count: Int): List<ResonanceOfferTemplate> {
        if (count <= 0) return emptyList()
        val result = mutableListOf<ResonanceOfferTemplate>()
        // First add the hand-crafted top offers
        result.addAll(TOP_OFFERS.shuffled().take(count))
        if (result.size >= count) return result

        val categories = listOf(
            "Heimwerker & Werkzeuge" to listOf(
                "Makita 18V Akku-Bohrschrauber DDF485Z Brushless",
                "Bosch Professional 18V Akku-Winkelschleifer GWS 18V-10",
                "Festool Tauchsäge TS 55 FEBQ-Plus im Systainer",
                "Metabo Kappsäge KGS 216 M mit Zugfunktion",
                "Wera Kraftform Kompakt 100 52-teiliger Werkzeugsatz",
                "Knipex Cobra Wasserpumpenzangen-Set 3-teilig",
                "DeWalt Akku-Handkreissäge DCS570N 18V 184mm",
                "Stabila Wasserwaagen-Set 3-teilig Type 70",
                "Einhell Power X-Change 18V 4,0Ah Akku & Schnellladegerät",
                "Wiha Drehmoment-Schraubendreher Set VDE 1000V"
            ),
            "Smart Home & Beleuchtung" to listOf(
                "Nanoleaf Shapes Triangles Starter Kit 9 Panels RGB",
                "Eve Energy Smarte Steckdose mit Strommessung Apple Home",
                "Bosch Smart Home Heizkörper-Thermostat II 3er-Pack",
                "Shelly Plus 1PM WLAN Relais-Schalter mit Leistungsmessung",
                "Aqara U200 Smartes Türschloss mit Fingerabdruck & Apple Home Key",
                "TP-Link Tapo C220 2K 4MP schwenkbare WLAN-Kamera",
                "Nuki Smart Lock Pro (4. Gen) mit integriertem Matter & WLAN",
                "Netatmo Smarte Wetterstation mit WLAN für Innen & Außen",
                "Govee RGBIC LED Stehleuchte mit Musiksynchronisation",
                "Somfy Funk-Handsender Situo 5 io Pure II 5-Kanal"
            ),
            "Audio & HiFi" to listOf(
                "JBL Charge 5 tragbarer Bluetooth Lautsprecher IP67",
                "Bose QuietComfort SC kabellose Noise-Cancelling Kopfhörer",
                "Sennheiser Momentum 4 Wireless Bluetooth Over-Ear",
                "Marshall Acton III Bluetooth Lautsprecher Vintage Design",
                "Teufel REAL BLUE NC kabelloser ANC Bluetooth Kopfhörer",
                "Sonos Era 100 Smart Speaker mit AirPlay 2 und Bluetooth",
                "Rode Wireless PRO kabelloses 2-Kanal Mikrofonsystem",
                "Audio-Technica AT-LP120XUSB DJ Plattenspieler mit USB",
                "Beyerdynamic DT 770 PRO 80 Ohm Studio-Kopfhörer",
                "Shure SM7B Dynamisches Mikrofon für Podcast und Studio"
            ),
            "Computer & Netzwerk" to listOf(
                "Logitech MX Master 3S kabellose Performance-Maus",
                "Samsung 990 PRO NVMe M.2 SSD 2TB mit Heatsink",
                "Synology DiskStation DS224+ 2-Bay NAS-Gehäuse",
                "Logitech MX Keys S beleuchtete kabellose Tastatur",
                "Crucial Pro DDR5 RAM 32GB (2x16GB) 6000MHz",
                "Elgato Stream Deck MK.2 mit 15 anpassbaren LCD-Tasten",
                "Anker 575 USB-C Dockingstation 12-in-1 Triple Display",
                "ASUS ROG Swift 27 Zoll QHD 240Hz Gaming-Monitor",
                "WD Red Plus 4TB NAS interne Festplatte 3,5 Zoll",
                "Razer DeathAdder V3 Pro kabellose eSports Gaming Maus"
            ),
            "Küche & Haushalt" to listOf(
                "Ninja Creami Eismaschine NC300EU 3 Becher",
                "Braun MultiQuick 9 Stabmixer MQ 9187XLI 1200W",
                "Kärcher Fenstersauger WV 6 Plus mit extra langer Laufzeit",
                "Philips Hue White & Color Ambiance Play Lightbar Doppelpack",
                "De'Longhi Dedica Deluxe Siebträgermaschine EC685.M",
                "Tefal OptiGrill Elite Kontaktgrill mit 12 Automatikprogrammen",
                "WMF Profi Pfannenset 24cm und 28cm Cromargan Edelstahl",
                "Bosch Küchenmaschine MUM5 CreationLine 1000 Watt",
                "Le Creuset Signature Gusseisen Bräter oval 29cm",
                "KitchenAid Artisan 4,8L Küchenmaschine Empire Rot"
            ),
            "Garten & Outdoor" to listOf(
                "Gardena Bewässerungscomputer Bluetooth für Wasserhahn",
                "Fiskars Spaltaxt X25 XL mit Fiberglasstiel",
                "Bosch Akku-Gartenschere Pro Pruner 12V Solo",
                "Kärcher Akku-Gras- und Strauchschere GSH 18-20",
                "Weber Master-Touch GBS Holzkohlegrill 57cm E-5750",
                "Olight Baton 3 Pro LED Taschenlampe 1500 Lumen wiederaufladbar",
                "Jackery Explorer 500 tragbare Powerstation 518Wh",
                "Petromax Feuertopf ft6 Dutch Oven aus Gusseisen",
                "Therm-a-Rest NeoAir XTherm NXT isolierte Isomatte",
                "Stanley Master Vakuum Thermosflasche 1,3 Liter"
            )
        )

        var idx = result.size + 1
        val random = java.util.Random(42)

        while (result.size < count) {
            val (categoryName, productTitles) = categories[random.nextInt(categories.size)]
            val productTitle = productTitles[random.nextInt(productTitles.size)]
            val basePrice = 25.0 + random.nextInt(350) + 0.90
            val shipping = if (random.nextBoolean()) (3.90 + random.nextInt(4)) else 0.0
            val score = 88 + random.nextInt(12)
            val views = 300 + random.nextInt(1200)
            val solds = 12 + random.nextInt(60)

            result.add(
                ResonanceOfferTemplate(
                    id = "EBAY-DE-${78000 + idx}",
                    title = "$productTitle (OVP Neuware)",
                    description = "Original $productTitle. Neu und unbenutzt in versiegelter Originalverpackung. Sofort lieferbar mit Sendungsverfolgung.",
                    originalPrice = Math.round(basePrice * 100.0) / 100.0,
                    originalShippingCost = Math.round(shipping * 100.0) / 100.0,
                    originalSeller = "top_retailer_${random.nextInt(900) + 100}",
                    category = categoryName,
                    imageUrl = "https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?auto=format&fit=crop&w=600&q=80",
                    resonanceScore = score,
                    resonanceReason = "Top Resonanz-Cluster: $solds Verkäufe in 48 Stunden. Ausgezeichnete Marge im Nischensegment.",
                    views = views,
                    solds = solds
                )
            )
            idx++
        }

        return result
    }
}
