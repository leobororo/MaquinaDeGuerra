package com.cadastro.projeto.maquinadeguerra;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import static android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE;
import static android.content.Intent.ACTION_MAIN;
import static android.content.Intent.CATEGORY_HOME;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;

public class MainActivity extends AppCompatActivity {

    private final static int REQUEST_ENABLE_BT = 1;
    private static final String NOME_CARRINHO = "SF-009B";
    private BluetoothAdapter mBluetoothAdapter;
    private ListView listViewDispositivos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        obtainBluetoothAdapter();
        enableBluetoothAdapterAndProgress();
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

    private void progress() {
        findDevice();
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
     * Redirects user to the home screen
     */
    private void goToHomeScreen() {
        Intent intent = new Intent(ACTION_MAIN);
        intent.addCategory(CATEGORY_HOME);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Procura o dispositivo bluetooth do carrinho
     */
    private void findDevice() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() == 0) {
            finishApp("É preciso parear o carrinho para utilizar o aplicativo.");
        } else {
            BluetoothDevice carrinho  = findDeviceInSet(pairedDevices);

            if (carrinho != null) {
                createListViewCarrinho(carrinho);

            } else {
                finishApp("É preciso parear o carrinho para utilizar o aplicativo.");
            }
        }
    }

    /**
     * Apresenta uma ListView com o nome do carrinho e o MAC address
     * @param carrinho BluetoothDevice
     */
    private void createListViewCarrinho(final BluetoothDevice carrinho) {
        listViewDispositivos = (ListView) findViewById(R.id.listViewBluetoothDispositivos);
        ArrayAdapter<String> dispositivosAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        dispositivosAdapter.add(carrinho.getName() + "\n" + carrinho.getAddress());
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
            if (NOME_CARRINHO.equals(device.getName())) {
                return device;
            }
        }

        return null;
    }
}
