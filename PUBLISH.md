# Publish TV Refresh

Repository: **tv-refresh**

Description: **Less clutter. More TV. A minimal Android TV launcher with guided cleanup, themes, screensavers and setup guides.**

1. Sign in to GitHub. Choose New repository.
2. Enter `tv-refresh` under your account.
3. Choose Public if others should see and use it.
4. Leave the README, .gitignore and license boxes unchecked. Those files are included here.
5. Create the repository.
6. Upload the contents of this folder or push the prepared Git repository. Avoid adding an extra outer folder.
7. Open the README and check screenshots, guide links and the APK link.
8. Create release `v1.0.0`, titled **TV Refresh 1.0 — Less clutter. More TV.** Attach the files in `dist/`.
9. Add topics: `android-tv`, `launcher`, `tcl`, `adb`, `screensaver`, `debloat`.

Only this publication folder/archive is intended for uploading. Private development backups and signing keys are excluded.

Alternatively, after installing GitHub CLI and running `gh auth login`, run from this repository:

```sh
gh repo create tv-refresh --public --description "Less clutter. More TV. A minimal Android TV launcher with guided cleanup, themes, screensavers and setup guides." --source . --remote origin --push
```

If that name already exists under your account, inspect the existing repository before choosing another name.
