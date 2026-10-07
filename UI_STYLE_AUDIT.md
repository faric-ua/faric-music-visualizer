# PulseDeck UI Style Audit

Audit scope: app-owned interactive UI under `app/src/main/java`.

## Result

The stock Android-looking windows observed on phone were not isolated cases. They came from a small cluster of app-owned `AlertDialog` flows in `MainActivity`. Those flows are now migrated to the shared PulseDeck dialog family in `ui/PulseDeckDialogs.kt`.

### Migrated app-owned modal flows

- PulseDeck tools menu.
- Composition Sets list.
- Save current set text input.
- Rename set text input.
- Set actions menu.
- Delete-set confirmation.
- Graphic Figure picker.
- Controls auto-hide picker.
- Export timing/result dialog.
- Emergency menu recovery dialog was already converted to the PulseDeck custom-dialog family in v0.19.27 work and remains styled.

### Existing custom windows that already follow the PulseDeck family

- Layers / objects draggable panel.
- Export folder chooser.
- Export progress/status dialog.
- Board editor panel.
- Main Library / Now Playing / Theme Picker / Export Lab screens.
- Center Calibration overlay.
- Object Constructor overlay.
- Template Constructor overlay.

### Separate full-screen tool surface

`ProjectMActivity` is a full-screen projectM tool rather than a modal dialog. It uses its own dark overlay controls. It must still follow `UI_STYLE_CONTRACT.md` for future changes; a later visual polish pass may move its controls onto the same shared tokens/helpers without changing projectM behavior.

### System-owned UI — intentionally exempt

The following are Android/system surfaces and are not app-owned windows:
- Android document open/create pickers invoked through `ActivityResultContracts.OpenDocument`, `OpenMultipleDocuments`, and `CreateDocument`.
- System permission prompts.
- System keyboard/IME.

Do not attempt to fake or skin those as PulseDeck dialogs.

### Toasts

Current engineering/tool activities still use Android Toasts for short transient success/error notifications. Toasts are not modal windows and do not block the current style contract. If a persistent in-app notification system is introduced later, it should inherit PulseDeck styling.

## Regression protection

The Validate workflow must reject new app-owned stock dialog APIs:
- `AlertDialog.Builder`
- `android.app.AlertDialog`
- `MaterialAlertDialogBuilder`
- direct imports of `android.widget.Button`

New app-owned modal UI should use `PulseDeckDialogs` or an explicitly documented custom PulseDeck window when the interaction is too specialized for the shared helper.
