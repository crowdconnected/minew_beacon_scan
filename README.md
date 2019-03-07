# minew_beacon_scan

## Connection Error

### Replication

With 100+ iBeacons within range:

1. Press the scan button.
2. The app scans for iBeacons for 20 seconds.
3. The app then attempts to connect to each iBeacon, one by one.

### Behaviour 

On Android 6 and 8:

1. 8-12 Connections are sucessful.
2. The code then hangs on the next connection and does not progress.

On Android 7:

1. 8-12 Connections are sucessful.
2. A few NullPointerExceptions are thrown, originating from the BluetoothGattCallback.onConnectionStateChange
3. The code then hangs on the next connection and does not progress.
