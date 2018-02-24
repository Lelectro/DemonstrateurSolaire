package ch.heigvd.huguelet.demonstrateursolaire.ble;

public interface IBLEManagement {

    void doOnBLEConnected ();

    void doOnBLEDataReceived (final byte[] txValues);

    void doOnBLEDisconnected();

    void sendDataOverBLE(String message);
}
