# FARIC PulseDeck — Player Product Vision

## Goal

Turn FARIC Music Visualizer into a full local music player with a reusable player shell that can later be embedded into YTM Importer.

The product must be visually original. Existing players are reference material for interaction patterns only; layouts, iconography, composition, naming and visual identity are FARIC-specific.

## Product modes

1. **Standalone local player**
   - scans local music;
   - folder/library navigation;
   - queue, search, shuffle/repeat;
   - album/artist/genre/year views;
   - mini-player + full Now Playing;
   - equalizer/audio tools;
   - FARIC visualizer.

2. **Embedded YTM player**
   - receives YouTube video IDs / playlist IDs from YTM;
   - uses the official YouTube embedded player path;
   - reuses the same queue/navigation/player UI where platform capability allows;
   - does not scrape or extract YouTube media streams.

## Design identity: PulseDeck

Core visual ideas:
- black/deep graphite canvas;
- warm neon accent derived from current cover/background;
- **PulseDock**: persistent bottom mini-player;
- **PulseCore**: central visual/album focus on Now Playing;
- **Orbit Rail**: primary playback actions around the center, not a copied linear Poweramp transport;
- **Library Worlds**: large dynamic collection cards rather than a Poweramp-style category list;
- **Action Shelf**: selection/context actions in a dedicated bottom sheet;
- **Tone Lab**: equalizer/audio tools;
- **Scene Lab**: visualizer/background/preset controls.

## Navigation

Primary destinations:
- Library;
- Visualizer;
- Search;
- Queue / More.

The mini-player is persistent above navigation and expands into Now Playing.

## MVP full-player scope

- local media index;
- all tracks;
- folders;
- albums;
- artists;
- genres;
- years;
- search;
- queue;
- play/pause/previous/next;
- seek;
- shuffle;
- repeat;
- multi-select;
- context actions;
- persistent mini-player;
- full Now Playing;
- 10-band EQ foundation;
- visualizer entry point.

## Non-goals for the first player release

- cloning another player's visual appearance;
- downloading or extracting media from YouTube;
- cloud synchronization;
- advanced tag editing;
- DSP feature parity with specialist audio players.
