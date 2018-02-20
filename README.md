# Flower Power BLE Example

This is very basic Bluetooth LE sample application that uses the Parrot Flower Power as a peripheral to show how to connect over Bluetooth LE. In fact I tried to keep the LOC as minimal as possible to only have the Bluetooth LE connection logic in there.

The app connects by using the MAC address of the Flower Power. **MAC address, Service UUID and Characteristic UUID is hard coded and need to be changed as desired.**

The app connects to the Live Service of the Flower Power and reads data from the air temperature characteristic.

The Android API level used is 21 (Android 5.0).

http://global.parrot.com/au/products/flower-power/
