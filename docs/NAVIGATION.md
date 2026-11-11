# Navigation v1.1

> **Filters dropped for now** (no Filters editor route or button anywhere). Restore from git history before commit a78807d.

No bottom tab bar. **Camera is the root.** Everything else is at most two levels deep.

## Map
```
Onboarding (first launch only) ──► Camera
Camera (root)
 ├─ project chip ─► Projects sheet ─► New project sheet
 ├─ calendar chip ─► Project ─┬─ day cell (filled) ─► Clip detail ─► Share sheet
 │                            ├─ day cell (empty) ─► system Photo Picker(forDate) ─► Edit clip | Photo clip
 │                            ├─ Make film ─► Film builder ─► Export ─► Player
 │                            ├─ film poster ─► Player
 │                            ├─ Fill empty days (menu)
 │                            └─ ⚙ ─► Settings ─► Backup / Account / Notifications / …
 ├─ record stop ─► Edit clip
 ├─ Add (+) ─► system Photo Picker ─► Edit clip | Photo clip
 └─ TODAY ✓ chip ─► Clip detail (today)
```

## Rules
| From | Action | Goes to | Back from there |
|---|---|---|---|
| Camera | system Back | exits app | — |
| Camera | stop recording | Edit clip (tmp file) | Discard sheet ("Discard this recording?") → Camera |
| Edit clip | Save clip | **Project screen** (edit screen popped from the stack); the saved day's cell pops in once (scale 0.6 to 1, 420 ms, 2 dp ember ring), count +1. No toast. Replace flow from Clip detail returns to Clip detail | Back → Camera |
| Edit clip | Retake | pop to Camera, tmp deleted | — |
| system Photo Picker | pick video | Edit clip | Back → screen that opened the picker |
| system Photo Picker | pick photo | Photo clip | Back → screen that opened the picker |
| Photo clip | Save clip | same as Edit clip | Back → Camera |
| Project | day cell | Clip detail | Back → Project, same month |
| Clip detail | Replace clip | system Photo Picker / Camera for that date → Save pops back to Clip detail | — |
| Film builder | Make film | Export (replaces builder in the stack) | Back while rendering = "Keep rendering in background?" → Project |
| Export done | Play film | Player (replaces Export) | Back → Project |
| Projects sheet | tap project | sheet closes, project becomes active, stay on current screen | — |
| Any | notification "reminder" | Camera (clear stack) | — |
| Any | notification "film ready" | Player (stack: Camera ▸ Project ▸ Player) | Back → Project |
| Any | notification "on this day" | Clip detail (stack: Camera ▸ Project ▸ Clip) | Back → Project |
| Widget / Quick Settings tile | tap | Camera (clear stack) | — |

## Global behaviour
- **Flow origin**: a save from Camera lands on Project; a save started from Project returns to Project; a Replace started from Clip detail returns to Clip detail.
- **Single-flight**: every primary button disables on tap and shows progress; navigation ignores duplicate events.
- **Process death**: Edit clip saves the tmp path + position in SavedStateHandle; on restore it reopens the same clip.
- **No toasts or snackbars anywhere.** Results show in place; undo = tap the day → Clip detail → Replace / Delete.
- **Discard confirm** only for in-app recordings (they exist nowhere else). Gallery picks close without asking.
- Transitions: push = slide 240 ms emphasized; sheets 400 ms; Camera ↔ Project = vertical slide (Project slides up).

> **Add media**: ADD, "Choose from Photos" (empty day) and Replace clip open the system Photo Picker directly (no gallery screen). The target date (default today) travels in `CaptureSession.forDate`. Cancel returns to the previous screen.
