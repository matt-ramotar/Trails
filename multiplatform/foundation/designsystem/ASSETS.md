# Bundled assets

The fonts and compass come from the Trails Figma design. The variable fonts are
unmodified, with SIL Open Font License files under `composeResources/files/fonts`.
Photo sources and licenses are listed in [trail photography](../../ui/trail/TRAIL_PHOTOS.md).

- **Manrope:** `wght` range 200–800, default 200. Heading weights use `FontVariation.weight` to override the variable font's default.
- **Inter:** `wght` range 100–900, default 400. The `opsz` range is 14–32, default 14. Reading and control weights use explicit `wght` with the source default `opsz=14`. No runtime font request is made.
- **Compass:** `trails_compass_mark.xml` converts the captured 48 × 48 SVG, preserving its path data, even-odd fill rule, viewport, and Forest fill. `TrailsCompass` allows color overrides. `TrailsBrand` uses the original lowercase Manrope Bold wordmark and -4% letter spacing.
- **Photography:** `TrailPhoto` maps each of the 50 catalog IDs to a bundled photograph of its route, destination, or a view from it. Compose crops the image for display without changing the source JPEG bytes. TrailPhotoCredit shows credits and source/license links on trail detail and Welcome. Cards and collection tiles show only the photograph. All photos work offline.

## Copied resource hashes

| Resource | SHA-256 |
| --- | --- |
| `src/commonMain/composeResources/font/manrope_variable.ttf` | `d0639be45d0af36e798172419d7bd173c4bd4f29e2b76cbb69db1d11bf8b0a40` |
| `src/commonMain/composeResources/font/inter_variable.ttf` | `29160a80ff49ddcab2c97711247e08b1fab27a484a329ce8b813d820dc559031` |
| `src/commonMain/composeResources/files/fonts/manrope-OFL.txt` | `e01b637272e0cbdfb240184dd98ea5cc671556d9894dae2668d92ab2c906787c` |
| `src/commonMain/composeResources/files/fonts/inter-OFL.txt` | `5b9321a4298cfeb6b34354164a1c3afc3db114569984c502b9b35d988fd58c57` |

## Theme and presentation

The default theme uses the Stone/Forest palette. Trail photography belongs to
`ui/trail`. The design system owns typography, color, spacing, and generic controls.
