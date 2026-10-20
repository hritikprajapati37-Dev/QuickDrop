# FINAL UI CORRECTION — SCREEN 3 — ACCOUNT SELECTION

This plan details the visual corrections for the Account Selection screen to match the original Stitch reference exactly.

## Proposed Changes

### [Layout] [fragment_account_selection.xml](file:///C:/Users/ItsBo/AndroidStudioProjects/Quick_Drop/app/src/main/res/layout/fragment_account_selection.xml)

#### [MODIFY]
- **Logo Section**:
    - Ensure `logo_final.png` is used.
    - Set exact `MaterialCardView` dimensions (120dp x 100dp).
    - Match corner radius (20dp).
    - Match vertical position (80dp top margin).
    - Set `ImageView` scaling (`fitCenter`) and padding (16dp).
- **Header**:
    - Update `tvTitle` to use `Stitch.Typography.HeadlineLarge` (32sp) if needed to match the 32px reference.
    - Adjust margins: `tvTitle` (40dp top margin), `tvSubtitle` (12dp top margin).
- **Selection Cards** (`cardSender`, `cardPartner`):
    - Apply `Stitch.Card.Level1` style.
    - Ensure `16dp` corner radius.
    - Adjust internal layout and padding (24dp as currently set seems correct for "generous padding").
    - Ensure icons (`ic_sender_login`, `ic_two_wheeler`) and chevron (`ic_chevron_right`) are correctly sized and tinted.
    - Adjust card vertical spacing: `48dp` above Sender card, `20dp` between cards.
- **Footer**:
    - Ensure `footerLayout` is centered at the bottom with `32dp` margin.
    - Match typography and spacing for "Back to Login".

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to ensure the project builds successfully.

### Manual Verification
- Render the `AccountSelectionFragment` using the Compose Preview tool (if applicable) or by deploying to a device.
- Compare the rendered UI against the Stitch reference screenshot.
- Verify that clicking `cardSender` and `cardPartner` still triggers the correct navigation/logic.
