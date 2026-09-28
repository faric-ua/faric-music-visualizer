# FARIC PulseDeck — UI Blueprint

## 1. Persistent PulseDock

Bottom component shown across Library/Search/Queue screens.

Contains:
- small reactive thumbnail or album art;
- title + artist;
- thin progress ribbon;
- play/pause;
- swipe up / tap to open Now Playing;
- swipe left/right for previous/next when safe.

Distinctive FARIC behavior:
- the thumbnail can morph between cover art and a live PulseCore preview;
- accent glow follows the current visualizer palette.

## 2. Now Playing

Portrait structure:

```text
┌──────────────────────────────┐
│ Back     source / device   ⋮ │
│                              │
│        VISUAL SCENE          │
│   background / cover / orb   │
│                              │
│      ◌ PulseCore / art ◌     │
│                              │
│ title                        │
│ artist · album               │
│ ─────── progress ──────────  │
│ elapsed                total │
│                              │
│   shuffle   ◀  ▶/Ⅱ  ▶  loop │
│                              │
│ Queue   Scene   Tone   More  │
└──────────────────────────────┘
```

The playback cluster is centered around PulseCore and does not copy the exact geometry of the reference player.

## 3. Library Worlds

Top:
- title;
- search;
- scan/status;
- overflow.

Main area uses configurable cards:
- All tracks;
- Folders;
- Albums;
- Artists;
- Genres;
- Years;
- Favorites;
- Recently added;
- Most played;
- custom smart collections later.

Cards may switch between:
- grid;
- compact list;
- cover wall.

## 4. Folder / album / artist detail

Header:
- art / generated visual;
- title;
- track count + duration;
- shuffle;
- play;
- search;
- select;
- overflow.

Track rows:
- art/reactive glyph;
- title;
- artist / secondary metadata;
- duration;
- overflow.

Long press enters multi-select.

## 5. Selection mode

Use a bottom **Action Shelf** instead of copying the reference selection panel.

Header:
- selected count;
- select all;
- close.

Actions:
- Play next;
- Add to queue;
- Add to playlist;
- Favorite;
- Share;
- Track info;
- Remove from library when applicable.

Destructive actions are visually separated.

## 6. Search

Unified search across:
- tracks;
- albums;
- artists;
- folders;
- genres.

Future embedded YTM mode may expose separate tabs:
- Local;
- Current YTM project;
- YouTube playlist contents.

## 7. Tone Lab

Own FARIC screen:
- preamp;
- 10-band EQ;
- presets;
- bass enhancement;
- balance;
- limiter/normalization only when technically supported and tested.

Do not promise DSP that Android/Media3 cannot reliably provide.

## 8. Scene Lab

Controls:
- background image/video;
- procedural scene;
- cover-art mode;
- center object;
- radial spectrum;
- beat sensitivity;
- palette;
- particles;
- brightness;
- motion strength.

Local audio mode can use PCM/FFT features.
YouTube embedded mode must expose capability limits clearly.

## 9. Layout rules

- portrait-first, landscape supported;
- safe insets;
- 48dp+ critical targets;
- persistent bottom UI must not hide the final list row;
- selection/action sheets survive rotation;
- current queue/playback state must not duplicate after recreation.


## 10. Canonical approved skin

The first approved visual baseline is stored in:

`docs/design/pulsedeck/prototypes/PULSEDECK_SKIN_V1.svg`

It is immutable historical design evidence. Future major visual redesigns get a new numbered prototype instead of overwriting V1.

The implementation may adapt spacing, typography and density for real Android constraints, but should preserve:
- graphite/black foundation;
- orange + cyan accents;
- PulseCore focal visual;
- luminous spectrum/waves;
- PulseDock;
- rounded glass-like surfaces.
