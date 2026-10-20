# Implementation Plan - Login UI Alignment

Align the Login screen (Screen 2) with the provided Stitch REFERENCE design exactly.

## User Review Required

> [!IMPORTANT]
> The design uses custom input field styling rather than standard Material Design floating labels. The labels are placed above the input boxes.

## Proposed Changes

### [Component Name] - Layout and Styling

#### [MODIFY] [fragment_login.xml](file:///C:/Users/ItsBo/AndroidStudioProjects/Quick_Drop/app/src/main/res/layout/fragment_login.xml)
- Replace `TextInputLayout` components with a custom layout: `TextView` for label above a styled `EditText`.
- Align typography, spacing, and colors with the REFERENCE.
- Maintain existing IDs (`etEmail`, `etPassword`, `btnLogin`, `btnCreateAccount`, `btnForgotPassword`) to ensure `LoginFragment.java` continues to function.

#### [NEW] [edittext_background.xml](file:///C:/Users/ItsBo/AndroidStudioProjects/Quick_Drop/app/src/main/res/drawable/edittext_background.xml)
- Create a shape drawable with a thin border (`vf_outline_variant`) and rounded corners (`stitch_rounded_default`).

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to ensure the project builds correctly.

### Manual Verification
- Deploy the app to the device/emulator.
- Navigate to the Login screen.
- Compare the UI against the REFERENCE screenshot provided by the user.
- Verify that entering "partner" in the email field navigates to the Partner Dashboard on Login click.
- Verify that regular email navigates to the Sender Dashboard.
- Verify "Create Account" navigates to Account Selection.
