package com.cadastro.projeto.maquinadeguerra;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static java.lang.System.arraycopy;

/**
 * Created by leobo on 10/15/2016.
 */
public class ConnectedThread extends Thread {
    private static final String TAG = ConnectedThread.class.getName();

    private final BluetoothSocket socket;
    private final InputStream inputStream;
    private final OutputStream outputStream;

    public ConnectedThread(BluetoothSocket socket) {
        this.socket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Ocorreu um erro ao tentar obter o stream de input e/ou de output");
        }

        inputStream = tmpIn;
        outputStream = tmpOut;
    }

    public void run() {
        // buffer para guardar o stream
        byte[] buffer = new byte[1024];

        // número de bytes retornados pelo método read()
        int bytes;

        while (true) {
            try {
                int bytesAvailable = this.inputStream.available();
                if (bytesAvailable > 0)
                {
                    byte[] packetBytes = new byte[bytesAvailable];
                    this.inputStream.read(packetBytes);

                    int readBufferPosition = 0;
                    byte[] readBuffer = new byte[1024];

                    for (int i = 0; i < bytesAvailable; i++)
                    {
                        byte b = packetBytes[i];
                        if (i % 1024 == 1023 || i == bytesAvailable - 1)
                        {
                            byte[] encodedBytes = new byte[readBufferPosition];
                            arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                            final String data = new String(encodedBytes, "US-ASCII");
                            readBufferPosition = 0;
                        } else {
                            readBuffer[readBufferPosition++] = b;
                        }
                    }
                }

                //String msg = "some message";
                //msg += "\n";
                //outputStream.write(msg.getBytes());
            } catch (IOException e) {
                Log.e(TAG, "Ocorreu um erro ao receber informações do carrinho");
            }
        }
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(byte[] bytes) {
        try {
            outputStream.write(bytes);
        } catch (IOException e) {
        }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            socket.close();
        } catch (IOException e) {
        }
    }
}

