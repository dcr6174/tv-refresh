# Use the remote

| Button | Result |
| --- | --- |
| Left / Right | Move between tiles and controls. |
| OK | Open the selected app or control. |
| Hold OK, then release | Open customization. A new OK press chooses an option. |
| Down from the dock | Open All Apps immediately. |
| Back in All Apps | Return to favorites. |
| Back after an app exits | Restore All Apps if that was where you opened it. |
| Home | Return to favorites. |
| Left beyond the first favorite, or Menu | Open the sidebar. |

Apps control their own internal Back behavior. You may need to exit their pages first.

## Change favorites

1. Select a tile.
2. Hold OK and release.
3. Choose Move, Replace favorite, Add to favorites or Hide, as available.
4. When moving, use arrows and press OK to save. Back cancels.

Hidden tiles stay installed. Use Hidden apps to restore them.

## Use the fan

Select the fan and press OK. It spins while wisps swoop into it, then reports task closures and cache cleanup. A fixed allowlist limits which hidden app tasks can close. Visible tasks, active media, casting and essential TV services are protected. It never calls `am force-stop` or clears app data. Android may retain background services and old history entries.

Cleanup requires local ADB on port 5555. Some TVs show a one-time debugging approval. Without local ADB, basic launcher navigation still works and cleanup reports that it could not complete.
