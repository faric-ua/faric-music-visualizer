# projectM integration spike

FARIC Music Visualizer v0.4.0 experimentally integrates **libprojectM 4.1.7**.

- Upstream: projectM-visualizer/projectm
- License: GNU LGPL-2.1-or-later
- Native library is fetched from the pinned tag `v4.1.7` at build time.
- The small `.milk` files in `app/src/main/assets/projectm/presets/` are adapted from libprojectM's own test presets in the same tagged source and are used only to validate the integration.

This spike intentionally does **not** bundle third-party community preset packs such as Cream of the Crop. Preset-pack licensing will be reviewed separately before any distribution decision.
