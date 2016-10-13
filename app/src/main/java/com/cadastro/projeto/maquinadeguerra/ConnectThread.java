package com.cadastro.projeto.maquinadeguerra;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;

/**
 * Created by leobo on 10/12/2016.
 */
public class ConnectThread extends Thread {
    public static final java.util.UUID MY_UUID = null;
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;

    public ConnectThread(BluetoothDevice device) {
        // Usa um objeto temporário, depois atribuído a mmSocket,
        // uma vez que mmSocket é final
        BluetoothSocket tmp = null; mmDevice = device;

        // Usa BluetoothSocket para se conectar a um determinado BluetoothDevice
        try {
            // MY_UUID é a string UUID da aplicação, também utilizado do lado servidor
            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {

        }

        mmSocket = tmp;
    }

    public void run() {
        // Cancela a descoberta, pois atrasa a conexão
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        try {
            // Conecta o dispositivo através do socket. Irá bloquear até que a conexão seja efetivada
            // ou lance uma exceção
            mmSocket.connect();
        } catch (IOException connectException) {
            // Impossível conectar; feche a aplicação e saia
            try {
                mmSocket.close();
            } catch (IOException closeException) {
            }
            return;
        }
        // Gerencia a conexão (em uma thread separada)
        manageConnectedSocket(mmSocket);
    }

    private void manageConnectedSocket(BluetoothSocket mmSocket) {

    }
}
