package com.cadastro.projeto.maquinadeguerra;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by leobo on 10/12/2016.
 */
public class AcceptThread extends Thread {
    public static final String NAME = "";
    public static final UUID MY_UUID = null;

    private final BluetoothServerSocket mmServerSocket;

    public AcceptThread() {
        BluetoothServerSocket tmp = null;

        try {
            tmp = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
        } catch (IOException e) {

        }
        mmServerSocket = tmp;
    }

    public void run() {
        BluetoothSocket socket = null;

        // Fica escutando até que ocorra uma exceção ou um socket é retornado
        while (true)
        {
            try
            {
                socket = mmServerSocket.accept();
            } catch (IOException e)
            {
                break;
            }
            // Se a conexão foi aceita
            if (socket != null) {
                // Gerencia a conexão (em uma thread separada)
                manageConnectedSocket(socket);

                try {
                    mmServerSocket.close();
                } catch (IOException e) {
                    break;
                }
                break;
            }
        }
    }

    private void manageConnectedSocket(BluetoothSocket socket) {

    }
}
