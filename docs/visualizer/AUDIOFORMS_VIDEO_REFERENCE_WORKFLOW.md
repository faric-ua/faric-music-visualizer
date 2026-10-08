# Audioforms — video reference analysis workflow (2026-10-08)

**Purpose:** Enable an independent ChatGPT chat for analyzing user-supplied visualizer videos while the main FARIC / PulseDeck implementation chat continues normal work. This document is not an implementation order.

## Two-chat separation

**Main chat — FARIC development:** release checkpoints, GitHub app changes, CI, Termux, phone QA, current navigation/export defects; implement user-approved proposals when ready.

**Separate chat — Audioforms reference lab:** watch each supplied video, classify visuals and reactions, compare examples, suggest feasible new Audioform presets, and write reference designs here. It must **not change Android application source or launch builds** unless explicitly requested; retain links to reference videos/frames only when actually available.

Both chats use the **same repository** `faric-ua/faric-music-visualizer` and can read this document. They do not automatically share all unposted chat attachments, so upload each source video to the reference chat; write reusable findings in this repository.

## Classification

- **Heroes** = Shark/Panther/character mascot packs (legacy `GraphicFigureCatalog`).
- **Audioforms** = procedural music-deforming geometry.
- **Decor** = frames / logos / ornamental layers.
- **FX** = glow / rays / particles.
- **Visualizer/projectM** = separate preset-based background engine; not a Hero.
- **Board** = workspace/composite with position/scale/rotation/z-order.

Authoritative proposed naming: `docs/architecture/VISUAL_ELEMENTS_TAXONOMY.md`.

## Per-video analysis checklist

1. Source video: filename, length, resolution, fps; verify footage is actually present rather than guessing.
2. Segments/timecodes: note each unique behavior and camera/layout change.
3. Geometry: smooth ring, deforming radial polygon, oscillating spline, mesh, fluid simulation, particle ring, spectrum bars, etc.
4. Audio binding **hypotheses** (not claims without audio+frames): bass to mean radius/thickness; mid to contour deformation; high to fine detail/glow; onset/beat to impulse; amplitude to global scale/opacity.
5. Dynamics: attack, release, smoothing, threshold, inertia and direction, phase/speed; distinguish steady motion from beat-triggered effects.
6. Palette and layering: fill/stroke/glow, additive compositing, occlusion, placement with Hero/Decor.
7. Feasibility: Android OpenGL shader vs Canvas, memory, 1080x1920×30fps, GPU direct export determinism, usable quality/performance tradeoffs.
8. Output: one named Audioform concept, suggested controls and default values, implementation complexity, risks, clear phone-QA acceptance checks.

## Provisional audio signal contract (to validate against footage)

Input: amplitude, bass, mid, high, beat/onset intensity, elapsed time and frame dimensions. Implement deterministic smoothing (fast attack and slower release), not direct raw FFT noise.

Possible smooth radial contour:

`r(θ,t) = R_base × [1 + B(t)·bassScale + M(t)·midWarp·sin(kθ + ωt) + H(t)·fineWarp·sin(nθ - νt)] + beatImpulse(t)`

The equation is a starting model, not a claimed reconstruction of an unseen video. Constrain perturbation amplitude to prevent negative radius, excessive self-intersection or vibrating edges. Do motion on the GPU when appropriate; align live time and export timeline.

## Research output template

- Reference: (video file name, 00:00-00:XX segment)
- Visual observation:
- Potential signal mapping:
- Motion/smoothing:
- Proposed Audioform name:
- Editor controls (position, size, orientation, speed, deform strength, bass/mid/high/beat sensitivity, color/glow, opacity):
- Layer placement:
- Live/export strategy:
- Performance risks:
- Phone test:
- Decision: **IDEA / APPROVED / NOT NOW / IMPLEMENTED**

## Suggested starter for separate chat

> Це окремий чат **FARIC Audioforms Reference Lab** у проєкті music visualizer. Працюємо з репозиторієм faric-ua/faric-music-visualizer і документами docs/architecture/VISUAL_ELEMENTS_TAXONOMY.md та docs/visualizer/AUDIOFORMS_VIDEO_REFERENCE_WORKFLOW.md. Тут тільки аналізуємо мої відео: таймкоди, геометрію, реакції на бас/мід/високі/біти, швидкість та плавність, пропонуємо Audioforms і налаштування. Не змінюй робочий Android-код і не запускай збірки без моєї прямої команди. Я завантажуватиму відео по одному.

## Caveats

- If original video is unavailable, ask the user to attach it; do not substitute guessed frame analysis.
- The assistant cannot reliably inspect a user's current image-generation remaining credits/reset time. When an explicit image creation request is made, attempt permitted generation then; the product UI may expose limits.
- Content is experimental and should not overwrite latest stable checkpoint or rename existing GF assets.
