package com.cadastro.projeto.maquinadeguerra;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE;
import static android.content.Intent.ACTION_MAIN;
import static android.content.Intent.CATEGORY_HOME;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;
import static java.lang.System.arraycopy;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();
    private static final String MAC_ADDRESS = "98:D3:31:FD:29:75";

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int MESSAGE_READ = 1;
    private static final int SUCCESS_CONNECT = 0;

    private BluetoothAdapter mBluetoothAdapter;
    private ConnectedThread connectedThread;

    private Handler mHandler = new Handler(){
        private final String TAG = Handler.class.getName();

        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "in handler, what: " + msg.what);
            super.handleMessage(msg);

            switch(msg.what){
                case SUCCESS_CONNECT:
                    // Cria e inicia a thread que tratará do envio e recebimento de mensagens
                    makeText(MainActivity.this, "Conectado com sucesso", LENGTH_SHORT).show();
                    connectedThread = new ConnectedThread((BluetoothSocket)msg.obj);
                    connectedThread.start();
                    break;
                case MESSAGE_READ:
                    //atualiza o valor retornado pelo sensor na tela
                    String string = (String)msg.obj;
                    TextView view = (TextView) findViewById(R.id.sensorLCD);
                    view.setText(string);
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setButtons();
        obtainBluetoothAdapter();
        enableBluetoothAdapterAndProgress();
    }

    /**
     * Configura as mensagens que serão enviadas ao clicar nos botões
     */
    private void setButtons() {
        Button btnF = (Button) findViewById(R.id.btnF);
        btnF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectedThread.write('F');
            }
        });

        Button btnB = (Button) findViewById(R.id.btnB);
        btnB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectedThread.write('B');
            }
        });

        Button btnS = (Button) findViewById(R.id.btnS);
        btnS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectedThread.write('S');
            }
        });
    }

    /**
     * Obtém uma referência para o BluetoothAdapter
     */
    private void obtainBluetoothAdapter() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            finishApp("Seu dispositivo não suporta Bluetooth.");
        }
    }

    /**
     * Envia toast para o usuário, redireciona para a tela de início e finaliza o aplicativo
     * @param mensagem String
     */
    private void finishApp(String mensagem) {
        makeText(MainActivity.this, mensagem, LENGTH_SHORT).show();
        goToHomeScreen();
        finish();
    }

    /**
     * Habilita o bluetooth do celular e dá progresso
     */
    private void enableBluetoothAdapterAndProgress() {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
        } else {
            progress();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                progress();
            } else {
                finishApp("O Bluetooth precisa estar habilitado para utilizar o aplicativo.");
            }
        }
    }

    /**
     * Redireciona para a tela inicial do dispositivo
     */
    private void goToHomeScreen() {
        Intent intent = new Intent(ACTION_MAIN);
        intent.addCategory(CATEGORY_HOME);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Procura o dispositivo bluetooth do carrinho e avança exibindo os dados do carrinho
     */
    private void progress() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() == 0) {
            finishApp("É preciso parear o carrinho para utilizar o aplicativo.");
        } else {
            BluetoothDevice carrinho  = findDeviceInSet(pairedDevices);

            if (carrinho == null) {
                finishApp("É preciso parear o carrinho para utilizar o aplicativo.");
            } else {
                createListViewCarrinho(carrinho);
            }
        }
    }

    /**
     * Apresenta uma ListView com o nome do carrinho e o MAC address
     * @param carrinho BluetoothDevice
     */
    private void createListViewCarrinho(final BluetoothDevice carrinho) {
        ArrayAdapter<String> dispositivosAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        dispositivosAdapter.add(carrinho.getName() + "\n" + carrinho.getAddress());

        ListView listViewDispositivos = (ListView) findViewById(R.id.listViewBluetoothDispositivos);
        listViewDispositivos.setAdapter(dispositivosAdapter);
        listViewDispositivos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ConnectThread connectThread =  new ConnectThread(carrinho);
                connectThread.start();
            }
        });
    }

    /**
     * Itera em um conjunto de dispositivos bluetooth procurando pelo carrinho
     * @param pairedDevices Set<BluetoothDevice>
     * @return BluetoothDevice
     */
    private BluetoothDevice findDeviceInSet(Set<BluetoothDevice> pairedDevices) {
        for (BluetoothDevice device : pairedDevices) {
            if (MAC_ADDRESS.equals(device.getAddress())) {
                return device;
            }
        }

        return null;
    }

    /**
     * Classe interna que representa a thread que fará a conexão com o dispositivo bia bluetooth
     */
    private class ConnectThread extends Thread {

        private final String TAG = ConnectThread.class.getName();
        private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        private final BluetoothDevice device;

        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;
        }

        public void run() {
            // Cancela a descoberta, pois atrasa a conexão
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

            // Usa BluetoothSocket para se conectar a um determinado BluetoothDevice
            try {
                // Usa BluetoothSocket para se conectar a um determinado BluetoothDevice, utiliza o MY_UUID para identificar esta conexão via socket
                mmSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                Log.e(TAG, "Socket criado com sucesso");

                try {
                    // Conecta o dispositivo através do socket. Irá bloquear até que a conexão seja efetivada ou lance uma exceção
                    mmSocket.connect();
                    Log.e(TAG, "Conexão estabelecida com sucesso");

                    // Envia o socket para outra thread utilizar
                    manageConnectedSocket(mmSocket);
                } catch (IOException connectException) {
                    try {
                        Log.e(TAG, "Tentando fallback...");

                        mmSocket =(BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[] {Integer.TYPE}).invoke(device, 1);
                        mmSocket.connect();
                        Log.e(TAG, "Conexão estabelecida com sucesso");

                        // Envia o socket para outra thread utilizar
                        manageConnectedSocket(mmSocket);
                    } catch (Exception connectException2) {
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
            } catch (IOException e) {
                Log.e(TAG, "Não foi possível fechar a conexão");
            }
        }

        /**
         * Envia mensagem para o Handler com o socket de conexão
         * @param mmSocket BluetoothSocket
         */
        private void manageConnectedSocket(BluetoothSocket mmSocket) {
            mHandler.obtainMessage(SUCCESS_CONNECT, 10, -1, mmSocket).sendToTarget();
        }
    }

    /**
     * Classe que representa a thread que ficará de fato responsávl pelo envio e recebimento de mensagens via bluetooth
     */
    class ConnectedThread extends Thread {
        private final String TAG = ConnectedThread.class.getName();

        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

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
            while (true) {
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
                    String line;

                    //envia para o handler as mensagens recebidas pela thread
                    while((line = in.readLine()) != null) {
                        mHandler.obtainMessage(MESSAGE_READ, 10, -1, line).sendToTarget();
                    }

                } catch (IOException e) {
                    Log.e(TAG, "Ocorreu um erro ao receber informações do carrinho");
                }
            }
        }

        /**
         * Método para envio de mensagens
         * @param comando char representando um comando
         */
        public void write(char comando) {
            PrintStream ps = new PrintStream(outputStream);
            ps.write(comando);
        }

        /**
         * Método para encerramento da conexão
         */
        public void cancel() {
            try {
                if (socket != null && socket.isConnected()) {
                    socket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Não foi possível fechar a conexão");
            }
        }
    }
}
