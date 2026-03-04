# Soil Data Storage - Testing Checklist

## Pre-Test Setup

- [ ] Build and deploy app to device/emulator
- [ ] Navigate to the soil data map screen
- [ ] Ensure Bluetooth device (ESP32) is powered on and discoverable
- [ ] Verify Bluetooth is paired/connected
- [ ] Have test data ready from ESP32

## Basic Functionality Tests

### Test 1: Receive Bluetooth Data
- [ ] Click any blue marker on the map
- [ ] Verify Sample Location dialog appears
- [ ] Verify coordinates display correctly (DMS format)
- [ ] Verify "No Data Stored" message shows
- [ ] Click "Receive Data" button
- [ ] Wait for Bluetooth communication
- [ ] Verify all 6 fields appear:
  - [ ] Nitrogen (integer)
  - [ ] Phosphorus (integer)
  - [ ] Potassium (integer)
  - [ ] pH Level (float, 2 decimals)
  - [ ] Temperature (float with °C)
  - [ ] Moisture (integer with %)
- [ ] Verify raw Bluetooth data displays
- [ ] Verify "Save Data" button is visible and green

**Expected Result**: Data dialog shows all fields correctly formatted

### Test 2: Save Soil Data
- [ ] From the received data dialog, click "Save Data"
- [ ] Verify success message appears:
  - [ ] Checkmark icon visible
  - [ ] "Data Saved Successfully!" text
  - [ ] "Marker color changed to green" subtitle
- [ ] Verify success dialog auto-closes after ~2 seconds
- [ ] Verify all dialogs close automatically
- [ ] Look at map - verify marker changed from BLUE to GREEN

**Expected Result**: Marker color changes to GREEN and success message shows

### Test 3: Load Saved Data
- [ ] Click the GREEN marker you just saved
- [ ] Verify Sample Location dialog opens immediately
- [ ] Verify all 6 soil data fields appear:
  - [ ] Nitrogen matches saved value
  - [ ] Phosphorus matches saved value
  - [ ] Potassium matches saved value
  - [ ] pH Level matches saved value
  - [ ] Temperature matches saved value
  - [ ] Moisture matches saved value
- [ ] Verify "Stored Soil Data" header shows (in green text)
- [ ] Verify "Receive Data" button still available

**Expected Result**: Stored data loads automatically and matches saved values

## Advanced Tests

### Test 4: Multiple Locations
- [ ] Save data to dot #1 (should be GREEN)
- [ ] Save data to dot #2 (should be GREEN)
- [ ] Save data to dot #3 (should be GREEN)
- [ ] Verify remaining dots are BLUE
- [ ] Click GREEN dot #1 - verify its specific data loads
- [ ] Click GREEN dot #2 - verify its specific data loads
- [ ] Click GREEN dot #3 - verify its specific data loads
- [ ] Verify each dot shows its own correct data (not mixed)

**Expected Result**: Each location stores and loads its own data independently

### Test 5: Data Formatting
- [ ] Open dialog with received data
- [ ] Verify nitrogen displays as plain integer (e.g., "12")
- [ ] Verify phosphorus displays as plain integer (e.g., "7")
- [ ] Verify potassium displays as plain integer (e.g., "9")
- [ ] Verify pH displays with exactly 2 decimals (e.g., "6.50")
- [ ] Verify temperature displays with 1 decimal + °C (e.g., "29.4°C")
- [ ] Verify moisture displays with % sign (e.g., "62%")

**Expected Result**: All values formatted exactly as specified

### Test 6: Data Persistence (Recomposition)
- [ ] Save data to a marker (should be GREEN)
- [ ] Navigate away from soil data screen
- [ ] Navigate back to soil data screen
- [ ] Verify marker is still GREEN
- [ ] Click the marker
- [ ] Verify data still loaded correctly
- [ ] Close app and restart
- [ ] Check if data persists (will be false if in-memory only)

**Expected Result**: Data persists during recomposition (survives nav away/back)

### Test 7: UI Responsiveness
- [ ] During Bluetooth receive, verify spinner shows
- [ ] Verify "Receiving..." text displays
- [ ] Verify app doesn't freeze (test on slow device)
- [ ] Verify buttons remain clickable
- [ ] Verify dialogs dismiss smoothly
- [ ] Verify success message duration is ~2 seconds

**Expected Result**: UI remains responsive, smooth animations

## Error Handling Tests

### Test 8: Bluetooth Errors
- [ ] Disable Bluetooth on device
- [ ] Try to click "Receive Data"
- [ ] Verify error message appears
- [ ] Verify error message is informative
- [ ] Verify dialog closes gracefully
- [ ] Re-enable Bluetooth

**Expected Result**: Clear error message, app doesn't crash

### Test 9: Permission Denied
- [ ] Revoke Bluetooth permissions in settings
- [ ] Try to click "Receive Data"
- [ ] Verify permission request dialog appears
- [ ] Click "Deny" on permission dialog
- [ ] Verify error message about permissions
- [ ] Grant permissions again

**Expected Result**: Graceful handling of permission denial

### Test 10: Malformed Data
- [ ] Configure ESP32 to send malformed data:
  - [ ] Missing fields: "NPK=12,7,9" (no pH, TEMP, MOIST)
  - [ ] Wrong format: "N12P7K9"
  - [ ] Empty response: ""
  - [ ] Non-numeric: "NPK=abc,def,ghi"
- [ ] Click "Receive Data" for each
- [ ] Verify error dialog appears
- [ ] Verify error message explains issue
- [ ] Verify "Save Data" button is hidden on errors

**Expected Result**: Graceful error handling without crashes

### Test 11: Invalid Data Ranges
- [ ] Configure ESP32 to send out-of-range values:
  - [ ] Negative nitrogen: "NPK=-5,7,9;PH=6.5;TEMP=29.4;MOIST=62"
  - [ ] Invalid pH: "NPK=12,7,9;PH=15.5;TEMP=29.4;MOIST=62"
  - [ ] Invalid temp: "NPK=12,7,9;PH=6.5;TEMP=100;MOIST=62"
  - [ ] Invalid moisture: "NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=150"
- [ ] Try to save each
- [ ] Verify:
  - [ ] No success message appears
  - [ ] Marker doesn't change color
  - [ ] Data isn't stored (reload app to verify)

**Expected Result**: Invalid data is silently rejected

## Performance Tests

### Test 12: Large Dataset (Optional)
- [ ] Save data to 50+ markers
- [ ] Verify map still responsive
- [ ] Verify scroll/zoom still smooth
- [ ] Verify marker color retrieval is instant
- [ ] Verify data loading is instant

**Expected Result**: Performance remains good with large dataset

## UI/UX Tests

### Test 13: Dialog Behavior
- [ ] Open Sample Location dialog
- [ ] Verify it can't be dismissed by clicking outside
- [ ] Verify Close button works
- [ ] Verify back button behavior
- [ ] Verify only one dialog shows at a time

**Expected Result**: Dialogs stack properly, close as expected

### Test 14: Map Interaction
- [ ] Verify clicking map (not on marker) closes dialog
- [ ] Verify map is zoomable with dialog open
- [ ] Verify map is scrollable with dialog open
- [ ] Verify rotation controls work with dialog open
- [ ] Verify map controls visible behind dialog

**Expected Result**: Map remains interactive with dialogs open

### Test 15: Visual Clarity
- [ ] Verify marker colors are clearly distinct:
  - [ ] BLUE is obviously different from GREEN
  - [ ] Colors visible in bright sunlight (outdoor test)
  - [ ] Colors visible in dimly lit environment
- [ ] Verify text is readable in dialogs:
  - [ ] Font size adequate
  - [ ] Contrast sufficient
  - [ ] Long values don't overflow
- [ ] Verify success message is eye-catching:
  - [ ] Icon visible
  - [ ] Text readable
  - [ ] Duration noticeable (2 seconds)

**Expected Result**: UI is clear and professional-looking

## Data Accuracy Tests

### Test 16: Data Consistency
- [ ] Save: NPK=10,20,30;PH=7.0;TEMP=25.0;MOIST=50
- [ ] Load it back
- [ ] Verify: All values match exactly
- [ ] No rounding errors on floats
- [ ] Timestamp is reasonable

**Expected Result**: Saved and loaded data match exactly

### Test 17: Coordinate Accuracy
- [ ] Note the exact GPS coordinates of a marker
- [ ] Save data to it
- [ ] Manually check the LatLng in logs
- [ ] Verify coordinates match

**Expected Result**: Location coordinates stored accurately

## Documentation Tests

### Test 18: Help/Documentation
- [ ] Read SOIL_DATA_QUICK_GUIDE.md
- [ ] Verify it accurately describes behavior
- [ ] Try examples from documentation
- [ ] Verify examples work as described

**Expected Result**: Documentation matches implementation

## Final Validation

### Pre-Release Checklist
- [ ] All basic tests passed
- [ ] All advanced tests passed
- [ ] All error handling tests passed
- [ ] No UI crashes or ANR (Application Not Responding)
- [ ] No memory leaks (check Android Profiler)
- [ ] No unhandled exceptions (check logcat)
- [ ] Battery drain is acceptable (Bluetooth receive only)
- [ ] All 6 soil parameters display correctly
- [ ] Marker colors correct (BLUE/GREEN)
- [ ] Success message duration accurate
- [ ] Data persists across recomposition
- [ ] Documentation is accurate
- [ ] Code is clean and commented

## Test Results Summary

| Test Category | Tests | Passed | Failed | Notes |
|---------------|-------|--------|--------|-------|
| Basic Functionality | 3 | [ ] | [ ] | |
| Advanced Features | 4 | [ ] | [ ] | |
| Error Handling | 4 | [ ] | [ ] | |
| Performance | 1 | [ ] | [ ] | |
| UI/UX | 3 | [ ] | [ ] | |
| Data Accuracy | 2 | [ ] | [ ] | |
| Documentation | 1 | [ ] | [ ] | |
| **TOTAL** | **18** | **[ ]** | **[ ]** | |

## Test Execution Notes

Date: _______________
Tester: _______________
Device: _______________
OS Version: _______________

Notes:
```
_________________________________________________________________

_________________________________________________________________

_________________________________________________________________
```

## Known Issues / Pending Fixes

- [ ] None identified

## Approved For Release

- [ ] All tests passed
- [ ] QA sign-off: _______________
- [ ] Date: _______________

---

## Testing Commands (If Using Automated Tests)

```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Run specific test class
./gradlew testDebug --tests com.example.binhi.viewmodel.SoilDataViewModelTest

# Run with coverage
./gradlew testDebug --coverage
```

---

**Testing Status**: Ready to Test ✅

