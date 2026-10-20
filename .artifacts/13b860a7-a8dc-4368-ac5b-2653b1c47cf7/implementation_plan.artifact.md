# Navigation Fixes Plan

Fix logical navigation flows in QuickDrop app as identified by manual audit and Stitch source of truth.

## User Review Required

> [!NOTE]
> The Partner flow is being adjusted to include the `PickupConfirmationFragment` which was previously unreachable. The flow will now be: `Available Requests` -> `Request Details` -> `Destination Delivery` -> `Pickup Confirmation` -> `Delivery In Progress` -> `Proof of Delivery`.

## Proposed Changes

### Navigation Graph

#### [MODIFY] [nav_graph.xml](file:///C:/Users/ItsBo/AndroidStudioProjects/Quick_Drop/app/src/main/res/navigation/nav_graph.xml)
- Update `destinationDeliveryFragment` action to point to `pickupConfirmationFragment`.
- Add `action_pickupConfirmation_to_deliveryInProgress` to `pickupConfirmationFragment`.
- Add `action_deliveries_to_tracking` and `action_deliveries_to_orderDetails` to `deliveriesFragment`.
- Add `action_paymentSuccess_to_tracking` and `action_paymentSuccess_to_dashboard` to `paymentSuccessFragment`.
- Ensure all other mentioned flows have corresponding actions.

---

### Java UI Components

#### [MODIFY] [PartnerDashboardFragment.java](file:///C:/Users/ItsBo/AndroidStudioProjects/Quick_Drop/app/src/main/java/com/example/quick_drop/ui/PartnerDashboardFragment.java)
- Change `btnViewDelivery` navigation to `action_partnerDashboard_to_availableRequests` to follow the intended acceptance flow.

#### [MODIFY] [DestinationDeliveryFragment.java](file:///C:/Users/ItsBo/AndroidStudioProjects/Quick_Drop/app/src/main/java/com/example/quick_drop/ui/DestinationDeliveryFragment.java)
- Change `btnArrived` navigation to `action_destDelivery_to_pickupConfirmation`.

#### [MODIFY] [PickupConfirmationFragment.java](file:///C:/Users/ItsBo/AndroidStudioProjects/Quick_Drop/app/src/main/java/com/example/quick_drop/ui/PickupConfirmationFragment.java)
- Change `btnConfirmPickup` navigation to `action_pickupConfirmation_to_deliveryInProgress`.

#### [MODIFY] [DeliveriesFragment.java](file:///C:/Users/ItsBo/AndroidStudioProjects/Quick_Drop/app/src/main/java/com/example/quick_drop/ui/DeliveriesFragment.java)
- Use defined actions for `btnTrack` and `btnDetails`.

#### [MODIFY] [PaymentSuccessFragment.java](file:///C:/Users/ItsBo/AndroidStudioProjects/Quick_Drop/app/src/main/java/com/example/quick_drop/ui/PaymentSuccessFragment.java)
- Use defined actions for `btnTrack` and `btnDashboard`.

## Verification Plan

### Automated Tests
- Run `./gradlew clean assembleDebug` to ensure all navigation actions are valid and the project builds.

### Manual Verification
- Verify that `PickupConfirmationFragment` is now reachable via the Partner flow.
- Verify that `PartnerDashboard` correctly leads to the acceptance flow.
- Verify all sender and common flows match the Stitch requirements.
