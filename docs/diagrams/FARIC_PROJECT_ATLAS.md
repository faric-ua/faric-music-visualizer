# FARIC Project Atlas

Captured: 2026-09-30

This page is the visual map of the current project direction. It combines the current repository modules, the new Layered Board model, the accepted PulseDeck skin direction, user references and generated Hero/GF concepts.

## 1. Product composition

```mermaid
flowchart LR
    M[Music / Video] --> P[Playback Engine]
    M --> A[Audio Analysis / SceneSignal]
    M --> D[Metadata]
    P --> B
    A --> B
    D --> B

    subgraph B[Layered Board]
      L0[L0 Background<br/>image / video / projectM]
      L1[L1 Playback Theme<br/>PulseCore / Vinyl / Cassette / etc.]
      L2[L2 Hero / GF<br/>whole or modular]
      L3[L3 Reactive FX<br/>spectrum / particles / glow]
      L4[L4 Metadata / branding<br/>title / artist / waveform]
      L0 --> L1 --> L2 --> L3 --> L4
    end

    B --> LIVE[Live preview]
    LIVE --> SKIN[PulseDeck Skin overlay<br/>show/hide • opacity • skinId]
    B --> EXP[Deterministic export]
    EXP --> MP4[H.264 + audio mux → MP4]
```

The **Board** is the visual composition. The **PulseDeck Skin** is the live control overlay above the Board and is not baked into export by default.

## 2. Repository tree — current major areas

```text
faric-music-visualizer/
├── app/src/main/java/com/saney/musicvisualizer/
│   ├── analysis/       audio feature extraction / SceneSignal
│   ├── playback/       transport and playback state
│   ├── scene/          scene specs and orchestration
│   ├── theme/          PlaybackTheme / HeroTheme rendering
│   ├── ui/             player and visualizer views
│   ├── projectm/       projectM background integration
│   └── export/         offline analysis / H.264 / AAC / mux proof
├── docs/
│   ├── architecture/
│   │   ├── ARCHITECTURE.md
│   │   └── FARIC_LAYERED_BOARD_VISION.md
│   ├── design/pulsedeck/
│   │   ├── prototypes/PULSEDECK_SKIN_V1.svg
│   │   └── references/PULSEDECK_UI_REFERENCE_332780.webp
│   ├── visualizer/
│   │   ├── PLAYBACK_THEME_EXPORT_RESEARCH.md
│   │   ├── LOGO_GF_REFERENCE_PACK.md
│   │   └── assets/
│   │       ├── references/user/
│   │       ├── generated/faric/
│   │       ├── generated/fmv/
│   │       └── generated/fvmp/
│   └── diagrams/FARIC_PROJECT_ATLAS.md
└── ACTIVE_PLAN.md / CURRENT_HANDOFF.md / START_HERE_ASSISTANT.md
```

## 3. Accepted PulseDeck UI reference

<img src="../design/pulsedeck/references/PULSEDECK_UI_REFERENCE_332780.webp" width="280" alt="PulseDeck UI reference">

Canonical editable baseline: `docs/design/pulsedeck/prototypes/PULSEDECK_SKIN_V1.svg`.

## 4. Generated Hero / GF concepts

### FARIC

| Preview | Preview | Preview | Preview |
| --- | --- | --- | --- |
| <img src="../visualizer/assets/generated/faric/cyber-panther-v1.webp" width="150" alt="cyber-panther-v1"> | <img src="../visualizer/assets/generated/faric/cyber-panther-v2.webp" width="150" alt="cyber-panther-v2"> | <img src="../visualizer/assets/generated/faric/cyber-shark-v1.webp" width="150" alt="cyber-shark-v1"> | <img src="../visualizer/assets/generated/faric/cyber-shark-v2.webp" width="150" alt="cyber-shark-v2"> |
| `cyber-panther-v1` | `cyber-panther-v2` | `cyber-shark-v1` | `cyber-shark-v2` |
| <img src="../visualizer/assets/generated/faric/mecha-tiger.webp" width="150" alt="mecha-tiger"> | <img src="../visualizer/assets/generated/faric/modular-set-v1.webp" width="150" alt="modular-set-v1"> | <img src="../visualizer/assets/generated/faric/void-dragon-v1.webp" width="150" alt="void-dragon-v1"> | <img src="../visualizer/assets/generated/faric/void-dragon-v2.webp" width="150" alt="void-dragon-v2"> |
| `mecha-tiger` | `modular-set-v1` | `void-dragon-v1` | `void-dragon-v2` |

### FMV

| Preview | Preview | Preview | Preview |
| --- | --- | --- | --- |
| <img src="../visualizer/assets/generated/fmv/inferno-phoenix.webp" width="150" alt="inferno-phoenix"> | <img src="../visualizer/assets/generated/fmv/modular-set-v1.webp" width="150" alt="modular-set-v1"> | <img src="../visualizer/assets/generated/fmv/neon-griffin-v1.webp" width="150" alt="neon-griffin-v1"> | <img src="../visualizer/assets/generated/fmv/neon-griffin-v2.webp" width="150" alt="neon-griffin-v2"> |
| `inferno-phoenix` | `modular-set-v1` | `neon-griffin-v1` | `neon-griffin-v2` |
| <img src="../visualizer/assets/generated/fmv/razor-raven.webp" width="150" alt="razor-raven"> | <img src="../visualizer/assets/generated/fmv/thunder-wolf.webp" width="150" alt="thunder-wolf"> |  |  |
| `razor-raven` | `thunder-wolf` |  |  |

### FVMP

| Preview | Preview | Preview | Preview |
| --- | --- | --- | --- |
| <img src="../visualizer/assets/generated/fvmp/modular-set-v1.webp" width="150" alt="modular-set-v1"> | <img src="../visualizer/assets/generated/fvmp/plasma-cobra-alt.webp" width="150" alt="plasma-cobra-alt"> | <img src="../visualizer/assets/generated/fvmp/plasma-cobra-v1.webp" width="150" alt="plasma-cobra-v1"> | <img src="../visualizer/assets/generated/fvmp/plasma-cobra-v2.webp" width="150" alt="plasma-cobra-v2"> |
| `modular-set-v1` | `plasma-cobra-alt` | `plasma-cobra-v1` | `plasma-cobra-v2` |
| <img src="../visualizer/assets/generated/fvmp/titan-scorpion.webp" width="150" alt="titan-scorpion"> |  |  |  |
| `titan-scorpion` |  |  |  |

## 5. User GF / logo references

| Preview | Preview | Preview | Preview |
| --- | --- | --- | --- |
| <img src="../visualizer/assets/references/user/333062.webp" width="150" alt="333062"> | <img src="../visualizer/assets/references/user/333063.webp" width="150" alt="333063"> | <img src="../visualizer/assets/references/user/333064.webp" width="150" alt="333064"> | <img src="../visualizer/assets/references/user/333065.webp" width="150" alt="333065"> |
| `333062` | `333063` | `333064` | `333065` |
| <img src="../visualizer/assets/references/user/333066.webp" width="150" alt="333066"> | <img src="../visualizer/assets/references/user/333069.webp" width="150" alt="333069"> | <img src="../visualizer/assets/references/user/333070.webp" width="150" alt="333070"> | <img src="../visualizer/assets/references/user/333071.webp" width="150" alt="333071"> |
| `333066` | `333069` | `333070` | `333071` |
| <img src="../visualizer/assets/references/user/333072.webp" width="150" alt="333072"> | <img src="../visualizer/assets/references/user/333074.webp" width="150" alt="333074"> | <img src="../visualizer/assets/references/user/333075.webp" width="150" alt="333075"> | <img src="../visualizer/assets/references/user/333076.webp" width="150" alt="333076"> |
| `333072` | `333074` | `333075` | `333076` |
| <img src="../visualizer/assets/references/user/333077.webp" width="150" alt="333077"> | <img src="../visualizer/assets/references/user/333096.webp" width="150" alt="333096"> |  |  |
| `333077` | `333096` |  |  |

## 6. Hero/GF production model

```mermaid
flowchart TB
    H[Hero / GF preset]
    H --> W[Whole GF<br/>one finished transparent asset]
    H --> MOD[Modular GF]
    MOD --> F[Back / frame]
    MOD --> C[Middle / creature]
    MOD --> T[Front / wordmark<br/>FARIC / FMV / FVMP]
    MOD --> X[Optional FX]

    F --> R[Audio routing]
    C --> R
    T --> R
    X --> R
    R --> BASS[Bass / amplitude]
    R --> BEAT[Beat punch]
    R --> HIGH[High-frequency sparks]
```

## 7. Skin-block model

Minimum configuration contract:

```text
visible: true / false
opacity: 0..1
skinId: selected block skin
```

Examples of configurable blocks:
- top header / FARIC branding;
- Library / Tone Lab / Scene Lab shortcuts;
- seek/progress;
- transport controls;
- Scene / EQ / Effects quick actions;
- PulseDock mini-player;
- bottom navigation.

## 8. Preview/export parity

The same saved Board/project description should drive both live preview and deterministic export. The live PulseDeck Skin is a separate UI plane.
