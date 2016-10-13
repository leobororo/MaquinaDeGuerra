package com.cadastro.projeto.maquinadeguerra;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private final static int REQUEST_ENABLE_BT = 1;
    private ListView listViewDispositivos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        verificarExistenciaBluetooth();
    }

    private void verificarExistenciaBluetooth() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            //dispositivo não suporta Bluetooth
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
            }

            listarDispositivos(mBluetoothAdapter);
        }
    }

    private void listarDispositivos(BluetoothAdapter mBluetoothAdapter) {
        listViewDispositivos = (ListView) findViewById(R.id.listViewBluetoothDispositivos);
        ArrayAdapter<String> dispositivosAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        //se existem dispositivos pareados
        if (pairedDevices.size() > 0) {
            // Realiza uma iteração entre todos os dispositivos pareados
            for (BluetoothDevice device : pairedDevices) {
                dispositivosAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }

        listViewDispositivos.setAdapter(dispositivosAdapter);
    }
}
