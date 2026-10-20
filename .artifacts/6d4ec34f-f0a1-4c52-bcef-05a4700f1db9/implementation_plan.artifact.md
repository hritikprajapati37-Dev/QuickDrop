# Implementation Plan - UX Flow Completion

This plan ensures that all UI elements intended for navigation (buttons, cards, menu items) are correctly wired to actions in the `nav_graph.xml` and implemented in the respective Fragment classes. This will allow for a complete, end-to-end UX flow visualization in the Navigation Editor and a working app experience.

## Proposed Changes

I will perform a comprehensive sweep of the project to ensure:
1.  **Navigation Graph (`nav_graph.xml`)**: All fragments have the necessary actions to transition between states.
2.  **Fragment Implementation**: All buttons identified in the layout XMLs have `setOnClickListener` implementations that call `Navigation.findNavController(v).navigate(...)`.
3.  **Consistency**: Back buttons and "Up" navigation are correctly handled using `navigateUp()` or `popBackStack()`.

### Key Flows to Verify/Complete:

#### 1. Onboarding & Auth Flow
- `WelcomeFragment` -> `LoginFragment` (via `btnGetStarted`)
- `LoginFragment` -> `AccountSelectionFragment` (via `btnCreateAccount`)
- `LoginFragment` -> `SenderDashboardFragment` or `PartnerDashboardFragment` (via `btnLogin`)
- `AccountSelectionFragment` -> `SenderSignUpFragment` or `PartnerSignUpFragment` (via `btnContinue`)
- `SenderSignUpFragment` -> `SenderDashboardFragment`
- `PartnerSignUpFragment` -> `PartnerDashboardFragment`

#### 2. Sender Flow (Delivery Lifecycle)
- `SenderDashboardFragment` -> `CreateDeliveryFragment` -> `OrderConfirmationFragment` -> `PaymentFragment` -> `PaymentSuccessFragment` -> `OrderTrackingFragment`.
- Bottom Navigation wiring for Sender Dashboard, Active Deliveries, Order History, and Profile.

#### 3. Partner Flow (Delivery Fulfillment)
- `PartnerDashboardFragment` -> `AvailableRequestsFragment` -> `DeliveryRequestDetailsFragment` -> `DestinationDeliveryFragment` -> `PickupConfirmationFragment` -> `DeliveryInProgressFragment` -> `ProofOfDeliveryFragment` -> `DeliveryCompletedFragment` -> `FeedbackFragment`.
- Bottom Navigation wiring for Partner Dashboard, Available Requests, Delivery History, and Profile.

#### 4. Shared/Utility Flow
- Profile screens -> Edit Profile.
- Profile screens -> Terms of Service / Privacy Policy.
- Logout functionality in Profile screens -> `LoginFragment`.

## Verification Plan

### Manual Verification
1.  Open `nav_graph.xml` in the Navigation Editor to verify all arrows (actions) represent the intended UX flow.
2.  Deploy the app to a device/emulator and perform a full "smoke test" of both Sender and Partner roles.
