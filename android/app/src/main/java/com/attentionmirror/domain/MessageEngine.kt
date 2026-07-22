package com.attentionmirror.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlin.random.Random

/**
 * Context-aware quirky message generator.
 *
 * Rather than storing thousands of fixed lines, it *assembles* each message from
 * pools of fragments — a context-aware opener + a tone punchline — so a few
 * hundred authored fragments combine into many thousands of unique messages, and
 * a fresh one shows every time the app opens (seeded by a rotating nonce).
 *
 * Context it reacts to: time of day, day vs. night, weekend, how heavy the day
 * was, and the *kind* of app that dominated (social / video / gaming / browsing
 * / shopping / messaging). Copy stays cheeky but never mean — aimed at the young
 * and the young-at-heart. Translations are first-pass slang, refined over time;
 * any language falls back to English for pools it hasn't filled yet.
 */
object MessageEngine {

    enum class TimeOfDay { MORNING, AFTERNOON, EVENING, NIGHT }
    enum class AppCategory { SOCIAL, VIDEO, GAMING, BROWSING, SHOPPING, MESSAGING, GENERAL }

    /** Map a tracked package/platform to a broad category for message flavour. */
    fun categoryFor(packageName: String): AppCategory = when (packageName) {
        "com.google.android.youtube" -> AppCategory.VIDEO
        "in.mohalla.sharechat", "com.mymoj.android", "com.eterno.shortvideos" -> AppCategory.VIDEO
        "com.instagram.android", "com.facebook.katana", "com.twitter.android",
        "com.zhiliaoapp.musically", "com.reddit.frontpage", "com.snapchat.android",
        -> AppCategory.SOCIAL
        "com.whatsapp" -> AppCategory.MESSAGING
        "com.android.chrome" -> AppCategory.BROWSING
        else -> AppCategory.GENERAL
    }

    fun timeOfDay(time: LocalTime): TimeOfDay = when (time.hour) {
        in 5..11 -> TimeOfDay.MORNING
        in 12..16 -> TimeOfDay.AFTERNOON
        in 17..21 -> TimeOfDay.EVENING
        else -> TimeOfDay.NIGHT
    }

    /**
     * Build a fresh message. [nonce] should change per app-open so the line
     * rotates; [lang] is the two-letter language code of the active locale.
     */
    fun generate(
        receipt: AttentionReceipt,
        date: LocalDate,
        time: LocalTime,
        category: AppCategory,
        tone: Tone,
        currency: Currency,
        lang: String,
        nonce: Long,
    ): DynamicMessage {
        val pack = PACKS[lang] ?: PACKS.getValue("en")
        val fallback = PACKS.getValue("en")
        val rng = Random(nonce xor date.toEpochDay())

        val minutes = receipt.totalMinutes
        val tod = timeOfDay(time)
        val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY

        // Assemble a candidate opener pool from every applicable context bucket,
        // falling back to English for any pool this language hasn't filled.
        fun catPool(cat: AppCategory): List<String> =
            pack.catOpeners[cat]?.takeIf { it.isNotEmpty() } ?: fallback.catOpeners[cat].orEmpty()
        fun timePool(t: TimeOfDay): List<String> =
            pack.timeOpeners[t]?.takeIf { it.isNotEmpty() } ?: fallback.timeOpeners[t].orEmpty()

        val openers = buildList {
            addAll(catPool(category))
            addAll(timePool(tod))
            if (isWeekend) addAll(pack.weekendOpeners.ifEmpty { fallback.weekendOpeners })
            if (minutes >= 180) addAll(pack.heavyOpeners.ifEmpty { fallback.heavyOpeners })
            if (minutes in 0.1..30.0) addAll(pack.lightOpeners.ifEmpty { fallback.lightOpeners })
            if (isEmpty()) addAll(catPool(AppCategory.GENERAL))
        }.ifEmpty { fallback.catOpeners.getValue(AppCategory.GENERAL) }

        val punchPool = (pack.punch[tone]?.takeIf { it.isNotEmpty() }
            ?: fallback.punch.getValue(tone))

        val headline = fill(openers[rng.nextInt(openers.size)], receipt, currency)
        val body = fill(punchPool[rng.nextInt(punchPool.size)], receipt, currency)
        return DynamicMessage(headline, body)
    }

    private fun fill(s: String, receipt: AttentionReceipt, currency: Currency): String {
        val value = Formatting.valueRange(receipt.estimatedValueLowInr, receipt.estimatedValueHighInr, currency)
        val top = receipt.perPlatform.firstOrNull()?.platform ?: "apps"
        return s.replace("{value}", value)
            .replace("{time}", Formatting.minutes(receipt.totalMinutes))
            .replace("{ads}", receipt.estimatedAdsSeen.toString())
            .replace("{top}", top)
            .replace("{returned}", Formatting.money(receipt.userReceivedInr, currency))
    }

    private class Pack(
        val timeOpeners: Map<TimeOfDay, List<String>>,
        val catOpeners: Map<AppCategory, List<String>>,
        val weekendOpeners: List<String>,
        val heavyOpeners: List<String>,
        val lightOpeners: List<String>,
        val punch: Map<Tone, List<String>>,
    )

    // ---------------------------------------------------------------- English
    private val EN = Pack(
        timeOpeners = mapOf(
            TimeOfDay.MORNING to listOf(
                "Morning scroll before the coffee even kicked in ☕",
                "Rise and grind — for someone else's ad revenue 🌅",
                "Bright and early, the feed was already open 👀",
                "First thing today: you fed the algorithm 🥱",
                "Sunrise scroll? The ads were awake too ☀️",
                "Good morning! The attention economy says thanks 🌄",
            ),
            TimeOfDay.AFTERNOON to listOf(
                "Midday break turned into a scroll marathon 🏃",
                "Afternoon slump? The feed caught you 😴",
                "Lunchtime doomscroll, a modern classic 🍜",
                "Post-lunch scroll, fully monetized 🥗",
                "Afternoon vibes, advertiser profits 📈",
            ),
            TimeOfDay.EVENING to listOf(
                "Evening wind-down = ad wind-up 🌆",
                "Prime time, and you were the audience 🍿",
                "Sunset scroll hits different 🌇",
                "After-work scroll, clocking unpaid overtime 🕕",
                "Evening feed session, brought to you by ads 📺",
            ),
            TimeOfDay.NIGHT to listOf(
                "3am and still scrolling? Bold move 🌙",
                "Late-night doomscroll speedrun 🏁",
                "The moon's out and so are the ads 🌚",
                "Can't sleep? The algorithm never does 😵‍💫",
                "Midnight scroll — advertisers love the night shift 🌃",
                "Burning the midnight data 🔋",
            ),
        ),
        catOpeners = mapOf(
            AppCategory.SOCIAL to listOf(
                "The group chat could wait. The feed couldn't 💬",
                "Stalking timelines like it's a full-time job 🕵️",
                "Another lap around {top} 🔁",
                "You came for one notification, stayed for 200 🔔",
                "Refresh, refresh, refresh — {top} thanks you 🙏",
                "Everyone's highlight reel, your unpaid time 🎞️",
                "Double-tapping your way into someone's revenue ❤️",
            ),
            AppCategory.VIDEO to listOf(
                "Every 3rd reel was an ad and you know it 📱",
                "Just one more video became twelve 🎬",
                "Autoplay: 1, Willpower: 0 ▶️",
                "The skip-ad button got a workout ⏭️",
                "Short videos, long ad breaks 🍿",
                "You didn't watch {top}. {top} watched you 👁️",
                "Endless scroll, endless pre-rolls 🌀",
            ),
            AppCategory.GAMING to listOf(
                "Grinding ranked while advertisers grind you 🎮",
                "GG — the ads won this match 🏆",
                "One more game became the whole session 🕹️",
                "Loading-screen ads: the real final boss 👾",
                "Your K/D was great, your screen time worse 🎯",
                "Level up! Advertisers leveled up faster 🆙",
            ),
            AppCategory.BROWSING to listOf(
                "Down the rabbit hole, ad banners all the way 🐇",
                "37 tabs and every one had ads 🗂️",
                "Just googling turned into an expedition 🔎",
                "The internet browsed you back 🌐",
                "You searched. They earned. 💸",
            ),
            AppCategory.SHOPPING to listOf(
                "Window shopping, but the window's tracking you 🛍️",
                "Cart full, wallet empty, ads happy 🛒",
                "Add to cart, add to their revenue 💳",
            ),
            AppCategory.MESSAGING to listOf(
                "Chatting away while ads slid in between 💬",
                "Replying to everyone but your own screen time 📲",
            ),
            AppCategory.GENERAL to listOf(
                "Your time went somewhere. Advertisers know where 📍",
                "Another day, another unpaid shift for the feed 🥱",
                "You showed up. The ads showed up more 🎪",
                "Time flies when someone else is billing it ⏳",
                "Attention spent. Receipt attached 🧾",
            ),
        ),
        weekendOpeners = listOf(
            "Weekend vibes, weekday ad load 🎉",
            "Your day off, the algorithm's payday 💼",
            "Relaxing? The ad engine isn't 🛋️",
            "Weekend scroll, no days off for advertisers 📅",
        ),
        heavyOpeners = listOf(
            "Big scroll energy today 🔋",
            "That was a LOT of screen time 📊",
            "Marathon session detected 🏅",
            "Heavy day on the feed 🏋️",
        ),
        lightOpeners = listOf(
            "Barely touched it today — respect ✋",
            "Light scroll day, nicely done 🌱",
            "Quick peek, quick exit 🚪",
        ),
        punch = mapOf(
            Tone.QUIRKY to listOf(
                "{time} of scrolling = {value} for advertisers. Your cut: {returned} 💸",
                "{ads} ads watched you back. {value} created. You? {returned} 👀",
                "Salary for today's scroll: {value}… to them. To you: {returned} 🧾",
                "{time} clocked in. Payslip says {returned} 😮‍💨",
                "You generated {value}. You keep {returned}. Math checks out 🤝",
                "Featured in ~{ads} ads. Box office: {value}. Royalties: {returned} 🎬",
                "The house always wins: {value} to them 🎰",
                "Tip jar for your attention: {returned} 🫙",
                "{value} of vibes monetized. Paid in dopamine 🧠",
                "Your eyeballs did {value} of work today 👁️",
                "{time} → {value}. Not a typo. Not yours either 🙃",
                "You: {returned}. Them: {value}. Rematch tomorrow? 🥊",
                "That's {value} of free labour, premium vibes ✨",
                "{ads} ads, {value} earned, {returned} returned. Iconic 💅",
                "Unpaid internship update: still {returned} 🧑‍💻",
                "Congrats, you were the product again 🎉",
                "Your attention: sold. Your wallet: {returned} 🏷️",
                "{value} created, {returned} received. The gap is the joke 😂",
                "The algorithm ate good today: {value} 🍽️",
                "{value} for them, character development for you 🎭",
                "You made rent… for an ad server 🏠",
                "{time} of your life, {value} of their money ⏳",
                "Plot twist: the free app wasn't free. You paid in attention 🪤",
                "Streak maintained. Bank balance: {returned} 🔥",
                "Somewhere a CEO thanks you for {value} 🤵",
            ),
            Tone.GENTLE to listOf(
                "{time} today created about {value}. None of it returned to you.",
                "Your attention was worth {value} to others today.",
                "That's {value} of value from your time — {returned} came back.",
                "{ads} ads across {time}. Value created: {value}.",
            ),
            Tone.HARD to listOf(
                "You worked for the attention economy today. Unpaid.",
                "Your scrolling created {value}. You were paid {returned}.",
                "Someone billed for your time today. It wasn't you.",
                "{time} gone. {value} earned. Not by you.",
            ),
        ),
    )

    // ------------------------------------------------------------------ Hindi
    // First-pass slang. Refine the punchlines to taste.
    private val HI = Pack(
        timeOpeners = mapOf(
            TimeOfDay.MORNING to listOf(
                "चाय से पहले ही सुबह-सुबह स्क्रॉल शुरू ☕",
                "सुबह उठते ही फ़ीड खुल गई 👀",
                "गुड मॉर्निंग! अटेंशन इकॉनमी का शुक्रिया 🌄",
            ),
            TimeOfDay.AFTERNOON to listOf(
                "दोपहर का ब्रेक स्क्रॉल मैराथन बन गया 🏃",
                "लंच वाला डूमस्क्रॉल, अब तो आदत है 🍜",
            ),
            TimeOfDay.EVENING to listOf(
                "शाम की चिल = विज्ञापन की बिल 🌆",
                "प्राइम टाइम, और दर्शक आप थे 🍿",
            ),
            TimeOfDay.NIGHT to listOf(
                "रात के 3 बजे भी स्क्रॉल? दमदार 🌙",
                "नींद नहीं आ रही? एल्गोरिद्म को भी नहीं 😵‍💫",
                "आधी रात का डेटा जल रहा है 🔋",
            ),
        ),
        catOpeners = mapOf(
            AppCategory.SOCIAL to listOf(
                "ग्रुप चैट रुक सकती थी, फ़ीड नहीं 💬",
                "{top} का एक और चक्कर 🔁",
                "एक नोटिफिकेशन के लिए आए, 200 देखकर गए 🔔",
            ),
            AppCategory.VIDEO to listOf(
                "हर तीसरी रील एक विज्ञापन थी, आपको पता है 📱",
                "बस एक और वीडियो… बारह हो गए 🎬",
                "{top} ने आपको देखा, आपने नहीं 👁️",
            ),
            AppCategory.GAMING to listOf(
                "रैंक ग्राइंड कर रहे थे, विज्ञापन आपको ग्राइंड कर रहे थे 🎮",
                "एक और गेम में पूरा सेशन निकल गया 🕹️",
            ),
            AppCategory.BROWSING to listOf(
                "37 टैब और हर एक पर विज्ञापन 🗂️",
                "आपने सर्च किया, उन्होंने कमाया 💸",
            ),
            AppCategory.GENERAL to listOf(
                "समय कहीं गया। विज्ञापन वालों को पता है कहाँ 📍",
                "एक और दिन, फ़ीड के लिए बिना पैसे की शिफ्ट 🥱",
                "अटेंशन खर्च हुआ। रसीद हाज़िर 🧾",
            ),
        ),
        weekendOpeners = listOf(
            "आपकी छुट्टी, एल्गोरिद्म की कमाई का दिन 💼",
            "वीकेंड स्क्रॉल, विज्ञापन वालों की कोई छुट्टी नहीं 📅",
        ),
        heavyOpeners = listOf(
            "आज तो जबरदस्त स्क्रीन टाइम रहा 📊",
            "फ़ीड पर भारी दिन 🏋️",
        ),
        lightOpeners = listOf(
            "आज मुश्किल से छुआ — रिस्पेक्ट ✋",
        ),
        punch = mapOf(
            Tone.QUIRKY to listOf(
                "{time} स्क्रॉलिंग = विज्ञापन वालों को {value}। आपका हिस्सा: {returned} 💸",
                "{ads} विज्ञापनों ने आपको देखा। बनी {value}। आपको? {returned} 👀",
                "आज के स्क्रॉल की सैलरी: {value}… उन्हें। आपको: {returned} 🧾",
                "बधाई हो, आज फिर आप ही प्रोडक्ट थे 🎉",
                "{value} बनी, {returned} मिली। यही तो मज़ाक है 😂",
                "आपने {value} की मेहनत की, इनाम मिला {returned} 🤝",
            ),
            Tone.GENTLE to listOf(
                "{time} ने आज करीब {value} बनाई। आपको कुछ नहीं मिला।",
                "आज आपका ध्यान दूसरों के लिए {value} का था।",
            ),
            Tone.HARD to listOf(
                "आज आपने अटेंशन इकॉनमी के लिए काम किया। बिना पैसे।",
                "आपकी स्क्रॉलिंग ने {value} बनाई। आपको मिला {returned}।",
            ),
        ),
    )

    // ------------------------------------------------------------------ Tamil
    private val TA = Pack(
        timeOpeners = mapOf(
            TimeOfDay.MORNING to listOf(
                "காபிக்கு முன்பே காலை ஸ்க்ரோல் ☕",
                "காலையிலேயே ஃபீட் திறந்தாச்சு 👀",
                "குட் மார்னிங்! கவன பொருளாதாரம் நன்றி சொல்கிறது 🌄",
            ),
            TimeOfDay.AFTERNOON to listOf(
                "மதிய ஓய்வு ஸ்க்ரோல் மாரத்தான் ஆச்சு 🏃",
                "லஞ்ச் நேர டூம்ஸ்க்ரோல், இப்ப பழக்கம் 🍜",
            ),
            TimeOfDay.EVENING to listOf(
                "மாலை சில் = விளம்பர பில் 🌆",
                "பிரைம் டைம், பார்வையாளர் நீங்க தான் 🍿",
            ),
            TimeOfDay.NIGHT to listOf(
                "இரவு 3 மணிக்கும் ஸ்க்ரோலா? சூப்பர் 🌙",
                "தூக்கம் வரலையா? அல்காரிதத்துக்கும் வராது 😵‍💫",
            ),
        ),
        catOpeners = mapOf(
            AppCategory.SOCIAL to listOf(
                "குரூப் சாட் காத்திருக்கும், ஃபீட் காத்திராது 💬",
                "{top}-ல இன்னொரு சுற்று 🔁",
            ),
            AppCategory.VIDEO to listOf(
                "மூணாவது ரீல் ஒரு விளம்பரம், உங்களுக்கே தெரியும் 📱",
                "இன்னொரு வீடியோ… பன்னிரண்டு ஆச்சு 🎬",
            ),
            AppCategory.GAMING to listOf(
                "ரேங்க் கிரைண்ட், விளம்பரங்கள் உங்களை கிரைண்ட் 🎮",
            ),
            AppCategory.BROWSING to listOf(
                "நீங்க தேடினீங்க, அவங்க சம்பாதிச்சாங்க 💸",
            ),
            AppCategory.GENERAL to listOf(
                "நேரம் எங்கயோ போச்சு. விளம்பரதாரர்களுக்கு தெரியும் 📍",
                "கவனம் செலவழிச்சாச்சு. ரசீது இதோ 🧾",
            ),
        ),
        weekendOpeners = listOf(
            "உங்க விடுமுறை, அல்காரிதத்தின் சம்பள நாள் 💼",
        ),
        heavyOpeners = listOf(
            "இன்னைக்கு ஸ்க்ரீன் டைம் ரொம்ப அதிகம் 📊",
        ),
        lightOpeners = listOf(
            "இன்னைக்கு கம்மியா தான் — ரெஸ்பெக்ட் ✋",
        ),
        punch = mapOf(
            Tone.QUIRKY to listOf(
                "{time} ஸ்க்ரோல் = விளம்பரதாரர்களுக்கு {value}. உங்க பங்கு: {returned} 💸",
                "{ads} விளம்பரங்கள் உங்களை பார்த்தன. {value} உருவாச்சு. நீங்க? {returned} 👀",
                "வாழ்த்துக்கள், இன்னைக்கும் நீங்க தான் ப்ரொடக்ட் 🎉",
                "{value} உருவாக்கினீங்க, {returned} கிடைச்சது. அதுதான் ஜோக் 😂",
            ),
            Tone.GENTLE to listOf(
                "{time} இன்று சுமார் {value} உருவாக்கியது. உங்களுக்கு எதுவும் வரல.",
            ),
            Tone.HARD to listOf(
                "இன்று கவன பொருளாதாரத்துக்கு வேலை செஞ்சீங்க. சம்பளம் இல்ல.",
            ),
        ),
    )

    // ----------------------------------------------------------------- Telugu
    private val TE = Pack(
        timeOpeners = mapOf(
            TimeOfDay.MORNING to listOf(
                "కాఫీకి ముందే ఉదయాన్నే స్క్రోల్ ☕",
                "ఉదయాన్నే ఫీడ్ ఓపెన్ అయిపోయింది 👀",
                "గుడ్ మార్నింగ్! అటెన్షన్ ఎకానమీ థాంక్స్ చెప్తోంది 🌄",
            ),
            TimeOfDay.AFTERNOON to listOf(
                "మధ్యాహ్నం బ్రేక్ స్క్రోల్ మారథాన్ అయ్యింది 🏃",
                "లంచ్ టైం డూమ్‌స్క్రోల్, ఇప్పుడు అలవాటే 🍜",
            ),
            TimeOfDay.EVENING to listOf(
                "సాయంత్రం చిల్ = యాడ్ బిల్ 🌆",
                "ప్రైమ్ టైం, ప్రేక్షకులు మీరే 🍿",
            ),
            TimeOfDay.NIGHT to listOf(
                "రాత్రి 3 గంటలకూ స్క్రోలా? అదుర్స్ 🌙",
                "నిద్ర రావట్లేదా? అల్గారిథమ్‌కీ రాదు 😵‍💫",
            ),
        ),
        catOpeners = mapOf(
            AppCategory.SOCIAL to listOf(
                "గ్రూప్ చాట్ ఆగుతుంది, ఫీడ్ ఆగదు 💬",
                "{top}లో మరో రౌండ్ 🔁",
            ),
            AppCategory.VIDEO to listOf(
                "మూడో రీల్ ఒక యాడ్, మీకు తెలుసు 📱",
                "ఇంకో వీడియో… పన్నెండు అయ్యాయి 🎬",
            ),
            AppCategory.GAMING to listOf(
                "ర్యాంక్ గ్రైండ్, యాడ్స్ మిమ్మల్ని గ్రైండ్ 🎮",
            ),
            AppCategory.BROWSING to listOf(
                "మీరు సెర్చ్ చేశారు, వాళ్ళు సంపాదించారు 💸",
            ),
            AppCategory.GENERAL to listOf(
                "సమయం ఎక్కడికో పోయింది. యాడ్ వాళ్ళకు తెలుసు 📍",
                "అటెన్షన్ ఖర్చయ్యింది. రసీదు ఇదిగో 🧾",
            ),
        ),
        weekendOpeners = listOf(
            "మీ సెలవు, అల్గారిథమ్‌కి జీతం రోజు 💼",
        ),
        heavyOpeners = listOf(
            "ఈరోజు స్క్రీన్ టైం చాలా ఎక్కువ 📊",
        ),
        lightOpeners = listOf(
            "ఈరోజు చాలా తక్కువ — రెస్పెక్ట్ ✋",
        ),
        punch = mapOf(
            Tone.QUIRKY to listOf(
                "{time} స్క్రోల్ = యాడ్ వాళ్ళకు {value}. మీ వాటా: {returned} 💸",
                "{ads} యాడ్స్ మిమ్మల్ని చూశాయి. {value} తయారైంది. మీరు? {returned} 👀",
                "అభినందనలు, ఈరోజూ మీరే ప్రొడక్ట్ 🎉",
                "{value} తయారుచేశారు, {returned} వచ్చింది. అదే జోక్ 😂",
            ),
            Tone.GENTLE to listOf(
                "{time} ఈరోజు దాదాపు {value} తయారుచేసింది. మీకు ఏమీ రాలేదు.",
            ),
            Tone.HARD to listOf(
                "ఈరోజు అటెన్షన్ ఎకానమీ కోసం పని చేశారు. జీతం లేదు.",
            ),
        ),
    )

    private val PACKS: Map<String, Pack> = mapOf(
        "en" to EN,
        "hi" to HI,
        "ta" to TA,
        "te" to TE,
    )
}
