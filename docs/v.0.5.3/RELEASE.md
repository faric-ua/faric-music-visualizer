# v0.5.3 — persistent projectM ratings and hidden filtering

## Rating controls

ProjectM LAB adds a second control row:

- 👍 = like;
- 👎 = dislike;
- − = hide from active pools.

Ratings are stored persistently in app preferences.

The original .milk files are never deleted by these actions.

## Canonical preset identity

TOP contains copied presets from the full Cream of the Crop tree.

Ratings therefore use a canonical relative preset identity instead of the absolute file path. The same preset receives the same rating in TOP and ALL.

## Selection policy

The CURRENT + 3 NEXT queue now filters and weights presets:

- HIDDEN => excluded completely;
- UP => weight 6;
- NONE => weight 3;
- DOWN => weight 1.

A short recent-history window still reduces immediate repetition.

## Hide behavior

Pressing − on the current preset:

1. persists HIDDEN;
2. rebuilds the current pool in place;
3. excludes that preset from future TOP/ALL queue selections;
4. advances to a valid visible preset without deleting the file.

A hidden-management/recovery screen is intentionally deferred; the stored source files remain intact.

## Status

The LAB status now shows:

- AUTO / MANUAL;
- TOP / ALL;
- visible preset count;
- PRELOAD queue depth;
- current rating symbol;
- current preset name;
- active FARIC FG sample.
