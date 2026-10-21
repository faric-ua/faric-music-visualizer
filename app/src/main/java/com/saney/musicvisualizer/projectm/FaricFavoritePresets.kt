package com.saney.musicvisualizer.projectm

/**
 * V81 (TODO, Poweramp favourites; faric 21.10 18:35: «так, постав»): the visualizations faric gave 👍
 * in Poweramp, as preset ids inside «Cream of the Crop» (path below the collection root — the same id
 * [ProjectMLibraryManager.presetId] gives). They get 👍 once, only where faric has no rating yet,
 * so the «👍» list (V110) is not empty from the start. Checked against the collection at
 * commit 0180df21 (the one the app downloads). Not in the collection: «Flexi - cell tissue»,
 * «artifac7».
 */
object FaricFavoritePresets {
    const val SEED_VERSION = 1

    val IDS: List<String> =
        listOf(
            "Particles/Points Trails/phat + EoS - Bass_responce_Red_Movements_Disorienting nebula4.milk",
            "Supernova/Burst/fiShbRaiN - betelguese (hyperdrive remix).milk",
            "Dancer/Comet Mirror/Krash & Rovastar - Cerebral Demons - Phat + EoS Killer Death Bunny Remix.milk",
            "Dancer/Comet Mirror/Krash & Rovastar - Cerebral Demons - Phat + EoS Moire Remix.milk",
            "Dancer/Comet Mirror/Krash & Rovastar - Cerebral Demons - Phat + EoS Stars Remix.milk",
            "Dancer/Comet Mirror/EoS + Phat - chasers 12 sentinel Daemon - mash0000 - multi-band time-distortion aurora granules.milk",
            "Dancer/Comet Mirror/EoS + Phat - chasers 14 sentinel 616.milk",
            "Dancer/Blobby/suksma - chemosynthetic nosferatu - gdy patent pending free energy devices - rand tritex - inv play.milk",
            "Drawing/Explosions/suksma - chemosynthetic nosferatu - automating transformation with b n s3 - claim form fsh shf flx nz+ christ a lien pistil.milk",
            "Drawing/Fractal Mirror/suksma - chemosynthetic nosferatu - automating transformation with b n s3.milk",
            "Waveform/Wire Flat/Geiss - Blur Mix 3.milk",
            "Fractal/Core Tunnel/A Tribute to Martin - bombyx mori - Ft Flexi - AdamFX - StahlRegen - Hd In Milk Ailen FX H.milk",
            "Fractal/Core Tunnel/A Tribute to Martin - bombyx mori - Ft Flexi - AdamFX - StahlRegen - Hd In Milk Ailen FX I.milk",
            "Fractal/Core Tunnel/A Tribute to Martin - bombyx mori - Ft Flexi - AdamFX - StahlRegen - Hd In Milk Ailen FX J.milk",
            "Reaction/Growth/Geiss - Brain Zoom 3.milk",
            "Dancer/Shapes/suksma - fiShbRaiN - brainstem activation - mash0000 - altar of metatronic chaos - j4-.milk",
            "Dancer/Blobby Mirror/flexi +  suksma + goody + fed + other artists - blarfff (throwing up cubes).milk",
        )
}
