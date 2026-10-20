# Polish "Available Requests" Screen

Refine the "Available Requests" screen and delivery card to match the Stitch design reference, focusing on colors, spacing, and the bottom navigation state.

## Proposed Changes

### [Component Name] Theme and Resources

#### [MODIFY] [colors.xml](file:///C:/Users/ItsBo/AndroidStudioProjects/Quick_Drop/app/src/main/res/values/colors.xml)
- Verify if any additional grey colors are needed for the bottom nav selected state. (Currently using `vf_outline` and `vf_outline_variant`).

#### [MODIFY] [bottom_nav_color_selector.xml](file:///C:/Users/ItsBo/AndroidStudioProjects/Quick_Drop/app/src/main/res/color/bottom_nav_color_selector.xml)
- Change the selected state color from `@color/vf_primary` to a grey color (likely `@color/vf_on_surface_variant` or `@color/vf_outline`) to match the Stitch reference.

### Available Requests Screen

#### [MODIFY] [fragment_available_requests.xml](file:///C:/Users/ItsBo/AndroidStudioProjects/Quick_Drop/app/src/main/res/layout/fragment_available_requests.xml)
- **Header:** Update Toolbar title color to `@color/vf_on_background` if it should not be purple.
- **Online Status Card:** Adjust margins and padding to match the 8px soft grid.
- **Bottom Navigation:**
    - Apply `style="@style/Theme.Quick_Drop.BottomNav"`.
    - Set `app:itemIconTint="@color/bottom_nav_color_selector"` and `app:itemTextColor="@color/bottom_nav_color_selector"`.
    - Ensure `app:itemActiveIndicatorStyle` is consistent with the grey pill color.

### Delivery Request Item

#### [MODIFY] [item_delivery_request.xml](file:///C:/Users/ItsBo/AndroidStudioProjects/Quick_Drop/app/src/main/res/layout/item_delivery_request.xml)
- **Typography:** Match exact font sizes and colors for Earnings (`vf_primary`), Order ID (`vf_on_surface_variant`), and Distance.
- **Alignment:** Ensure Pickup and Destination labels and icons are correctly aligned with 12dp spacing as seen in the code.
- **Button:** Use `style="@style/Stitch.Button.Secondary"` for "View Details".
- **Spacing:** Ensure 16dp padding and 16dp bottom margin.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to ensure the project builds successfully.

### Manual Verification
- Render the `fragment_available_requests.xml` and `item_delivery_request.xml` in Android Studio's Layout Editor.
- Deploy to the device and take a screenshot to compare with the Stitch reference.
- Verify that the "Requests" bottom nav item is selected and appears GREY.
