# Navigation v1.1

No bottom tab bar. **Camera is the root.** Everything else is at most two levels deep.

## Map
```
Onboarding (first launch only) ──► Camera
Camera (root)
 ├─ project chip ─► Projects sheet ─► New project sheet
 ├─ calendar chip ─► Project ─┬─ day cell (filled) ─► Clip detail ─► Filters editor / Share sheet
 │                            ├─ day cell (empty) ─► Media picker(forDate) ─► Edit clip | Photo clip
 │                            ├─ Make film ─► Film builder ─► Export ─► Player
 │                            ├─ film poster ─► Player
 │                            ├─ Fill empty days (menu)
 │                            └─ ⚙ ─► Settings ─► Backup / Account / Notifications / …
 ├─ record stop ─► Edit clip ─► Filters editor
 ├─ Add (+) ─► Media picker ─► Edit clip | Photo clip
 └─ TODAY ✓ chip ─► Clip detail (today)
```

## Rules
| From | Action | Goes to | Back from there |
|---|---|---|---|
| Camera | system Back | exits app | — |
| Camera | stop recording | Edit clip (tmp file) | Discard sheet ("Discard this recording?") → Camera |
| Edit clip | Save clip | **pop to the screen that opened the flow** (Camera or Project) + toast "Clip saved to <day> · View · Undo" | — |
| Edit clip | Retake | pop to Camera, tmp deleted | — |
| Media picker | pick video | Edit clip (picker stays below) | Back → picker |
| Media picker | pick photo | Photo clip | Back → picker |
| Photo clip | Save clip | pop to flow origin + toast | — |
| Filters editor | Apply | pop with result | Back = cancel, no changes |
| Project | day cell | Clip detail | Back → Project, same month |
| Clip detail | Replace clip | Media picker / Camera for that date → Save pops back to Clip detail | — |
| Film builder | Make film | Export (replaces builder in the stack) | Back while rendering = "Keep rendering in background?" → Project |
| Export done | Play film | Player (replaces Export) | Back → Project |
| Projects sheet | tap project | sheet closes, project becomes active, stay on current screen | — |
| Any | notification "reminder" | Camera (clear stack) | — |
| Any | notification "film ready" | Player (stack: Camera ▸ Project ▸ Player) | Back → Project |
| Any | notification "on this day" | Clip detail (stack: Camera ▸ Project ▸ Clip) | Back → Project |
| Widget / Quick Settings tile | tap | Camera (clear stack) | — |

## Global behaviour
- **Flow origin**: capture flows remember where they started (Camera or Project/date). Save always returns there.
- **Single-flight**: every primary button disables on tap and shows progress; navigation ignores duplicate events.
- **Process death**: Edit clip saves the tmp path + position in SavedStateHandle; on restore it reopens the same clip.
- **Toasts** live on the destination screen (passed as a one-shot result), not on the screen being closed.
- **Discard confirm** only for in-app recordings (they exist nowhere else). Gallery picks close without asking.
- Transitions: push = slide 240 ms emphasized; sheets 400 ms; Camera ↔ Project = vertical slide (Project slides up).
