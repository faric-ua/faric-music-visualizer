# v0.6.0 — Theme engine foundation + Hero Pack 1 + projectM Index v2

## Product direction

FARIC now has an explicit product model for turning a song into a finished visual video:

- choose music;
- choose playback theme;
- choose background/art/logo;
- preview;
- export later through the deterministic video pipeline.

The initial theme registry contains 10 theme families/specs:
- Neon Emblem
- Energy Core
- Portrait Halo
- Glass Core
- Orbital Crown
- Star Seed
- Vinyl
- Cassette
- Poster
- Visualizer

## Hero Pack 1 inside projectM LAB

Five additional FARIC foreground samples are immediately testable through FG:

- Neon Emblem
- Energy Core
- Orbital Crown
- Star Seed
- Wave Idol

They are added to the existing:
- Pulse Rays
- Orbit Rings
- Spectrum Halo

Existing samples are not removed.

Each new hero has a distinct center silhouette and separate bass/mid/high/beat motion.

## projectM Library Index v2

The full 9,795 Cream of the Crop preset library remains outside the APK and exists once on disk.

Changes:
- old ~4,898-file derived TOP copy is no longer used;
- normal/default pool is reduced to a 1,200-item index;
- ALL remains 9,795 through an index;
- no duplicate FAST/TOP preset files;
- old duplicate directory is removed during migration;
- state reads marker/index metadata instead of recursively counting 9k files;
- projectM Activity reads persistent indexes rather than traversing the filesystem.

This should reduce startup/disk overhead. It does not eliminate per-preset projectM shader compilation cost; heavy-preset timing remains a separate task.

## Documentation

Added:
- docs/product/MUSIC_VIDEO_CREATOR_ROADMAP.md
- docs/architecture/VISUAL_PACKS.md
- theme/export research remains in docs/visualizer/PLAYBACK_THEME_EXPORT_RESEARCH.md

## Next

- phone-test all 8 FG samples;
- verify migration from old 4,898 TOP copy to index-only 1,200 pool;
- measure slow projectM preset load/compile times;
- add visual theme picker;
- begin Neon Emblem as a standalone PlaybackTheme independent of projectM;
- then start export MVP.
