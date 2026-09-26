# Trail photography

All 50 catalog trails have bundled photos available offline. Three photos come
from Hiking Project pages that explicitly identify Creative Commons licenses.
The remaining 47 are openly licensed or public-domain Wikimedia Commons photos.
Each photo shows the named route, a shared route segment, its destination, or a
view from it. The manifest records which applies to each photo.

Compose crops each photo for display, positioning it to keep summits, landmarks,
and paths visible. Source JPEG bytes are unchanged. Each photo retains its own
license, separate from the application code license. Photographer credits,
including edited-source credits and source/license links, appear on trail details
and Welcome. Cards and collection covers show only the photograph.

The bundled [manifest](src/commonMain/composeResources/files/trail_photography.json)
records source and image URLs, photographer, title, license, match notes, and
SHA-256 for every resource. Photos are mapped by stable trail IDs, so persisted
records and saved memberships need no reseed. Legacy `photoIndex` remains
serialized for compatibility and is no longer used by the UI.

| Trail | Photographer | License | Source |
| --- | --- | --- | --- |
| Half Dome | Doug Letterman | [CC BY-SA 1.0](https://creativecommons.org/licenses/by-sa/1.0/) | [Hiking Project](https://www.hikingproject.com/photo/7004494/over-the-edge-of-half-dome) |
| Trolltunga | TerjeN | [CC BY 3.0](https://creativecommons.org/licenses/by/3.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:View_of_trolltunga.jpg) |
| Tongariro Alpine Crossing | Karin Noresten | [CC BY-SA 1.0](https://creativecommons.org/licenses/by-sa/1.0/) | [Hiking Project](https://www.hikingproject.com/photo/7020599/the-red-crater-and-mt-ngauruhoe) |
| Mount Fuji via Yoshida Trail | Photos of Japan | [CC0](https://creativecommons.org/publicdomain/zero/1.0/deed.en) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Mt._Fuji_Yoshida_Trail_Jun_21_621am.jpg) |
| Laguna de los Tres | Almonroth | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Laguna_de_los_Tres_color.jpg) |
| Platteklip Gorge | Lovemedead | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Rock_path_on_Platteklip_Gorge_trail,_Table_Mountain_in_Cape_Town,_South_Africa_04.jpg) |
| Plain of Six Glaciers | Florian Fuchs | [CC BY 3.0](https://creativecommons.org/licenses/by/3.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Plain_of_the_Six_Glaciers.jpg) |
| Angels Landing | Mountain walrus | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Angel%27s_Landing_Summit.JPG) |
| Preikestolen | Andreas Tille | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Preikestolen.jpg) |
| Roys Peak | Julrob Photography | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Roys_Peak_Mountain_near_Lake_Wanaka.jpg) |
| Tiger's Nest | Nina R; derivative edit by UnpetitproleX | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Paro_Taktsang,_Bhutan_(edited).jpg) |
| Base de las Torres | Marco Nürnberger | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Base_las_Torres_(23615678820).jpg) |
| Petra Monastery Trail | Jordan Klein | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Petra_monestary.jpg) |
| Franconia Ridge Loop | Mike9827 | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Looking_South_Down_the_Franconia_Ridge.jpg) |
| Mist Trail to Nevada Fall | rmceoin | [CC BY-SA 1.0](https://creativecommons.org/licenses/by-sa/1.0/) | [Hiking Project](https://www.hikingproject.com/photo/7003983/mist-trail-and-nevada-fall) |
| Tre Cime di Lavaredo Loop | Simone A. Bertinotti | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Tre_Cime_di_Lavaredo,_Sentiero_per_il_Rifugio_Locatelli.jpg) |
| Hooker Valley Track | Jan Helebrant | [CC0](https://creativecommons.org/publicdomain/zero/1.0/deed.en) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:NZ_Hooker_Valley_track.jpg) |
| Adam's Peak | CaCo789 | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Adams_Peak,_Sri_Buddha.jpg) |
| Laguna 69 | Esmée Winnubst | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Laguna_69,_July_22,_2017.jpg) |
| Tugela Falls via Sentinel Peak | Doc James | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:TelugaWaterFallApr2026.jpg) |
| Lake Agnes Tea House | Wilson Hui from Calgary, Canada | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Lake_Agnes_Tea_House_(15464773570).jpg) |
| The Narrows | Jon Sullivan | [Public domain](https://commons.wikimedia.org/wiki/File:Zion_narrows_river.jpg) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Zion_narrows_river.jpg) |
| Ben Nevis Mountain Track | Chris Morgan | [CC BY-SA 2.0](https://creativecommons.org/licenses/by-sa/2.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Ascending_Mountain_Track_up_Ben_Nevis_-_geograph.org.uk_-_4534313.jpg) |
| Mount Kosciuszko via Thredbo | P. Lu | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Mount_Kosciuszko_Summit_Path_-_panoramio.jpg) |
| Dragon's Back | ChInG_* | [CC BY-SA 2.0](https://creativecommons.org/licenses/by-sa/2.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Dragon%27s_Back,_Hong_Kong_01.jpg) |
| Huayna Picchu | Jorge Láscar from Australia | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Lascar_Machu_Picchu,_the_Urubamba_river_and_Huayna_Picchu_(4548173955).jpg) |
| Wadi Shab | Uhooep | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Wadi_Shab,_Oman.jpg) |
| Garibaldi Lake | Vinay.yo | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Garibaldi_lake_wide.jpg) |
| Bright Angel Trail to Havasupai Gardens | Sharon Mollerus | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Bright_Angel_Trail,_South_Rim,_Grand_Canyon_(30158881123).jpg) |
| Yr Wyddfa (Snowdon) Llanberis Path | Chris Morgan | [CC BY-SA 2.0](https://creativecommons.org/licenses/by-sa/2.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Descending_Llanberis_path_from_Snowdon_-_geograph.org.uk_-_4101552.jpg) |
| Bondi to Coogee Coastal Walk | Maurice van Creij | [CC BY 3.0](https://creativecommons.org/licenses/by/3.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Bondi_coastal_walk_-_panoramio_(1).jpg) |
| Kawah Ijen | Aditya Suseno | [CC0](https://creativecommons.org/publicdomain/zero/1.0/deed.en) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Kawah_Ijen_Acid_Lake.jpg) |
| Quilotoa Crater Rim Loop | Lombax89 at English Wikipedia | [Public domain](https://commons.wikimedia.org/wiki/File:Quilotoa_Crater.jpg) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Quilotoa_Crater.jpg) |
| Alum Cave Trail to Mount LeConte | Andrew Heneen | [CC BY 4.0](https://creativecommons.org/licenses/by/4.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Alum_Cave_Trail_2.jpg) |
| Upper Yosemite Fall Trail | InSapphoWeTrust | [CC BY-SA 2.0](https://creativecommons.org/licenses/by-sa/2.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Upper_Yosemite_Falls_(5637225900).jpg) |
| Caminito del Rey | Diego Delso | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Caminito_del_Rey,_M%C3%A1laga,_Espa%C3%B1a,_2023-05-18,_DD_12.jpg) |
| Mount Takao Trail 1 | Guilhem Vellut | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Summit_of_Mount_Takao_(11094693124).jpg) |
| Highline Trail | Katie Brady | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Glacier_Park_Highline_Trail.jpg) |
| Samaria Gorge | Irini Tzagournisaki | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Samaria_Gorge.jpg) |
| Delicate Arch Trail | Philippe Lavoie | [Public domain](https://commons.wikimedia.org/wiki/File:Delicate_arch.jpg) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Delicate_arch.jpg) |
| Lac Blanc | Tiia Monto | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Chamonix_-_Lac_Blanc.jpg) |
| Diamond Head Summit Trail | Famartin | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:2021-10-11_15_08_54_View_east_from_the_summit_of_Diamond_Head_within_Diamond_Head_State_Monument_in_Honolulu,_Oahu,_Hawaii.jpg) |
| Classic Inca Trail | Bcasterline at English Wikipedia | [Public domain](https://commons.wikimedia.org/wiki/File:Inca_trail,_Peru.jpg) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Inca_trail,_Peru.jpg) |
| Everest Base Camp Trek | Billjones94 | [CC0](https://creativecommons.org/publicdomain/zero/1.0/deed.en) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Everest_Base_camp_in_Nepal,_photographed_on_November_29,_2023.jpg) |
| Tour du Mont Blanc | Rémih | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Tour_du_Mont-Blanc_@_Col_des_Montets_01.jpg) |
| Milford Track | AlasdairW | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Milford_Track_Mackinnon_Pass.jpg) |
| Torres del Paine W Trek | Cachulooo | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Torres_de_Paine_Valle_Frances_arboles.jpg) |
| Kilimanjaro Machame Route | BerotBurns | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Machame_Gate,_Mt._Kilimanjaro.jpg) |
| Laugavegur Trail | Michal Klajban | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Laugavegur_hiking_trail,_Iceland_05.jpg) |
| Overland Track | Quinn Kacic-Midson | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Overland_Track_Landscape,_Tasmania.jpg) |
