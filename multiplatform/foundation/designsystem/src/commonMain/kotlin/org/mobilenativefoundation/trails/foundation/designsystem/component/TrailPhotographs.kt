package org.mobilenativefoundation.trails.foundation.designsystem.component

import androidx.compose.ui.Alignment
import org.jetbrains.compose.resources.DrawableResource
import trails.multiplatform.foundation.designsystem.generated.resources.Res
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_half_dome
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_trolltunga
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_tongariro_alpine_crossing
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_mount_fuji_via_yoshida_trail
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_laguna_de_los_tres
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_platteklip_gorge
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_plain_of_six_glaciers
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_angels_landing
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_preikestolen
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_roys_peak
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_tiger_s_nest
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_base_de_las_torres
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_petra_monastery_trail
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_franconia_ridge_loop
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_mist_trail_to_nevada_fall
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_tre_cime_di_lavaredo_loop
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_hooker_valley_track
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_adam_s_peak
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_laguna_69
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_tugela_falls_via_sentinel_peak
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_lake_agnes_tea_house
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_the_narrows
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_ben_nevis_mountain_track
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_mount_kosciuszko_via_thredbo
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_dragon_s_back
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_huayna_picchu
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_wadi_shab
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_garibaldi_lake
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_bright_angel_trail_to_havasupai_gardens
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_yr_wyddfa_snowdon_llanberis_path
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_bondi_to_coogee_coastal_walk
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_kawah_ijen
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_quilotoa_crater_rim_loop
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_alum_cave_trail_to_mount_leconte
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_upper_yosemite_fall_trail
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_caminito_del_rey
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_mount_takao_trail_1
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_highline_trail
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_samaria_gorge
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_delicate_arch_trail
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_lac_blanc
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_diamond_head_summit_trail
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_classic_inca_trail
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_everest_base_camp_trek
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_tour_du_mont_blanc
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_milford_track
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_torres_del_paine_w_trek
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_kilimanjaro_machame_route
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_laugavegur_trail
import trails.multiplatform.foundation.designsystem.generated.resources.trail_photo_overland_track

/** Offline asset registry. Provenance and byte hashes: composeResources/files/trail_photography.json. */
internal data class TrailPhotograph(
    val image: DrawableResource,
    val description: String,
    val photographer: String,
    val sourceUrl: String,
    val license: String,
    val licenseUrl: String,
    val alignment: Alignment = Alignment.Center,
)

internal val trailPhotographs: Map<String, TrailPhotograph> = mapOf(
    "half-dome" to TrailPhotograph(
        image = Res.drawable.trail_photo_half_dome,
        description = "Half Dome summit overlooking Yosemite Valley",
        photographer = "Doug Letterman",
        sourceUrl = "https://www.hikingproject.com/photo/7004494/over-the-edge-of-half-dome",
        license = "CC BY-SA 1.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/1.0/",
        alignment = Alignment.Center,
    ),
    "trolltunga" to TrailPhotograph(
        image = Res.drawable.trail_photo_trolltunga,
        description = "Trolltunga rock ledge above Ringedalsvatnet.",
        photographer = "TerjeN",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:View_of_trolltunga.jpg",
        license = "CC BY 3.0",
        licenseUrl = "https://creativecommons.org/licenses/by/3.0",
        alignment = Alignment.Center,
    ),
    "tongariro-alpine-crossing" to TrailPhotograph(
        image = Res.drawable.trail_photo_tongariro_alpine_crossing,
        description = "Red Crater and Mount Ngauruhoe on the Tongariro Alpine Crossing",
        photographer = "Karin Noresten",
        sourceUrl = "https://www.hikingproject.com/photo/7020599/the-red-crater-and-mt-ngauruhoe",
        license = "CC BY-SA 1.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/1.0/",
        alignment = Alignment.CenterEnd,
    ),
    "mount-fuji-via-yoshida-trail" to TrailPhotograph(
        image = Res.drawable.trail_photo_mount_fuji_via_yoshida_trail,
        description = "Descending Mount Fuji on the Yoshida Trail.",
        photographer = "Photos of Japan",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Mt._Fuji_Yoshida_Trail_Jun_21_621am.jpg",
        license = "CC0",
        licenseUrl = "https://creativecommons.org/publicdomain/zero/1.0/deed.en",
        alignment = Alignment.Center,
    ),
    "laguna-de-los-tres" to TrailPhotograph(
        image = Res.drawable.trail_photo_laguna_de_los_tres,
        description = "Fitz Roy massif reflected in Laguna de los Tres.",
        photographer = "Almonroth",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Laguna_de_los_Tres_color.jpg",
        license = "CC BY-SA 3.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/3.0",
        alignment = Alignment.Center,
    ),
    "platteklip-gorge" to TrailPhotograph(
        image = Res.drawable.trail_photo_platteklip_gorge,
        description = "Rocky steps on the Platteklip Gorge trail.",
        photographer = "Lovemedead",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Rock_path_on_Platteklip_Gorge_trail,_Table_Mountain_in_Cape_Town,_South_Africa_04.jpg",
        license = "CC BY-SA 4.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0",
        alignment = Alignment.Center,
    ),
    "plain-of-six-glaciers" to TrailPhotograph(
        image = Res.drawable.trail_photo_plain_of_six_glaciers,
        description = "Mountains and glacial terrain along the Plain of Six Glaciers.",
        photographer = "Florian Fuchs",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Plain_of_the_Six_Glaciers.jpg",
        license = "CC BY 3.0",
        licenseUrl = "https://creativecommons.org/licenses/by/3.0",
        alignment = Alignment.Center,
    ),
    "angels-landing" to TrailPhotograph(
        image = Res.drawable.trail_photo_angels_landing,
        description = "The summit ridge of Angels Landing in Zion.",
        photographer = "Mountain walrus",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Angel%27s_Landing_Summit.JPG",
        license = "CC BY-SA 3.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/3.0",
        alignment = Alignment.TopCenter,
    ),
    "preikestolen" to TrailPhotograph(
        image = Res.drawable.trail_photo_preikestolen,
        description = "Preikestolen above the Lysefjord.",
        photographer = "Andreas Tille",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Preikestolen.jpg",
        license = "CC BY-SA 4.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0",
        alignment = Alignment.Center,
    ),
    "roys-peak" to TrailPhotograph(
        image = Res.drawable.trail_photo_roys_peak,
        description = "Lake Wanaka seen from Roys Peak.",
        photographer = "Julrob Photography",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Roys_Peak_Mountain_near_Lake_Wanaka.jpg",
        license = "CC BY-SA 4.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0",
        alignment = Alignment.Center,
    ),
    "tiger-s-nest" to TrailPhotograph(
        image = Res.drawable.trail_photo_tiger_s_nest,
        description = "Tiger's Nest monastery on the cliffs above Paro.",
        photographer = "Nina R; derivative edit by UnpetitproleX",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Paro_Taktsang,_Bhutan_(edited).jpg",
        license = "CC BY 2.0",
        licenseUrl = "https://creativecommons.org/licenses/by/2.0",
        alignment = Alignment.Center,
    ),
    "base-de-las-torres" to TrailPhotograph(
        image = Res.drawable.trail_photo_base_de_las_torres,
        description = "The Torres del Paine towers above the lake at their base, in black and white.",
        photographer = "Marco Nürnberger",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Base_las_Torres_(23615678820).jpg",
        license = "CC BY 2.0",
        licenseUrl = "https://creativecommons.org/licenses/by/2.0",
        alignment = Alignment.TopCenter,
    ),
    "petra-monastery-trail" to TrailPhotograph(
        image = Res.drawable.trail_photo_petra_monastery_trail,
        description = "The rock-carved Monastery at Petra.",
        photographer = "Jordan Klein",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Petra_monestary.jpg",
        license = "CC BY 2.0",
        licenseUrl = "https://creativecommons.org/licenses/by/2.0",
        alignment = Alignment.TopCenter,
    ),
    "franconia-ridge-loop" to TrailPhotograph(
        image = Res.drawable.trail_photo_franconia_ridge_loop,
        description = "Franconia Ridge extending south from Mount Lincoln.",
        photographer = "Mike9827",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Looking_South_Down_the_Franconia_Ridge.jpg",
        license = "CC BY-SA 4.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0",
        alignment = Alignment.Center,
    ),
    "mist-trail-to-nevada-fall" to TrailPhotograph(
        image = Res.drawable.trail_photo_mist_trail_to_nevada_fall,
        description = "The Mist Trail beside Nevada Fall",
        photographer = "rmceoin",
        sourceUrl = "https://www.hikingproject.com/photo/7003983/mist-trail-and-nevada-fall",
        license = "CC BY-SA 1.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/1.0/",
        alignment = Alignment.Center,
    ),
    "tre-cime-di-lavaredo-loop" to TrailPhotograph(
        image = Res.drawable.trail_photo_tre_cime_di_lavaredo_loop,
        description = "Trail to Rifugio Locatelli in the Dolomites.",
        photographer = "Simone A. Bertinotti",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Tre_Cime_di_Lavaredo,_Sentiero_per_il_Rifugio_Locatelli.jpg",
        license = "CC BY-SA 4.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0",
        alignment = Alignment.Center,
    ),
    "hooker-valley-track" to TrailPhotograph(
        image = Res.drawable.trail_photo_hooker_valley_track,
        description = "The Hooker Valley Track beneath Mount Sefton and the Footstool.",
        photographer = "Jan Helebrant",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:NZ_Hooker_Valley_track.jpg",
        license = "CC0",
        licenseUrl = "https://creativecommons.org/publicdomain/zero/1.0/deed.en",
        alignment = Alignment.Center,
    ),
    "adam-s-peak" to TrailPhotograph(
        image = Res.drawable.trail_photo_adam_s_peak,
        description = "A view across the surrounding landscape from Adam’s Peak in Sri Lanka.",
        photographer = "CaCo789",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Adams_Peak,_Sri_Buddha.jpg",
        license = "CC BY-SA 4.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0",
        alignment = Alignment.Center,
    ),
    "laguna-69" to TrailPhotograph(
        image = Res.drawable.trail_photo_laguna_69,
        description = "Laguna 69 in Peru’s Cordillera Blanca.",
        photographer = "Esmée Winnubst",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Laguna_69,_July_22,_2017.jpg",
        license = "CC BY 2.0",
        licenseUrl = "https://creativecommons.org/licenses/by/2.0",
        alignment = Alignment.Center,
    ),
    "tugela-falls-via-sentinel-peak" to TrailPhotograph(
        image = Res.drawable.trail_photo_tugela_falls_via_sentinel_peak,
        description = "Tugela Falls in South Africa’s Drakensberg.",
        photographer = "Doc James",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:TelugaWaterFallApr2026.jpg",
        license = "CC BY-SA 4.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0",
        alignment = Alignment.Center,
    ),
    "lake-agnes-tea-house" to TrailPhotograph(
        image = Res.drawable.trail_photo_lake_agnes_tea_house,
        description = "Lake Agnes Tea House in Banff National Park.",
        photographer = "Wilson Hui from Calgary, Canada",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Lake_Agnes_Tea_House_(15464773570).jpg",
        license = "CC BY 2.0",
        licenseUrl = "https://creativecommons.org/licenses/by/2.0",
        alignment = Alignment.Center,
    ),
    "the-narrows" to TrailPhotograph(
        image = Res.drawable.trail_photo_the_narrows,
        description = "The Virgin River between the sandstone walls of the Zion Narrows.",
        photographer = "Jon Sullivan",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Zion_narrows_river.jpg",
        license = "Public domain",
        licenseUrl = "https://commons.wikimedia.org/wiki/File:Zion_narrows_river.jpg",
        alignment = Alignment.Center,
    ),
    "ben-nevis-mountain-track" to TrailPhotograph(
        image = Res.drawable.trail_photo_ben_nevis_mountain_track,
        description = "The Mountain Track ascending Ben Nevis.",
        photographer = "Chris Morgan",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Ascending_Mountain_Track_up_Ben_Nevis_-_geograph.org.uk_-_4534313.jpg",
        license = "CC BY-SA 2.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/2.0",
        alignment = Alignment.CenterEnd,
    ),
    "mount-kosciuszko-via-thredbo" to TrailPhotograph(
        image = Res.drawable.trail_photo_mount_kosciuszko_via_thredbo,
        description = "The summit path on Mount Kosciuszko.",
        photographer = "P. Lu",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Mount_Kosciuszko_Summit_Path_-_panoramio.jpg",
        license = "CC BY-SA 3.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/3.0",
        alignment = Alignment.Center,
    ),
    "dragon-s-back" to TrailPhotograph(
        image = Res.drawable.trail_photo_dragon_s_back,
        description = "Dragon’s Back in Hong Kong.",
        photographer = "ChInG_*",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Dragon%27s_Back,_Hong_Kong_01.jpg",
        license = "CC BY-SA 2.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/2.0",
        alignment = Alignment.Center,
    ),
    "huayna-picchu" to TrailPhotograph(
        image = Res.drawable.trail_photo_huayna_picchu,
        description = "Huayna Picchu above Machu Picchu and the Urubamba valley.",
        photographer = "Jorge Láscar from Australia",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Lascar_Machu_Picchu,_the_Urubamba_river_and_Huayna_Picchu_(4548173955).jpg",
        license = "CC BY 2.0",
        licenseUrl = "https://creativecommons.org/licenses/by/2.0",
        alignment = Alignment.TopCenter,
    ),
    "wadi-shab" to TrailPhotograph(
        image = Res.drawable.trail_photo_wadi_shab,
        description = "Wadi Shab in Oman.",
        photographer = "Uhooep",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Wadi_Shab,_Oman.jpg",
        license = "CC BY-SA 4.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0",
        alignment = Alignment.Center,
    ),
    "garibaldi-lake" to TrailPhotograph(
        image = Res.drawable.trail_photo_garibaldi_lake,
        description = "Garibaldi Lake with mountains and glaciers beyond.",
        photographer = "Vinay.yo",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Garibaldi_lake_wide.jpg",
        license = "CC BY-SA 4.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0",
        alignment = Alignment.Center,
    ),
    "bright-angel-trail-to-havasupai-gardens" to TrailPhotograph(
        image = Res.drawable.trail_photo_bright_angel_trail_to_havasupai_gardens,
        description = "Bright Angel Trail near the South Rim of the Grand Canyon.",
        photographer = "Sharon Mollerus",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Bright_Angel_Trail,_South_Rim,_Grand_Canyon_(30158881123).jpg",
        license = "CC BY 2.0",
        licenseUrl = "https://creativecommons.org/licenses/by/2.0",
        alignment = Alignment.Center,
    ),
    "yr-wyddfa-snowdon-llanberis-path" to TrailPhotograph(
        image = Res.drawable.trail_photo_yr_wyddfa_snowdon_llanberis_path,
        description = "The Llanberis Path descending from Yr Wyddfa.",
        photographer = "Chris Morgan",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Descending_Llanberis_path_from_Snowdon_-_geograph.org.uk_-_4101552.jpg",
        license = "CC BY-SA 2.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/2.0",
        alignment = Alignment.Center,
    ),
    "bondi-to-coogee-coastal-walk" to TrailPhotograph(
        image = Res.drawable.trail_photo_bondi_to_coogee_coastal_walk,
        description = "The coast at Mackenzies Bay along the Bondi coastal walk.",
        photographer = "Maurice van Creij",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Bondi_coastal_walk_-_panoramio_(1).jpg",
        license = "CC BY 3.0",
        licenseUrl = "https://creativecommons.org/licenses/by/3.0",
        alignment = Alignment.Center,
    ),
    "kawah-ijen" to TrailPhotograph(
        image = Res.drawable.trail_photo_kawah_ijen,
        description = "The crater lake at Kawah Ijen.",
        photographer = "Aditya Suseno",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Kawah_Ijen_Acid_Lake.jpg",
        license = "CC0",
        licenseUrl = "https://creativecommons.org/publicdomain/zero/1.0/deed.en",
        alignment = Alignment.Center,
    ),
    "quilotoa-crater-rim-loop" to TrailPhotograph(
        image = Res.drawable.trail_photo_quilotoa_crater_rim_loop,
        description = "The lake inside Quilotoa’s crater.",
        photographer = "Lombax89 at English Wikipedia",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Quilotoa_Crater.jpg",
        license = "Public domain",
        licenseUrl = "https://commons.wikimedia.org/wiki/File:Quilotoa_Crater.jpg",
        alignment = Alignment.Center,
    ),
    "alum-cave-trail-to-mount-leconte" to TrailPhotograph(
        image = Res.drawable.trail_photo_alum_cave_trail_to_mount_leconte,
        description = "A mountain vista from the upper Alum Cave Trail.",
        photographer = "Andrew Heneen",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Alum_Cave_Trail_2.jpg",
        license = "CC BY 4.0",
        licenseUrl = "https://creativecommons.org/licenses/by/4.0",
        alignment = Alignment.Center,
    ),
    "upper-yosemite-fall-trail" to TrailPhotograph(
        image = Res.drawable.trail_photo_upper_yosemite_fall_trail,
        description = "Upper Yosemite Falls above Yosemite Valley.",
        photographer = "InSapphoWeTrust",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Upper_Yosemite_Falls_(5637225900).jpg",
        license = "CC BY-SA 2.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/2.0",
        alignment = Alignment.Center,
    ),
    "caminito-del-rey" to TrailPhotograph(
        image = Res.drawable.trail_photo_caminito_del_rey,
        description = "Caminito del Rey in the gorge at El Chorro.",
        photographer = "Diego Delso",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Caminito_del_Rey,_M%C3%A1laga,_Espa%C3%B1a,_2023-05-18,_DD_12.jpg",
        license = "CC BY-SA 4.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0",
        alignment = Alignment.Center,
    ),
    "mount-takao-trail-1" to TrailPhotograph(
        image = Res.drawable.trail_photo_mount_takao_trail_1,
        description = "The summit of Mount Takao.",
        photographer = "Guilhem Vellut",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Summit_of_Mount_Takao_(11094693124).jpg",
        license = "CC BY 2.0",
        licenseUrl = "https://creativecommons.org/licenses/by/2.0",
        alignment = Alignment.Center,
    ),
    "highline-trail" to TrailPhotograph(
        image = Res.drawable.trail_photo_highline_trail,
        description = "Highline Trail in Glacier National Park.",
        photographer = "Katie Brady",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Glacier_Park_Highline_Trail.jpg",
        license = "CC BY 2.0",
        licenseUrl = "https://creativecommons.org/licenses/by/2.0",
        alignment = Alignment.Center,
    ),
    "samaria-gorge" to TrailPhotograph(
        image = Res.drawable.trail_photo_samaria_gorge,
        description = "A panorama of Samaria Gorge on Crete.",
        photographer = "Irini Tzagournisaki",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Samaria_Gorge.jpg",
        license = "CC BY-SA 4.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0",
        alignment = Alignment.Center,
    ),
    "delicate-arch-trail" to TrailPhotograph(
        image = Res.drawable.trail_photo_delicate_arch_trail,
        description = "Delicate Arch in Arches National Park.",
        photographer = "Philippe Lavoie",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Delicate_arch.jpg",
        license = "Public domain",
        licenseUrl = "https://commons.wikimedia.org/wiki/File:Delicate_arch.jpg",
        alignment = Alignment.TopCenter,
    ),
    "lac-blanc" to TrailPhotograph(
        image = Res.drawable.trail_photo_lac_blanc,
        description = "Lac Blanc in the mountains above Chamonix.",
        photographer = "Tiia Monto",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Chamonix_-_Lac_Blanc.jpg",
        license = "CC BY-SA 3.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/3.0",
        alignment = Alignment.Center,
    ),
    "diamond-head-summit-trail" to TrailPhotograph(
        image = Res.drawable.trail_photo_diamond_head_summit_trail,
        description = "The eastward view from Diamond Head summit.",
        photographer = "Famartin",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:2021-10-11_15_08_54_View_east_from_the_summit_of_Diamond_Head_within_Diamond_Head_State_Monument_in_Honolulu,_Oahu,_Hawaii.jpg",
        license = "CC BY-SA 4.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0",
        alignment = Alignment.Center,
    ),
    "classic-inca-trail" to TrailPhotograph(
        image = Res.drawable.trail_photo_classic_inca_trail,
        description = "Stone paving on the Inca Trail to Machu Picchu.",
        photographer = "Bcasterline at English Wikipedia",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Inca_trail,_Peru.jpg",
        license = "Public domain",
        licenseUrl = "https://commons.wikimedia.org/wiki/File:Inca_trail,_Peru.jpg",
        alignment = Alignment.Center,
    ),
    "everest-base-camp-trek" to TrailPhotograph(
        image = Res.drawable.trail_photo_everest_base_camp_trek,
        description = "Everest Base Camp on the Nepal side of the mountain.",
        photographer = "Billjones94",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Everest_Base_camp_in_Nepal,_photographed_on_November_29,_2023.jpg",
        license = "CC0",
        licenseUrl = "https://creativecommons.org/publicdomain/zero/1.0/deed.en",
        alignment = Alignment.Center,
    ),
    "tour-du-mont-blanc" to TrailPhotograph(
        image = Res.drawable.trail_photo_tour_du_mont_blanc,
        description = "The Tour du Mont Blanc trail near Col des Montets.",
        photographer = "Rémih",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Tour_du_Mont-Blanc_@_Col_des_Montets_01.jpg",
        license = "CC BY-SA 4.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0",
        alignment = Alignment.Center,
    ),
    "milford-track" to TrailPhotograph(
        image = Res.drawable.trail_photo_milford_track,
        description = "Mackinnon Pass seen from the Milford Track.",
        photographer = "AlasdairW",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Milford_Track_Mackinnon_Pass.jpg",
        license = "CC BY-SA 3.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/3.0",
        alignment = Alignment.Center,
    ),
    "torres-del-paine-w-trek" to TrailPhotograph(
        image = Res.drawable.trail_photo_torres_del_paine_w_trek,
        description = "Trees in the French Valley of Torres del Paine.",
        photographer = "Cachulooo",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Torres_de_Paine_Valle_Frances_arboles.jpg",
        license = "CC BY-SA 4.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0",
        alignment = Alignment.Center,
    ),
    "kilimanjaro-machame-route" to TrailPhotograph(
        image = Res.drawable.trail_photo_kilimanjaro_machame_route,
        description = "Machame Gate at the start of the Kilimanjaro route.",
        photographer = "BerotBurns",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Machame_Gate,_Mt._Kilimanjaro.jpg",
        license = "CC BY-SA 3.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/3.0",
        alignment = Alignment.TopCenter,
    ),
    "laugavegur-trail" to TrailPhotograph(
        image = Res.drawable.trail_photo_laugavegur_trail,
        description = "Landscape along the Laugavegur Trail in Iceland.",
        photographer = "Michal Klajban",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Laugavegur_hiking_trail,_Iceland_05.jpg",
        license = "CC BY-SA 4.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0",
        alignment = Alignment.Center,
    ),
    "overland-track" to TrailPhotograph(
        image = Res.drawable.trail_photo_overland_track,
        description = "Mountain scenery along Tasmania’s Overland Track.",
        photographer = "Quinn Kacic-Midson",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:Overland_Track_Landscape,_Tasmania.jpg",
        license = "CC BY-SA 4.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0",
        alignment = Alignment.Center,
    ),
)
