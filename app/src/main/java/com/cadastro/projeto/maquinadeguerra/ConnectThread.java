package com.cadastro.projeto.maquinadeguerra;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by leobo on 10/12/2016.
 */
public class ConnectThread extends Thread {

    private static final String TAG = ConnectThread.class.getName();
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private BluetoothSocket mmSocket;;

    private final BluetoothDevice device;

    public ConnectThread(BluetoothDevice device) {
        this.device = device;
    }

    public void run() {
        // Cancela a descoberta, pois atrasa a conexão
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

        // Usa BluetoothSocket para se conectar a um determinado BluetoothDevice
        try {
            // MY_UUID é a string UUID da aplicação, também utilizado do lado servidor
            mmSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            Log.e(TAG, "Socket criado com sucesso");

            try {
                // Conecta o dispositivo através do socket. Irá bloquear até que a conexão seja efetivada ou lance uma exceção
                mmSocket.connect();
                Log.e(TAG, "Conexão estabelecida com sucesso");

                // Gerencia a conexão (em uma thread separada)
                manageConnectedSocket(mmSocket);
            } catch (IOException connectException) {
                try {
                    Log.e(TAG, "Tentando fallback...");

                    mmSocket =(BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[] {Integer.TYPE}).invoke(device,1);
                    mmSocket.connect();
                    Log.e(TAG, "Conexão estabelecida com sucesso");
                    //mmSocket.isConnected()
                    // Gerencia a conexão (em uma thread separada)
                    manageConnectedSocket(mmSocket);
                } catch (Exception e2) {
                    Log.e(TAG, "Não foi possível estabelecer uma conexão");
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Erro criando socket");
        }
    }

    /**
     * Fecha o socket de conexão
     */
    public void cancel() {
        try {
            if (mmSocket != null && mmSocket.isConnected()) {
                mmSocket.close();
            }
        } catch (IOException e) { }
    }

    /**
     * Cria uma thread para consumo e envio de dados
     * @param mmSocket BluetoothSocket
     */
    private void manageConnectedSocket(BluetoothSocket mmSocket) {
        ConnectedThread connectedThread = new ConnectedThread(mmSocket);
        connectedThread.start();
    }
}
