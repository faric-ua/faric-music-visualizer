# PulseDeck UI Style Contract

This is a project-wide UI rule. Treat it as a hard default for all future UI work.

## Core rule — inherit before inventing

Every new UI element must visually inherit from the nearest existing parent/sibling component before introducing any new styling.

This applies to:
- dialogs and modal windows;
- menus and context menus;
- buttons, pills and icon buttons;
- cards, panels and sheets;
- labels, headers and status blocks;
- recovery/error/confirmation UI;
- constructors, editors and service tools.

When creating something new:
1. Identify the nearest existing parent flow or visually equivalent sibling.
2. Reuse its existing component helpers, spacing, radii, stroke treatment, typography, button hierarchy and color logic.
3. Prefer existing shared helpers such as `panelDrawable`, `actionPill`, `iconButton`, `label` and established custom-dialog patterns.
4. If a parent/ancestor already defines the interaction style, descendants follow it unless there is an explicit product reason to diverge.
5. Only create a new visual language when there is no suitable existing family. That divergence must be intentional and documented.

## Do not silently fall back to stock Android styling

Do not introduce a default grey `AlertDialog`, stock button, stock list row, stock menu or unrelated Material look into a PulseDeck flow when a styled PulseDeck equivalent already exists.

A platform dialog may still be used for system-owned flows or where Android requires it, but app-owned windows should use the PulseDeck visual family.

## PulseDeck dialog baseline

Unless a closer parent flow specifies otherwise, app-owned modal dialogs should follow the existing PulseDeck custom-dialog pattern:
- dark translucent panel;
- rounded corners;
- subtle light border;
- white primary title;
- muted secondary text;
- orange accent for the primary/destructive-to-proceed action where appropriate;
- dark secondary action pills;
- consistent 44–56 dp action heights;
- 8–14 dp vertical spacing between actions;
- transparent dialog window background;
- width near 88% of the phone viewport on compact screens.

## Interaction inheritance

Style inheritance includes behavior, not only appearance:
- buttons must have the same tap target conventions as sibling controls;
- modal actions must remain reachable and tappable;
- close/cancel behavior should match nearby windows;
- long-running actions should reuse established progress/status patterns;
- disabled state, feedback, haptics and accessibility labels should follow the parent component family.

## Regression rule

Whenever a new UI is added, phone QA must verify:
- it visually belongs to the parent flow;
- all visible actions are actually tappable;
- no action is hidden by layout/content conflicts;
- rotation/recreation does not create a mismatched fallback UI;
- the new component does not introduce a second competing style system.

## Current reference families

Use these as primary references unless a closer component exists:
- layer panel: `showPulseDeckLayersDialog()`;
- export folder chooser: `showExportFolderChooser()`;
- export progress window: custom `Dialog` + `panelDrawable` + `actionPill`;
- common controls: `actionPill`, `iconButton`, `roundControl`, `actionTile`, `label`.

The emergency recovery dialog follows this contract and must stay in the same PulseDeck family.
