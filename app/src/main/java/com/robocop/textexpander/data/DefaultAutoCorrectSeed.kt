package com.robocop.textexpander.data

/** A small built-in library of common English typos, seeded once on first launch. */
object DefaultAutoCorrectSeed {
    val entries: List<Pair<String, String>> = listOf(
        "teh" to "the",
        "adn" to "and",
        "taht" to "that",
        "thier" to "their",
        "recieve" to "receive",
        "recieved" to "received",
        "definately" to "definitely",
        "seperate" to "separate",
        "occured" to "occurred",
        "untill" to "until",
        "wich" to "which",
        "becuase" to "because",
        "alot" to "a lot",
        "cant" to "can't",
        "dont" to "don't",
        "wont" to "won't",
        "im" to "I'm",
        "youre" to "you're",
        "its" to "it's",
        "goverment" to "government",
        "enviroment" to "environment",
        "neccessary" to "necessary",
        "acommodate" to "accommodate",
        "calender" to "calendar",
        "concious" to "conscious",
        "embarass" to "embarrass",
        "existance" to "existence",
        "februar" to "february",
        "foward" to "forward",
        "hieght" to "height",
        "independant" to "independent",
        "knowlege" to "knowledge",
        "liason" to "liaison",
        "maintainance" to "maintenance",
        "noticable" to "noticeable",
        "occassion" to "occasion",
        "persistant" to "persistent",
        "posession" to "possession",
        "publically" to "publicly",
        "recomend" to "recommend",
        "succesful" to "successful",
        "tommorow" to "tomorrow",
        "truely" to "truly",
        "wether" to "whether"
    )

    fun toEntities(): List<AutoCorrectEntry> = entries.map { (typo, correction) ->
        AutoCorrectEntry(typo = typo, correction = correction, isBuiltIn = true, enabled = true)
    }
}
