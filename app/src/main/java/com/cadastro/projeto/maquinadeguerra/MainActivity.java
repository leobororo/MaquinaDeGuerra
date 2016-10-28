package com.cadastro.projeto.maquinadeguerra;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.cadastro.projeto.maquinadeguerra.Utilitarios.ConverterTextoVoz;
import com.cadastro.projeto.maquinadeguerra.Utilitarios.TextoMensagemVoz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE;
import static android.content.Intent.ACTION_MAIN;
import static android.content.Intent.CATEGORY_HOME;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH;
import static android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL;
import static android.speech.RecognizerIntent.EXTRA_PROMPT;
import static android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_UP;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;
import static com.cadastro.projeto.maquinadeguerra.Utilitarios.TextoMensagemVoz.MENSAGEM_VOZ_BEM_VINDO;
import static com.cadastro.projeto.maquinadeguerra.Utilitarios.TextoMensagemVoz.MENSAGEM_VOZ_IR_PARA_DIREITA;
import static com.cadastro.projeto.maquinadeguerra.Utilitarios.TextoMensagemVoz.MENSAGEM_VOZ_IR_PARA_ESQUERDA;
import static com.cadastro.projeto.maquinadeguerra.Utilitarios.TextoMensagemVoz.MENSAGEM_VOZ_IR_PARA_FRENTE;
import static com.cadastro.projeto.maquinadeguerra.Utilitarios.TextoMensagemVoz.MENSAGEM_VOZ_IR_PARA_TRAS;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String TAG = MainActivity.class.getName();

    // MAC address do carrinho
    private static final String MAC_ADDRESS = "98:D3:31:FD:29:75";

    // constantes para controle de activities for result
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final int REQUEST_VOICE_CONTROL = 1234;

    // constantes para controle de mensagens enviadas para o Handler
    private static final int MESSAGE_READ = 1;
    private static final int SUCCESS_CONNECT = 0;

    // constantes para controle da imagem de feedback da intensidade de luz
    private static final String DIA = "Dia";
    private static final String TARDE = "Tarde";
    private static final String NOITE = "Noite";

    // constantes que representam mensagens que serão enviadas para o arduino
    public static final char FORWARD_COMMAND = 'F';
    public static final char BACKWARD_COMMAND = 'B';
    public static final char RIGHT_COMMAND = 'R';
    public static final char LEFT_COMMAND = 'L';
    public static final char STOP_COMMAND = 'S';

    // adapter do bluetooth
    private BluetoothAdapter mBluetoothAdapter;

    // dispositivo bluetooth do carrinho
    private BluetoothDevice carrinho;

    // thread que mantém o socket de conexão via bluetooth
    private ConnectedThread connectedThread;

    // views utilizadas para comandos dos usuários
    private ImageView forwardImageView = null;
    private ImageView backwardImageView = null;
    private ImageView stopImageView = null;
    private ImageView leftImageView = null;
    private ImageView rightImageView = null;
    private ImageView speakImageView = null;
    private ImageView lightIntensityImageView = null;
    private ImageView voiceControlImageView = null;

    // variável sentinela que guarda a última imagem
    private String lastImage = DIA;

    // engine para comunicação por voz proveniente do aplicativo
    private TextToSpeech textToSpeech = null;

    // variável sentinela que indica se o controle por voz está ativado ou não
    private boolean voiceControlActive = true;

    /**
     * Handler que permite que as thread se comuniquem com a activity main
     */
    private Handler mHandler = new Handler() {
        private final String TAG = Handler.class.getName();

        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "dentro do handler, what: " + msg.what);
            super.handleMessage(msg);

            switch (msg.what) {
                case SUCCESS_CONNECT:

                    // envia toast de conexão com sucesso
                    makeText(MainActivity.this, "Conectado com sucesso", LENGTH_SHORT).show();

                    // cria e inicia a thread que tratará do envio e recebimento de mensagens
                    connectedThread = new ConnectedThread((BluetoothSocket) msg.obj);
                    connectedThread.start();

                    // remove da tela a list view de carrinhos
                    ListView listView = (ListView) findViewById(R.id.listViewBluetoothDispositivos);
                    listView.setVisibility(View.GONE);

                    // dá boas vindas ao usuário
                    ConverterTextoVoz.texto(MENSAGEM_VOZ_BEM_VINDO, textToSpeech, voiceControlActive);

                    //Configura a visibilidade dos botões
                    setButtonsVisibility(true);

                    break;
                case MESSAGE_READ:
                    // exibe na tela o valor retornado pelo sensor de luminosidade
                    String string = (String) msg.obj;
                    TextView view = (TextView) findViewById(R.id.sensorLCD);

                    // modifica a imagem de feedback de acordo com a intensidade da luz
                    verificarPeriodoDia((String) view.getText());
                    view.setText(string);

                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textToSpeech = new TextToSpeech(this, this);

        obtainBluetoothAdapter();

        enableBluetoothAdapterAndProgress();

        obtainImageViewInstances();

        setButtonsVisibility(false);

        setButtons();

        verifyVoiceRecognizer();
    }

    /**
     * Verifica a presença da engine para reconhecimento de voz
     */
    private void verifyVoiceRecognizer() {
        Intent recognizerIntent = new Intent(ACTION_RECOGNIZE_SPEECH);
        List<ResolveInfo> activities = getPackageManager().queryIntentActivities(recognizerIntent, 0);

        if (activities.size() == 0) {
            makeText(getApplicationContext(), "Reconhecedor de voz nao encontrado", LENGTH_LONG).show();
        }
    }

    /**
     * Configura as mensagens que serão enviadas ao clicar nos botões
     */
    private void setButtons() {
        ConverterTextoVoz.texto(MENSAGEM_VOZ_BEM_VINDO, textToSpeech, voiceControlActive);

        //configura evento da image view de comando para frente
        forwardImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case ACTION_DOWN:
                        goForward();
                        return true;

                    case ACTION_UP:
                        connectedThread.write(STOP_COMMAND);
                        break;
                }

                return false;
            }
        });

        //configura evento da image view de comando para trás
        backwardImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case ACTION_DOWN:
                        goBackward();
                        return true;

                    case ACTION_UP:
                        connectedThread.write(STOP_COMMAND);
                        break;
                }

                return false;
            }
        });

        //configura evento da image view de comando para direita
        rightImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case ACTION_DOWN:
                        goToTheRight();
                        return true;

                    case ACTION_UP:
                        connectedThread.write(STOP_COMMAND);
                        break;
                }

                return false;
            }
        });

        //configura evento da image view de comando para esquerda
        leftImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case ACTION_DOWN:
                        goToTheLeft();
                        return true;

                    case ACTION_UP:
                        connectedThread.write(STOP_COMMAND);
                        break;
                }

                return false;
            }
        });

        //configura evento da image view de comando parar
        stopImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectedThread.write(STOP_COMMAND);
            }
        });

        //configura evento da image view para enviar comando de voz
        speakImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startVoiceRecognitionActivity();
            }
        });

        //configura evento da image view para ativar comandos por voz
        voiceControlImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (voiceControlActive){
                    voiceControlActive = false;
                    voiceControlImageView.setImageResource(R.drawable.ic_volume_desativado);
                } else {
                    voiceControlActive = true;
                    voiceControlImageView.setImageResource(R.drawable.ic_volume_ativado);
                }
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
     *
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
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH);
        } else {
            progress();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                progress();
            } else {
                finishApp("O Bluetooth precisa estar habilitado para utilizar o aplicativo.");
            }
        }
        if (requestCode == REQUEST_VOICE_CONTROL) {
            if (resultCode == RESULT_OK) {
                dealWithVoiceCommand(data);
            }
        }

    }

    /**
     * Trata dos comandos de voz enviados pelo usuário
     * @param data Intent
     */
    private void dealWithVoiceCommand(Intent data) {
        ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

        for (int i = 0; i < matches.size(); i++) {
            if (matches.get(i).equalsIgnoreCase("Conectar")) {
                ConverterTextoVoz.texto(TextoMensagemVoz.MENSAGEM_VOZ_CONECTANDO, textToSpeech, voiceControlActive);
                ConnectThread connectThread = new ConnectThread(carrinho);
                connectThread.start();
                break;
            }

            if (matches.get(i).equalsIgnoreCase("em frente")) {
                goForward();

                break;
            }

            if (matches.get(i).equalsIgnoreCase("para traz")) {
                goBackward();

                break;
            }

            if (matches.get(i).equalsIgnoreCase("parar")) {
                connectedThread.write(STOP_COMMAND);

                break;
            }

            if (matches.get(i).equalsIgnoreCase("direita")) {
                goToTheRight();
                break;
            }

            if (matches.get(i).equalsIgnoreCase("esquerda")) {
                goToTheLeft();
                break;
            }

            if (matches.get(i).equalsIgnoreCase("sair")) {
                this.finish();
                break;
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
            carrinho = findDeviceInSet(pairedDevices);

            if (carrinho == null) {
                finishApp("É preciso parear o carrinho para utilizar o aplicativo.");
            } else {
                createListViewCarrinho(carrinho);
            }
        }
    }

    /**
     * Apresenta uma ListView com o nome do carrinho e o MAC address
     *
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
                ConverterTextoVoz.texto(TextoMensagemVoz.MENSAGEM_VOZ_CONECTANDO, textToSpeech, voiceControlActive);
                ConnectThread connectThread = new ConnectThread(carrinho);
                connectThread.start();
            }
        });
    }

    /**
     * Itera em um conjunto de dispositivos bluetooth procurando pelo carrinho
     *
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


    //Iniciar a tela de comando por voz
    private void startVoiceRecognitionActivity() {
        Intent intent = new Intent(ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(EXTRA_LANGUAGE_MODEL, LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(EXTRA_PROMPT, "Fala para a Máquina de guerra o que fazer!!!");

        startActivityForResult(intent, REQUEST_VOICE_CONTROL);
    }

    /**
     * Modifica a ImageView de intensidade de luz de acordo com o valor enviado pelo parâmetro String
     * @param valor String
     */
    private void verificarPeriodoDia(String valor){
        if (!valor.isEmpty()) {
            int intensidade = Integer.valueOf(valor);
            ImageView ivIntLuz = (ImageView) findViewById(R.id.ivIntLuz);

            if (intensidade < 600 ) {
                if (!lastImage.equals(DIA)) {
                    ivIntLuz.setImageResource(R.drawable.ic_dia);
                    lastImage = DIA;
                }

            } else if (intensidade > 600 && intensidade < 1000) {
                if (!lastImage.equals(TARDE)) {
                    ivIntLuz.setImageResource(R.drawable.ic_tarde);
                    lastImage = TARDE;
                }
            } else {
                if (!lastImage.equals(NOITE)) {
                    ivIntLuz.setImageResource(R.drawable.ic_noite);
                    lastImage = NOITE;
                }
            }
        }
    }

    /**
     * Configura a visibilidade dos botões de comando do carrinho
     * @param conectado boolean
     */
    private void setButtonsVisibility(boolean conectado){
        if (!conectado) {
            forwardImageView.setVisibility(INVISIBLE);
            backwardImageView.setVisibility(INVISIBLE);
            stopImageView.setVisibility(INVISIBLE);
            leftImageView.setVisibility(INVISIBLE);
            rightImageView.setVisibility(INVISIBLE);
            lightIntensityImageView.setVisibility(INVISIBLE);
        } else {
            forwardImageView.setVisibility(VISIBLE);
            backwardImageView.setVisibility(VISIBLE);
            stopImageView.setVisibility(VISIBLE);
            leftImageView.setVisibility(VISIBLE);
            rightImageView.setVisibility(VISIBLE);
            lightIntensityImageView.setVisibility(VISIBLE);
        }
    }

    /**
     * Método resposável por enviar o comando de ir para frente para o arduino através da thread
     * que mantém o socket de conexão com o arduino, além de enviar a mensagem o comando é
     * convertido para mensagem de voz através da engine TextToSpeech
     */
    private void goForward(){
        ConverterTextoVoz.texto(MENSAGEM_VOZ_IR_PARA_FRENTE, textToSpeech, voiceControlActive);
        connectedThread.write(FORWARD_COMMAND);
    }

    /**
     * Método resposável por enviar o comando de ir para trás para o arduino através da thread
     * que mantém o socket de conexão com o arduino, além de enviar a mensagem o comando é
     * convertido para mensagem de voz através da engine TextToSpeech
     */
    private void goBackward(){
        connectedThread.write(BACKWARD_COMMAND);
        ConverterTextoVoz.texto(MENSAGEM_VOZ_IR_PARA_TRAS, textToSpeech, voiceControlActive);
    }

    /**
     * Método resposável por enviar o comando de ir para a direita para o arduino através da thread
     * que mantém o socket de conexão com o arduino, além de enviar a mensagem o comando é
     * convertido para mensagem de voz através da engine TextToSpeech
     */
    private void goToTheRight(){
        connectedThread.write(RIGHT_COMMAND);
        ConverterTextoVoz.texto(MENSAGEM_VOZ_IR_PARA_DIREITA, textToSpeech, voiceControlActive);
    }

    /**
     * Método resposável por enviar o comando de ir para a esquerda para o arduino através da thread
     * que mantém o socket de conexão com o arduino, além de enviar a mensagem o comando é
     * convertido para mensagem de voz através da engine TextToSpeech
     */
    private void goToTheLeft(){
        connectedThread.write(LEFT_COMMAND);
        ConverterTextoVoz.texto(MENSAGEM_VOZ_IR_PARA_ESQUERDA, textToSpeech, voiceControlActive);
    }


    /**
     * Obtém referências para as instâncias das image views que serão utilizadas para controlar a tela
     */
    private void obtainImageViewInstances() {
        forwardImageView = (ImageView) findViewById(R.id.ivF);
        backwardImageView = (ImageView) findViewById(R.id.ivB);
        stopImageView = (ImageView) findViewById(R.id.ivS);
        leftImageView = (ImageView) findViewById(R.id.ivE);
        rightImageView = (ImageView) findViewById(R.id.ivD);
        speakImageView = (ImageView) findViewById(R.id.ivFalar);
        voiceControlImageView = (ImageView) findViewById(R.id.ivAtivaVoz);
        lightIntensityImageView = (ImageView) findViewById(R.id.ivIntLuz);
    }


    @Override
    public void onInit(int status) {}

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

                        mmSocket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[]{Integer.TYPE}).invoke(device, 1);
                        mmSocket.connect();
                        Log.e(TAG, "Conexão estabelecida com sucesso");

                        // Envia o socket para outra thread utilizar
                        manageConnectedSocket(mmSocket);
                    } catch (Exception connectException2) {
                        Log.e(TAG, "Não foi possível estabelecer uma conexão");
                        ConverterTextoVoz.texto(TextoMensagemVoz.MENSAGEM_VOZ_ERRO_AO_CONECTAR, textToSpeech, voiceControlActive);
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
         *
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
                    while ((line = in.readLine()) != null) {
                        mHandler.obtainMessage(MESSAGE_READ, 10, -1, line).sendToTarget();
                    }

                } catch (IOException e) {
                    Log.e(TAG, "Ocorreu um erro ao receber informações do carrinho");
                }
            }
        }

        /**
         * Método para envio de mensagens
         *
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
