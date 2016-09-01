package org.terminalsupport.jcapiz.moduletester;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.terminalsupport.jcapiz.moduletester.terminalsupport.activities.DevicePickerActivity;
import org.terminalsupport.jcapiz.moduletester.terminalsupport.activities.WaiterActivity;
import org.terminalsupport.jcapiz.moduletester.terminalsupport.activities.WaiterActivityBT;
import org.terminalsupport.jcapiz.moduletester.terminalsupport.bluetooth.BluetoothManager;
import org.terminalsupport.jcapiz.moduletester.terminalsupport.dialogos.DialogoDeConsultaSimple;
import org.terminalsupport.jcapiz.moduletester.terminalsupport.dialogos.ObtenerTexto;
import org.terminalsupport.jcapiz.moduletester.terminalsupport.networking.IOHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Arrays;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private ToggleButton btToggle;
    private ToggleButton wifiToggle;
    private boolean wasServerSelected = false;
    private BluetoothManager manager;

    // El presente código enviará un archivo llamado "calaca.txt"
    // situado en la raiz de la memoria interna del teléfono.
    //
    // Para que lo anterior funcione, la aplicación debe tener
    // permisos de acceso a la memoria externa del teléfono
    // declarado en el manifiesto del proyecto.
    //
    // También es necesario contar con permiso de acceso a INTERNET.
    //
    // La actividad contiene código para entrar en modo receptor
    // y de esa forma recibir el archivo.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageButton ib = (ImageButton) findViewById(R.id.activity_main_on_off);
        wifiToggle = (ToggleButton) findViewById(R.id.activity_main_wifi_toggle);
        btToggle = (ToggleButton) findViewById(R.id.activity_main_bluetooth_toggle);
        wifiToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                btToggle.setChecked(!isChecked);
            }
        });
        btToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                wifiToggle.setChecked(!isChecked);
            }
        });
        manager = new BluetoothManager(this);
        manager.getBluetoothAdapter();
        ib.setOnClickListener(new View.OnClickListener() {

            private boolean clicked = false;

            @Override
            public void onClick(View v) {
                if (!clicked) {
                    clicked = true;
                    if (wifiToggle.isChecked()) {
                        ObtenerTexto ot = new ObtenerTexto();
                        Bundle args = new Bundle();
                        args.putString("mensaje", "Escriba la ip del destino");
                        ot.setArguments(args);
                        ot.setAgenteDeInteraccion(new DialogoDeConsultaSimple.AgenteDeInteraccionConResultado() {
                            @Override
                            public void clickSobreAccionPositiva(DialogFragment dialogo) {
                                final String texto = ((ObtenerTexto) dialogo).obtenerTexto();
                                new Thread() {

                                    @Override
                                    public void run() {
                                        try {
                                            int available;
                                            FileInputStream fis = new FileInputStream(new File(SOURCE + "/calaca.txt"));
                                            available = fis.available();
                                            Log.e("Sender", "there are " + available + " bytes to read before blocking");
                                            DataInputStream entrada = new DataInputStream(fis);
                                            byte[] chunk = new byte[1024];
                                            int length;
                                            Socket socket = new Socket(texto, 23500);
                                            IOHandler ioHandler = new IOHandler(new DataInputStream(socket.getInputStream()), new DataOutputStream(socket.getOutputStream()));
                                            ioHandler.setRate(1024);
                                            int i = 1;
                                            long init = System.currentTimeMillis();
                                            // A partir de este punto se especifica un protocolo para indicar si siguen bytes o no.
                                            while ((length = entrada.read(chunk)) != -1) {
                                                Log.e("Sender", length + " bytes extracted");
                                                ioHandler.sendMessage(Arrays.copyOf(chunk, length));
                                                Log.e("Sender", length + " bytes sent\t" + i++);
                                            }
                                            Log.e("Sender", "Done. Lasted " + ((System.currentTimeMillis() - init) / 60000l) + " minutes.");
                                            entrada.close();
                                            ioHandler.close();
                                            socket.close();
                                            socket = new Socket(texto, 23501);
                                            ioHandler = new IOHandler(new DataInputStream(socket.getInputStream()), new DataOutputStream(socket.getOutputStream()));
                                            ioHandler.sendMessage("Disconnect".getBytes());
                                            ioHandler.close();
                                            socket.close();
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(MainActivity.this, "Archivo enviado con éxito.", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        } catch (final IOException e) {
                                            e.printStackTrace();
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(MainActivity.this, "Hubo un error: " + e.getMessage() + ".", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        } finally {
                                            clicked = false;
                                        }
                                    }
                                }.start();
                            }

                            @Override
                            public void clickSobreAccionNegativa(DialogFragment dialogo) {}
                        });
                        FragmentManager fm = MainActivity.this.getSupportFragmentManager();
                        Log.e("Shu", fm == null ? "Nulo" : "Yop");
                        ot.show(fm, "ObtenerIP");
                    }
                }else{
                    if(!manager.isBluetoothEnabled())
                        manager.enableBluetooth();
                    else
                        launchAction(START_CLIENT_ACTION);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        boolean consumed = false;
        if( item.getItemId() == R.id.main_menu_start_listening){
            if(wifiToggle.isChecked())
                launchWaiterDialog(); // Cuando se toque la opción del menú lanza un dialogo de espera.
            else if(btToggle.isChecked()){
                wasServerSelected = true;
                launchWaiterDialogBluetooth();
            }
            consumed = true;
        }
        return consumed;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data){
        // This method is called when the activity regains control after a launched activity has
        // finished.
        if(resultCode == RESULT_OK){ switch(requestCode) {
            case WAITER_ACTIVITY:
                byte[] content = data.getByteArrayExtra("content"); // Retrieves the file's bytes
                // The way we do things here is not recommended, it only serves as an example of sharing
                // between activities.
                File f = new File(SOURCE, "TerminalSupport");
                if (!f.exists()) {
                    f.mkdirs();
                }
                try {
                    DataOutputStream dos = new DataOutputStream(new FileOutputStream(new File(SOURCE + "/TerminalSupport/new_calaca.txt")));
                    dos.write(content);
                    dos.close();
                    Toast.makeText(this, "Éxito al recibir y escribir el archivo", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Problemas al escribir el archivo", Toast.LENGTH_SHORT).show();
                }
                break;
            case START_CLIENT_ACTION:
                manager.cancelDiscovery();
                SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
                editor.putString("device_addr", data.getStringExtra("device_addr"));
                editor.apply();
                // Inicia la conexión para la transferencia por blutú
                new Thread(){
                    @Override public void run() {
                        BluetoothSocket temp;
                        BluetoothSocket socket = null;
                        BluetoothDevice btDev = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(data.getStringExtra("device_addr"));
                        try {
                            socket = btDev.createRfcommSocketToServiceRecord(UUID.fromString(BluetoothManager.MY_UUID));
                            Log.e("Melchor", "Connecting...");
                            socket.connect();
                            Log.e("Melchor", "Connected, preparing streams...");
                        } catch (IOException e) {
                            Log.e("Tulman", "There was an error while establishing Bluetooth connection. Falling back..", e);
                            Class<?> clazz = socket.getRemoteDevice().getClass();
                            Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};
                            try {
                                Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                                Object[] params = new Object[]{Integer.valueOf(1)};
                                temp = (BluetoothSocket) m.invoke(socket.getRemoteDevice(), params);
                                temp.connect();
                                socket = temp;
                            } catch (Exception e2) {
                                Log.e("TulmanSan", "Couldn't fallback while establishing Bluetooth connection. Stopping app..", e2);
                            }
                            e.printStackTrace();
                        }
                        try {
                            IOHandler ioHandler = new IOHandler(new DataInputStream(socket.getInputStream()), new DataOutputStream(socket.getOutputStream()));
                            ioHandler.setRate(512);
                            FileInputStream fis = new FileInputStream(new File(SOURCE + "/calaca.txt"));
                            DataInputStream entrada = new DataInputStream(fis);
                            byte[] chunk = new byte[512];
                            int length;
                            int i = 1;
                            long init = System.currentTimeMillis();
                            // A partir de este punto se especifica un protocolo para indicar si siguen bytes o no.
                            while ((length = entrada.read(chunk)) != -1) {
                                Log.e("Sender", length + " bytes extracted");
                                ioHandler.sendMessage(Arrays.copyOf(chunk, length));
                                Log.e("Sender", length + " bytes sent\t" + i++);
                            }
                            long interval = (System.currentTimeMillis() - init);
                            long minutes = interval / 60000l;
                            long secs = (interval - minutes * 60000l) / 1000;
                            Log.e("Sender", "Done. Lasted " + minutes + " minutes and " + secs + " seconds.");
                            entrada.close();
                            socket.close();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "Archivo enviado con éxito.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
                break;
            case BluetoothManager.REQUEST_ENABLE_BT:
                if(wasServerSelected) {
                    wasServerSelected = false;
                    launchWaiterDialogBluetooth();
                }else{
                    launchAction(START_CLIENT_ACTION);
                }
                break;
            }
        }else{
            Toast.makeText(this, "Problemas de conexión", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchWaiterDialog() {
        startActivityForResult(new Intent(this, WaiterActivity.class), WAITER_ACTIVITY);
    }

    private void launchWaiterDialogBluetooth() {
        startActivityForResult(new Intent(this, WaiterActivityBT.class), WAITER_ACTIVITY);
    }

    private void launchAction(int action){
        switch(action){
            case START_CLIENT_ACTION:
                Intent i = new Intent(this, DevicePickerActivity.class);
                startActivityForResult(i, START_CLIENT_ACTION);
                break;
        }
    }

    private static final int START_CLIENT_ACTION = 11;
    private static final String SOURCE = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static final int WAITER_ACTIVITY = 01;
}
