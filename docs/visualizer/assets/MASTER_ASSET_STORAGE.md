# FARIC Master Asset Storage Policy

Captured: 2026-09-30

## Current state

The project currently has two different classes of image assets:

1. **Git-tracked previews / catalogs**
   - lightweight WebP contact sheets and UI previews already committed to `main`;
   - intended for project history, browsing and documentation.

2. **Master originals**
   - original user-supplied JPG/PNG files;
   - original generated PNG outputs;
   - these are the files that should be preserved when we may later need to crop, separate layers, regenerate masks, or promote a concept into an app-ready Hero/GF.

At the time of this capture, **the master originals are identified but are not all committed to Git yet**.

Current identified master set:
- 15 user-supplied originals;
- 19 generated originals;
- 34 files total;
- approximately 61.82 MiB.

The exact names, byte sizes and SHA-256 hashes are in:
`MASTER_ASSET_MANIFEST.md`.

## Why previews alone are not enough

A preview/contact sheet is useful for browsing, but it is not a safe substitute for the master file.

The master is needed for:
- highest available resolution;
- alpha/transparency work;
- separating frame / creature / wordmark / FX;
- clean masks;
- color correction;
- future upscaling;
- producing app-ready WebP/PNG assets;
- verifying that a later file is exactly the same source.

Therefore the project should preserve **both**:
- small Git-visible previews;
- full source masters.

## Preferred storage model

For long-term use, do **not** keep endlessly growing AI image history as normal Git blobs in the main application history.

Preferred model:

```text
faric-music-visualizer/            # code + docs + previews
└── docs/visualizer/assets/...

faric-music-visualizer-assets/     # master asset archive OR Git LFS-backed asset area
├── user-originals/
├── generated-masters/
├── production/
│   ├── faric/
│   ├── fmv/
│   └── fvmp/
└── manifests/
```

Two acceptable implementations:

### Option A — separate Git asset repository

Recommended for the current project.

Advantages:
- normal code repo stays fast;
- the phone can clone/pull the asset repo separately;
- source masters have a clear permanent location;
- easier to archive many future generations.

Suggested phone path:

`~/faric-music-visualizer-assets`

### Option B — Git LFS in the main repository

Also valid, but requires Git LFS support on every environment that needs master bytes.

Use LFS patterns such as:
- `*.png`
- `*.jpg`
- large `*.webp`

This avoids putting large binary revisions directly into normal Git history.

## Phone workflow

The important contract is: **the phone should never depend on this chat session to recover a source image.**

After the master archive is created:

```bash
cd ~
git clone <asset-repository-url> faric-music-visualizer-assets
```

Later:

```bash
cd ~/faric-music-visualizer-assets
git pull
```

The user should then always know:

- app/code project: `~/faric-music-visualizer`
- master images: `~/faric-music-visualizer-assets`

Production assets that are actually packaged into the Android app may still be copied into the application repository in optimized form.

## Promotion model

A master image is never used blindly as the final app asset.

Promotion path:

```text
MASTER
  ↓
selected concept
  ↓
cleanup / transparency / layer separation
  ↓
production Hero/GF set
  ├── full
  ├── frame
  ├── creature
  ├── wordmark
  ├── fx
  └── manifest
  ↓
optimized Android asset
```

## Do not silently delete masters

Once a source is admitted to the master archive:
- keep its original bytes;
- keep its original filename or a traceable renamed filename;
- record SHA-256;
- do not overwrite it with an edited version;
- edits become new versioned files.

This makes visual work reproducible and keeps project history recoverable.
