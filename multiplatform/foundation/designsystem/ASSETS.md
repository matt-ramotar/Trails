# Bundled assets

The fonts and compass come from the Trails Figma design. The original variable fonts remain unmodified and are distributed with their SIL Open Font License files under `composeResources/files/fonts`. Location photography has separate sources and licenses listed in [trail photography](../../ui/trail/TRAIL_PHOTOS.md).

- **Manrope:** original `wght` range 200–800, default 200. Heading weights are explicitly selected through `FontVariation.weight`, preventing the variable-font default from silently becoming the heading face.
- **Inter:** original `wght` range 100–900, default 400; `opsz` range 14–32, default 14. Reading and control weights use explicit `wght` plus the source default `opsz=14`. No runtime font request is made.
- **Compass:** `trails_compass_mark.xml` is a mechanical conversion of the captured 48 × 48 SVG. Its path data, even-odd fill rule, viewport and Forest fill are preserved. `TrailsCompass` permits semantic recoloring; `TrailsBrand` uses the original lowercase Manrope Bold signature and -4% letter spacing. Android launcher artwork is a separate platform task.
- **Photography:** `TrailPhoto` resolves each of the 50 catalog IDs to a bundled photograph of its route, destination, or a view from it. Source JPEG bytes remain unchanged; Compose crops the displayed image to fit. Credits, source and license links appear on trail detail and Welcome through TrailPhotoCredit; cards and collection tiles show the photograph alone. All photos work offline.

## Copied resource hashes

| Resource | SHA-256 |
| --- | --- |
| `src/commonMain/composeResources/font/manrope_variable.ttf` | `d0639be45d0af36e798172419d7bd173c4bd4f29e2b76cbb69db1d11bf8b0a40` |
| `src/commonMain/composeResources/font/inter_variable.ttf` | `29160a80ff49ddcab2c97711247e08b1fab27a484a329ce8b813d820dc559031` |
| `src/commonMain/composeResources/files/fonts/manrope-OFL.txt` | `e01b637272e0cbdfb240184dd98ea5cc671556d9894dae2668d92ab2c906787c` |
| `src/commonMain/composeResources/files/fonts/inter-OFL.txt` | `5b9321a4298cfeb6b34354164a1c3afc3db114569984c502b9b35d988fd58c57` |

## Theme and presentation

The default theme uses the Stone/Forest palette. Explicit heading weights and the original compass geometry are preserved by the theme and brand components. Trail photography belongs to `ui/trail`; the design system owns typography, color, spacing, and generic controls.
