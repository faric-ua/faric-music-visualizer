# projectM integration

FARIC Music Visualizer integrates **libprojectM 4.1.7**.

- Upstream: `projectM-visualizer/projectm`
- Engine license: GNU LGPL-2.1-or-later
- Native library is fetched from the pinned tag `v4.1.7` at build time.

## Bundled integration presets

The tiny fallback `.milk` files in
`app/src/main/assets/projectm/presets/` are adapted from libprojectM's own test presets in the same tagged source. They exist only so ProjectM LAB can render before the external preset library finishes installing.

## Optional full preset library

FARIC v0.4.1 can download the complete **Cream of the Crop** preset repository on the user's device.

Pinned source commit:

`projectM-visualizer/presets-cream-of-the-crop@0180df21f5e0bd39b9060cc5de420ed2f1f9e509`

Expected library size at this pin:

- 9,795 `.milk` preset files.

Related texture pack pin:

`projectM-visualizer/presets-milkdrop-texture-pack@6368812f27bc747b517218fbf89d21d59afce4d9`

The Cream of the Crop repository notes that historical MilkDrop presets were generally released without explicit individual licenses and may remain copyrighted by their individual authors. For that reason FARIC does not vendor the entire pack into the application repository/APK. It is downloaded on demand from the upstream source and the exact source revisions are recorded here.

Before any public-store release, preset distribution/licensing remains a separate release-review item.
