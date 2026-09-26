# Trail catalog

The sample contains 50 real routes: 42 day hikes followed by eight multi-day
itineraries. Half Dome and Trolltunga are first. Stable IDs join catalog rows,
saved memberships, feeds, and bundled photographs. Catalog order supplies the
**Most popular** sort. It does not imply personalization.

Ratings, review counts, and review excerpts are sample data. Route
descriptions are summaries. Distances are rounded to 100 m. Return routes include
both directions. Point-to-point figures cover one direction. Day-hike durations
are estimates. Multi-day durations encode itinerary days as `days * 1440`,
including overnight time rather than continuous movement.

The eight itineraries use `STRENUOUS`. Day-hike grades follow their sources.
Every route has `HIKING`, and the eight itineraries also have `BACKPACKING`.
Dog-friendly is a positive fixture tag, not a guarantee about current access.
No highest-point or kid-friendly attribute is inferred from these sources.

The source review was recorded on September 18, 2026. Not every field on every
route was independently reverified. The source limitations below describe those
gaps. Source links do not establish current access, conditions, or the precision
of every stored value. The source research record had SHA-256
`c07537016be9db2a3f9a24bf6716074e56e974300bf95733174b27fd7e7592f3`.

Photographs are bundled by trail ID. Their separate sources, match notes,
licenses, and byte hashes are in the [photography documentation](../multiplatform/ui/trail/TRAIL_PHOTOS.md).
The serialized `photoIndex` field remains for compatibility and does not choose
the current image.

## Ordered routes and sources

`+m` is the stored ascent estimate. The source notes below describe its precision
and provenance. Feature abbreviations: **L** lake, **F** forest, **W** waterfall,
**S** summit or named high viewpoint, **O** loop, **D** dog friendly. A dash means
none of the defined features applies. It does not mean an absence of scenery.
The Trail model has no route-type field. The route column identifies the variant
whose distance is listed.

| # | Trail | Region | Route variant | km | +m | Time | Grade | Features | Sources |
| --- | --- | --- | --- | ---: | ---: | --- | --- | --- | --- |
| 1 | Half Dome | Yosemite National Park, USA | Return via Mist Trail | 22.7 | 1463 | 11 h | Hard | W S F | [nps.gov](https://www.nps.gov/yose/planyourvisit/halfdome.htm) |
| 2 | Trolltunga | Hardangerfjord, Norway | Return from P2 Skjeggedal | 27.0 | 800 | 10 h | Hard | L S D | [trolltunga.com](https://www.trolltunga.com/en); [trolltunga.com](https://www.trolltunga.com/en/plan-your-hike/the-hike-to-trolltunga) |
| 3 | Tongariro Alpine Crossing | Tongariro National Park, New Zealand | One way | 19.4 | 765 | 7.5 h | Hard | L S | [doc.govt.nz](https://www.doc.govt.nz/parks-and-recreation/places-to-go/central-north-island/places/tongariro-national-park/things-to-do/tracks/tongariro-alpine-crossing/); [en.wikipedia.org](https://en.wikipedia.org/wiki/Tongariro_Alpine_Crossing) |
| 4 | Mount Fuji via Yoshida Trail | Fuji-Hakone-Izu National Park, Japan | Loop via ascent/descent paths | 13.8 | 1470 | 10 h | Hard | S O | [fujisan-climb.jp](https://www.fujisan-climb.jp/en/comparison-of-routes/) |
| 5 | Laguna de los Tres | Los Glaciares National Park, Argentina | Return | 22.5 | 1050 | 9 h | Hard | L F S | [alltrails.com](https://www.alltrails.com/trail/argentina/santa-cruz/laguna-de-los-tres-via-sendero-al-fitz-roy); [argentina.gob.ar](https://www.argentina.gob.ar/parquesnacionales/patagonia-austral/recomendaciones-para-visitar-el-parque-nacional-los-glaciares) |
| 6 | Platteklip Gorge | Table Mountain National Park, South Africa | One way to upper cable station | 4.5 | 655 | 3 h | Hard | S | [alltrails.com](https://www.alltrails.com/trail/south-africa/western-cape/platteklip-gorge-to-upper-cable-station); [hiketablemountain.co.za](https://hiketablemountain.co.za/hiking-routes-table-mountain/platteklip-gorge/) |
| 7 | Plain of Six Glaciers | Banff National Park, Canada | Return to tea house | 10.6 | 365 | 4.5 h | Moderate | L F D | [parks.canada.ca](https://parks.canada.ca/pn-np/ab/banff/activ/randonnee-hiking/lakelouise) |
| 8 | Angels Landing | Zion National Park, USA | Return | 8.7 | 454 | 4 h | Hard | S | [nps.gov](https://www.nps.gov/zion/planyourvisit/angels-landing-hiking-permits.htm); [en.wikipedia.org](https://en.wikipedia.org/wiki/Angels_Landing) |
| 9 | Preikestolen | Lysefjord, Norway | Return | 8.0 | 350 | 4 h | Moderate | F S D | [preikestolen365.com](https://preikestolen365.com/hiking-to-preikestolen/); [preikestolen365.com](https://preikestolen365.com/wp-content/uploads/2023/09/Digital-Map-Preikestolen.pdf) |
| 10 | Roys Peak | Wānaka, New Zealand | Return | 16.0 | 1228 | 5.5 h | Hard | S L | [doc.govt.nz](https://www.doc.govt.nz/parks-and-recreation/places-to-go/otago/places/wanaka-area/things-to-do/roys-peak-track/); [alltrails.com](https://www.alltrails.com/trail/new-zealand/otago/roys-peak-track) |
| 11 | Tiger's Nest | Paro Valley, Bhutan | Return | 6.5 | 600 | 4.5 h | Moderate | F W S | [alltrails.com](https://www.alltrails.com/en-gb/trail/bhutan/paro/tigers-nest-trail); [drukasia.com](https://www.drukasia.com/bhutan/paro/taktsang-monastery/) |
| 12 | Base de las Torres | Torres del Paine, Chile | Return | 20.6 | 1025 | 8.5 h | Hard | L F S | [alltrails.com](https://www.alltrails.com/trail/chile/magallanes/mirador-torres-del-paine-via-sendero-las-torres-a-chileno); [parquetorresdelpaine.cl](https://parquetorresdelpaine.cl/actividades-no-autorizadas/) |
| 13 | Petra Monastery Trail | Petra, Jordan | Return from visitor centre | 12.1 | 592 | 5 h | Moderate | S | [visitpetra.jo](https://www.visitpetra.jo/en/Trails); [alltrails.com](https://www.alltrails.com/trail/jordan/ma-an--2/petra-monastery-trail-ad-deir-trail) |
| 14 | Franconia Ridge Loop | White Mountains, New Hampshire, USA | Loop | 13.5 | 1190 | 7 h | Hard | S W F O D | [fastestknowntime.com](https://fastestknowntime.com/route/franconia-ridge-loop-nh); [earthtrekkers.com](https://www.earthtrekkers.com/franconia-ridge-loop-trail/) |
| 15 | Mist Trail to Nevada Fall | Yosemite National Park, USA | Return | 8.7 | 610 | 5.5 h | Hard | W F | [nps.gov](https://www.nps.gov/yose/planyourvisit/vernalnevadatrail.htm); [nps.gov](https://www.nps.gov/yose/planyourvisit/pets.htm) |
| 16 | Tre Cime di Lavaredo Loop | Dolomites, Italy | Loop | 10.0 | 400 | 4 h | Moderate | O L S D | [fullsuitcase.com](https://fullsuitcase.com/tre-cime-di-lavaredo-hike/); [dolomiti.it](https://www.dolomiti.it/en/auronzo-misurina/news/toll-road-tre-cime-di-lavaredo-open) |
| 17 | Hooker Valley Track | Aoraki/Mount Cook National Park, New Zealand | Return | 10.0 | 130 | 3 h | Easy | L | [doc.govt.nz](https://www.doc.govt.nz/hooker-valley-track); [alltrails.com](https://www.alltrails.com/trail/new-zealand/canterbury/hooker-lake-via-hooker-valley-track) |
| 18 | Adam's Peak | Peak Wilderness Sanctuary, Sri Lanka | Return | 9.5 | 966 | 5.5 h | Hard | S F | [alltrails.com](https://www.alltrails.com/en-gb/trail/sri-lanka/nuwara-eliya/adams-peak); [sripada.org](http://sripada.org/nallathanni_trail.htm) |
| 19 | Laguna 69 | Huascarán National Park, Peru | Return | 13.7 | 830 | 5.5 h | Hard | L W | [alltrails.com](https://www.alltrails.com/trail/peru/ancash/sendero-laguna-69); [visitaareasnaturales.sernanp.gob.pe](https://visitaareasnaturales.sernanp.gob.pe/en/anps/national-park-huascaran/) |
| 20 | Tugela Falls via Sentinel Peak | Royal Natal, Drakensberg, South Africa | Return | 12.4 | 472 | 6 h | Moderate | W S | [alltrails.com](https://www.alltrails.com/trail/south-africa/kwazulu-natal/tugela-falls-hike-via-sentinel-peak); [strayalongtheway.com](https://www.strayalongtheway.com/sentinel-peak-hike/) |
| 21 | Lake Agnes Tea House | Banff National Park, Canada | Return | 6.8 | 385 | 2.5 h | Moderate | L F W D | [parks.canada.ca](https://parks.canada.ca/pn-np/ab/banff/activ/randonnee-hiking/lakelouise) |
| 22 | The Narrows | Zion National Park, USA | Return to Big Spring | 16.0 | 100 | 7 h | Hard | — | [nps.gov](https://www.nps.gov/zion/planyourvisit/thenarrows.htm); [hikingproject.com](https://www.hikingproject.com/trail/7001726/the-narrows-bottom-up-route) |
| 23 | Ben Nevis Mountain Track | Lochaber, Scotland | Return | 17.0 | 1345 | 8 h | Hard | S L D | [visitscotland.com](https://www.visitscotland.com/things-to-do/outdoor-activities/walking/mountains-hills/ben-nevis); [en.wikipedia.org](https://en.wikipedia.org/wiki/Ben_Nevis) |
| 24 | Mount Kosciuszko via Thredbo | Kosciuszko National Park, Australia | Return from upper chairlift | 13.0 | 300 | 4.5 h | Moderate | S L | [trailhiking.com.au](https://www.trailhiking.com.au/hikes/thredbo-to-mount-kosciuszko-hike/); [alltrails.com](https://www.alltrails.com/trail/australia/new-south-wales/mount-kosciuszko-summit-via-thredbo) |
| 25 | Dragon's Back | Shek O Country Park, Hong Kong | One way to Big Wave Bay | 8.0 | 490 | 3 h | Moderate | S F D | [roundislandtrail.gov.hk](https://www.roundislandtrail.gov.hk/en/exploring-the-trail/route-suggestions/hong-kong-trail-section-8-to-tei-wan-to-tai-long-wan); [discoverhongkong.com](https://www.discoverhongkong.com/eng/place-to-go/travel.guide-dragon-s-back.html) |
| 26 | Huayna Picchu | Machu Picchu, Peru | Return including citadel approach | 4.0 | 300 | 2.5 h | Moderate | S | [alltrails.com](https://www.alltrails.com/trail/peru/cusco/circuito-huayna-picchu); [en.wikipedia.org](https://en.wikipedia.org/wiki/Huayna_Picchu) |
| 27 | Wadi Shab | Wadi Shab, Oman | Return to pools; waterfall needs swim | 5.0 | 85 | 2.5 h | Easy | W | [alltrails.com](https://www.alltrails.com/trail/oman/ash-sharqiyah-south/wadi-shab); [walkmyworld.com](https://www.walkmyworld.com/posts/wadi-shab) |
| 28 | Garibaldi Lake | Garibaldi Provincial Park, Canada | Return | 18.0 | 820 | 5.5 h | Moderate | L F | [bcparks.ca](https://bcparks.ca/garibaldi-park/); [vancouvertrails.com](https://www.vancouvertrails.com/trails/garibaldi-lake/) |
| 29 | Bright Angel Trail to Havasupai Gardens | Grand Canyon National Park, USA | Return | 14.5 | 927 | 7.5 h | Hard | — | [nps.gov](https://www.nps.gov/grca/planyourvisit/upload/bright_angel_trail.pdf) |
| 30 | Yr Wyddfa (Snowdon) Llanberis Path | Eryri (Snowdonia), Wales | Return | 14.5 | 975 | 7 h | Hard | S D | [visitsnowdonia.info](https://www.visitsnowdonia.info/snowdon-walking-routes); [threepeakschallenge.uk](https://www.threepeakschallenge.uk/national-three-peaks-challenge/snowdon-yr-wyddfa/routes/llanberis-path) |
| 31 | Bondi to Coogee Coastal Walk | Sydney, Australia | One way coastal path | 6.0 | 160 | 2.5 h | Easy | D | [sydney.com](https://www.sydney.com/things-to-do/nature-and-parks/walks/bondi-to-coogee-coastal-walk); [alltrails.com](https://www.alltrails.com/trail/australia/new-south-wales/bondi-beach-to-coogee-beach-walk) |
| 32 | Kawah Ijen | Ijen, East Java, Indonesia | Return; crater access conditional | 9.5 | 592 | 4.5 h | Moderate | L S | [alltrails.com](https://www.alltrails.com/trail/indonesia/jawa-timur/kawah-ijen-volcano); [kawahijen.org](https://kawahijen.org/hours-and-fees/) |
| 33 | Quilotoa Crater Rim Loop | Quilotoa, Ecuador | Crater rim loop | 10.1 | 795 | 4.5 h | Moderate | L O | [alltrails.com](https://www.alltrails.com/trail/ecuador/cotopaxi/laguna-quilotoa); [en.wikipedia.org](https://en.wikipedia.org/wiki/Quilotoa) |
| 34 | Alum Cave Trail to Mount LeConte | Great Smoky Mountains National Park, USA | Return | 16.1 | 880 | 6.5 h | Hard | S F | [nps.gov](https://www.nps.gov/thingstodo/leconte-via-alum-cave-trail.htm); [alltrails.com](https://www.alltrails.com/trail/us/tennessee/myrtle-point-and-mount-leconte-via-alum-cave-trail) |
| 35 | Upper Yosemite Fall Trail | Yosemite National Park, USA | Return | 11.6 | 823 | 7 h | Hard | W S | [nps.gov](https://www.nps.gov/yose/planyourvisit/yosemitefallstrail.htm); [nps.gov](https://www.nps.gov/yose/planyourvisit/pets.htm) |
| 36 | Caminito del Rey | El Chorro, Andalusia, Spain | One way | 7.7 | 250 | 3 h | Easy | F | [caminitodelrey.info](https://www.caminitodelrey.info/en/your-visit/faq); [en.wikipedia.org](https://en.wikipedia.org/wiki/Caminito_del_Rey) |
| 37 | Mount Takao Trail 1 | Takao, Tokyo, Japan | Return on Trail 1 | 7.6 | 411 | 3 h | Easy | F S D | [takaotozan.co.jp](https://www.takaotozan.co.jp/course/); [kankyo1.metro.tokyo.lg.jp](https://www.kankyo1.metro.tokyo.lg.jp/naturepark/english/know/rule/takaosan.html) |
| 38 | Highline Trail | Glacier National Park, USA | One way Logan Pass to The Loop | 19.0 | 594 | 6 h | Hard | — | [hikinginglacier.com](https://www.hikinginglacier.com/highline-loop.htm); [earthtrekkers.com](https://www.earthtrekkers.com/highline-trail-hike-logan-pass-to-the-loop-glacier/) |
| 39 | Samaria Gorge | Crete, Greece | One way Xyloskalo to Agia Roumeli | 16.0 | 100 | 6.5 h | Moderate | F D | [samaria.gr](https://www.samaria.gr/tips-for-crossing-samaria/); [outdooractive.com](https://www.outdooractive.com/en/route/hiking-trail/chania/samaria-gorge/114819052/); [samaria-tickets.necca.gov.gr](https://samaria-tickets.necca.gov.gr/checkout) |
| 40 | Delicate Arch Trail | Arches National Park, USA | Return | 4.8 | 146 | 2.5 h | Moderate | — | [nps.gov](https://www.nps.gov/arch/planyourvisit/delicate-arch.htm) |
| 41 | Lac Blanc | Chamonix, France | Return from upper La Flégère | 7.0 | 500 | 3 h | Hard | L | [en.chamonix.com](https://en.chamonix.com/randonnees-en-famille/hike-to-the-lac-blanc-from-la-flegere) |
| 42 | Diamond Head Summit Trail | Oʻahu, Hawaiʻi, USA | Return | 2.6 | 171 | 1.5 h | Moderate | S | [dlnr.hawaii.gov](https://dlnr.hawaii.gov/dsp/parks/oahu/diamond-head-state-monument/); [dlnr.hawaii.gov](https://dlnr.hawaii.gov/dsp/files/2014/09/2015-Parks-Brochure.pdf); [gostateparks.hawaii.gov](https://gostateparks.hawaii.gov/diamondhead/faq) |
| 43 | Classic Inca Trail | Machu Picchu, Peru | One way | 42.0 | 2500 | 4 days | Strenuous | F S | [en.wikipedia.org](https://en.wikipedia.org/wiki/Inca_Trail_to_Machu_Picchu); [alltrails.com](https://www.alltrails.com/trail/peru/cusco/camino-inca) |
| 44 | Everest Base Camp Trek | Sagarmatha National Park, Nepal | Return Lukla, base camp and Kala Patthar | 130.0 | 7500 | 12 days | Strenuous | S F | [acethehimalaya.com](https://www.acethehimalaya.com/everest-base-camp-elevation-gain/); [alltrails.com](https://www.alltrails.com/trail/nepal/province-1/everest-base-camp-trek) |
| 45 | Tour du Mont Blanc | Mont Blanc Massif, France · Italy · Switzerland | Loop | 170.0 | 10000 | 11 days | Strenuous | O L F | [autourdumontblanc.com](https://www.autourdumontblanc.com/en/) |
| 46 | Milford Track | Fiordland National Park, New Zealand | One way Glade Wharf to Sandfly Point | 53.5 | 1300 | 4 days | Strenuous | W F L | [doc.govt.nz](https://www.doc.govt.nz/parks-and-recreation/places-to-go/fiordland/places/fiordland-national-park/things-to-do/tracks/milford-track/); [fiordlandoutdoors.co.nz](https://www.fiordlandoutdoors.co.nz/blog/running-the-milford-track/) |
| 47 | Torres del Paine W Trek | Torres del Paine, Chile | One way | 75.0 | 2750 | 5 days | Strenuous | L F | [alltrails.com](https://www.alltrails.com/trail/chile/magallanes/torres-del-paine-circuito-w); [swoop-patagonia.com](https://www.swoop-patagonia.com/chile/torres-del-paine/hiking/w-trek) |
| 48 | Kilimanjaro Machame Route | Kilimanjaro National Park, Tanzania | Machame ascent / Mweka descent | 62.0 | 5000 | 7 days | Strenuous | S F | [ultimatekilimanjaro.com](https://www.ultimatekilimanjaro.com/machame-route/); [tranquilkilimanjaro.com](https://www.tranquilkilimanjaro.com/kilimanjaro-national-park-regulations-and-fees/) |
| 49 | Laugavegur Trail | Fjallabak Nature Reserve, Iceland | One way Landmannalaugar to Þórsmörk | 55.0 | 1500 | 4 days | Strenuous | L | [fi.is](https://www.fi.is/en/hiking-trails/trails/view/laugavegur); [adventures.is](https://adventures.is/iceland/attractions/laugavegur-trail/) |
| 50 | Overland Track | Cradle Mountain-Lake St Clair, Tasmania, Australia | One way to Narcissus; ferry out | 65.0 | 2230 | 6 days | Strenuous | L F W | [parks.tas.gov.au](https://parks.tas.gov.au/explore-our-parks/cradle-mountain/overland-track); [trailhiking.com.au](https://www.trailhiking.com.au/hikes/overland-track/) |

## Source limitations

| Rows | Decision and evidence boundary |
|---|---|
| 2 — Trolltunga | Retain the official P2 figures of 27 km and 800 m ascent. The official site published that combination at the source review. Cumulative GPS totals may be higher; this is not a surveyed cumulative total and must not be combined with P3's shorter distance. |
| 3 — Tongariro | The 765 m estimate represents the principal Mangatepopo-to-Red Crater climb; smaller undulations can make cumulative gain higher. |
| 4 — Yoshida | The official site gives 6.8 km ascent plus 7.0 km descent on distinct paths, so 13.8 km and LOOP describe the combined route. This is not an additional circuit around the summit crater. The 1,470 m figure approximates the climb from the fifth station to the summit area. |
| 5, 11, 12, 18, 19 | Distance and ascent vary with the selected starting point and viewpoint. The fixture variants are El Chaltén return, Paro trailhead return, Torres base viewpoint, Hatton/Nallathanni return, and Laguna 69 return respectively. These values lie within the published route ranges recorded during source review; they are not exact survey measurements. |
| 6 — Platteklip | The variant is **point-to-point**, including the walk across the plateau to the upper cable station. The 4.5 km is not a full return walk. The description requires a separate descent plan. Omitting DOG_FRIENDLY avoids advertising a route with cableway and park-zone complications; it is not a claim that dogs are banned across Table Mountain. |
| 7, 21 — Lake Louise walks | Plain of Six Glaciers ends at the tea house; the Abbot Pass viewpoint adds distance and gain. Lake Agnes excludes either Beehive extension. Parks Canada's one-way distances are doubled for these return walks. |
| 9 — Preikestolen | The fixture uses 350 m; the official operator also published 500 m at the source review. The stored ascent is approximate, not a verified cumulative measurement. |
| 13, 20, 22, 25, 31, 33, 36, 43, 44, 47, 50 | Some elevation values rest on GPS/route-platform or operator estimates rather than independently confirmed park totals. Their stored gains remain researched estimates; a route link alone must not be cited as proof of every number. |
| 15, 35 — Yosemite additions | NPS gives Nevada Fall 5.4 miles / 2,000 ft / 5–6 h and Upper Yosemite Fall 7.2 miles / 2,700 ft / 6–8 h. The seeds convert these to 8.7 km / 610 m / 330 min and 11.6 km / 823 m / 420 min. Both are strenuous return hikes without dog tags. |
| 25 — Dragon's Back | The Hong Kong government route at the source review was 8.5 km / 2 h 45 min and did not publish a 490 m ascent. That gain may be overstated: GPS profiles reviewed were nearer 300–400 m. The stored 8 km / 490 m / 3 h values remain approximate fixture estimates, not a verified transcription of government statistics. |
| 27 — Wadi Shab | The five-kilometre walking route reaches the pools; reaching the hidden waterfall involves swimming beyond the walking section. The description states that distinction rather than implying a dry walking approach to the waterfall. |
| 32 — Kawah Ijen | The 9.5 km / 592 m variant includes the crater approach. Access inside the crater depends on current restrictions; the description directs users to check rather than presenting crater descent as an available or recommended action. |
| 38 — Highline | The **594 m** gain and **hard** grade use the route source's 11.8 miles, 1,950 ft total ascent and strenuous rating. Its route runs Logan Pass → Granite Park Chalet → The Loop, without the Grinnell Glacier overlook spur. The source title says “Loop” because of the return shuttle; the walking route is point-to-point and has no LOOP tag. |
| 39 — Samaria | The archived official park FAQ explicitly permits dogs on a leash. At the source review, that page stated it was archived and pointed to NECCA. NECCA pages were checked on September 18, 2026 but no replacement dog rule was available in their readable text. The tag retains that provenance limit; it does not establish the current rule. The 100 m ascent is a route-profile estimate on a predominantly descending walk. |
| 41 — Lac Blanc | The **hard** grade follows the official Chamonix page, which graded this 7 km / 500 m / 3 h route difficult at the source review. It starts above the La Flégère lift; walking from the valley adds substantial ascent. |
| 42 — Diamond Head | The **moderate** grade follows the state park brochure. The state park narrative at the source review also calls the short ascent steep and strenuous. The 171 m gain is the published 560 ft; the seed is the 2.6 km return walk. |
| 44 — Everest Base Camp | The 130 km / approximately 7,500 m itinerary includes acclimatization walks and the Kala Patthar viewpoint. Retain SUMMIT under the app definition of a named high viewpoint; this does not mean an Everest summit attempt. The description names Kala Patthar explicitly. The 12 days represent a typical researched itinerary with acclimatization, not a promise of a suitable schedule for every visitor. |
| 46 — Milford | The stored ascent is **approximately 1,300 m**. DOC supported the 53.5 km / four-day route but published no total ascent in the reviewed evidence. The local transport operator publishes 1,070 m ascent and 1,270 m descent, while describing both standard hikers and reverse-direction runners; its endpoint order is not consistent enough to derive an exact standard-direction gain. Consequently 1,300 m is an explicitly approximate fixture value, not an official DOC measurement. The optional Sutherland Falls side trip adds distance and is not included in the 53.5 km. |
| 49 — Laugavegur | The **approximately 1,500 m total ascent** is published by Arctic Adventures for the same 55 km / four-day Landmannalaugar → Þórsmörk route. FÍ's 500 m headline and stage net changes do not establish full cumulative ascent. Do not include the separate Fimmvörðuháls extension. |
