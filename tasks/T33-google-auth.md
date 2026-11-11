# T33 — Google sign-in + Drive scope

Milestone: M6 Backup  
Depends on: T03, T04

## Goal
Optional account for backup.

## Do
- Credential Manager (GetGoogleIdOption) sign-in; AuthorizationClient for `drive.file`; `GoogleAuth.accessToken()` refreshes silently; `revoke()`.
- 'Save your seconds' sheet + 'Before Google consent' explainer screen (copy from SPEC). Entry points: onboarding step 7, Settings → Account, Backup.
- Store signedIn/email in settings.

## Don't
- No Firebase.

## Done when
- [ ] Sign in / out works on device with your OAuth client
- [ ] Build passes

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
