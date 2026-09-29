# Visual Packs — projectM and FARIC assets

## Current state

The full projectM Cream of the Crop library is NOT bundled inside the APK.

The base APK contains only tiny fallback presets.

On first projectM use the app downloads the pinned upstream preset archive and texture pack, then extracts them into app storage.

So the current architecture already keeps the large library outside the APK binary.

## Problem found in v0.5.x

The old TOP mode physically copied roughly half of the 9,795 presets into a second directory.

That caused:
- thousands of duplicate small files;
- extra disk use;
- extra directory traversal;
- slower state checks;
- no real quality benefit because "TOP" was only every second sorted preset.

## Library Index v2

Starting with v0.6.0:

- original 9,795 preset files exist only once;
- ALL is a text index of those original files;
- fast/default pool is an index of 1,200 evenly distributed presets;
- no 4,898-file TOP copy;
- old duplicated TOP directory is deleted during migration;
- library state reads marker/index metadata instead of recursively counting 9k files on the UI thread;
- ALL / FAST catalog reads persistent index files instead of filesystem traversal.

Ratings still point to canonical preset identity and therefore work across pools.

## Why 1,200

1,200 is a seed pool, not a claim that these are the 1,200 best visuals.

It is small enough to:
- create queues quickly;
- rate and curate over time;
- avoid presenting thousands of near-duplicates immediately.

Later the default pool should become learned/curated:

```text
LIKED
 + good performance
 + low switch latency
 + category diversity
 - disliked
 - hidden
 - repeatedly heavy presets
```

ALL remains available.

## Future .faricpack format

The next storage step is an independently installable visual pack:

```text
projectm-core.faricpack
hero-pack-01.faricpack
retro-pack-01.faricpack
...
```

A pack should contain:

```text
manifest.properties
index/
assets/
presets/
textures/
LICENSES/
checksums.sha256
```

Manifest fields:
- pack id;
- version;
- minimum FARIC version;
- content type;
- item count;
- source/provenance;
- license metadata;
- checksum.

## Install flow

```text
FARIC APK
  |
  +-- fallback visuals only
  |
  +-- Pack Manager
         |
         +-- Download pack
         +-- Verify SHA-256
         +-- Validate manifest
         +-- Unpack to app-specific disk storage
         +-- Build/read indexes
         +-- Enable pack
```

The user should never manually unpack thousands of files.

## Memory rule

Pack files live on disk.

Only:
- indexes;
- current item;
- small NEXT queue;
- required textures

should be active in memory.

Do not preload the whole library into RAM.

## Distribution

For sideload/GitHub releases:
- host versioned .faricpack release assets;
- app downloads a selected pack on demand.

For Play distribution later:
- evaluate Play Asset Delivery, but keep FARIC's pack abstraction so the renderer does not care how the files arrived.

## TODO

- [x] Stop physically duplicating TOP presets.
- [x] Add persistent ALL index.
- [x] Add persistent 1,200 FAST index.
- [x] Remove recursive state counting from normal UI startup.
- [x] Migrate/delete old faric-top-half duplicate directory.
- [ ] Add PackManifest model.
- [ ] Add PackManager.
- [ ] Add SHA-256 pack verification.
- [ ] Add pack install/update/remove UI.
- [ ] Move projectM library delivery from raw upstream zip to versioned FARIC pack.
- [ ] Add storage usage screen.
- [ ] Add backup/export of ratings independently from large pack files.
