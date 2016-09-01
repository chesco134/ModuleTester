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
        // Se trata de la imagen del botón de on/off
        ImageButton ib = (ImageButton) findViewById(R.id.activity_main_on_off);
        // Es el botón de operación en modo wifi. Por default ésta es la opción seleccionada.
        wifiToggle = (ToggleButton) findViewById(R.id.activity_main_wifi_toggle);
        // Es el botón de operación en modo blutú.
        btToggle = (ToggleButton) findViewById(R.id.activity_main_bluetooth_toggle);
        // La siguiente línea agrega mediante una clase anónima la funcionalidad de click.
        // Lo único que hace es simplemente altenrnar selección entre sí y el modo blutú.
        wifiToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                btToggle.setChecked(!isChecked);
            }
        });
        // El siguiente cuerpo de código agrega la funcionalidad de click sobre el elemento.
        btToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                wifiToggle.setChecked(!isChecked);
            }
        });
        // El objeto "manager" ayuda con las tareas de administración del blutú.
        manager = new BluetoothManager(this);
        // La siguiente línea de código sirve para instanciar un BluetoothAdapter dentro de "manager".
        manager.getBluetoothAdapter();
        // La siguiente clase anónima define el comportamiento de click sobre la imagen de on/off.
        // Las buenas prácticas de programación recomiendad que la definición siguiente se normalice
        // en más clases, pero por cuestiones de tiempo se dejó escrito dentro de la clase anónima.
        // Su funcionalidad principal es la de adoptar un comportamiento según sea el modo de operación
        // activo (wifi o blutú).
        ib.setOnClickListener(new View.OnClickListener() {

            // La bandera "clicked" sirve para controlar que el comportamiento se haga una sola vez,
            // e impida que se ejecute repetidas veces mientras el usuario continúe tocando la imagen.
            private boolean clicked = false;

            @Override
            public void onClick(View v) {
                if (!clicked) {
                    clicked = true;
                    // Si el modo de opración activo es wifi:
                    if (wifiToggle.isChecked()) {
                        // Declaramos un objeto de diálogo, que servirá para pedirle texto al usuario.
                        ObtenerTexto ot = new ObtenerTexto();
                        // Bundle es una clase de mapa que permite el paso de valores entre actividades.
                        Bundle args = new Bundle();
                        // El mapa trabaja fundamentalmente con "Strings" como índices.
                        // El siguiente sólo define el mensaje que queremos que muestre el diálogo.
                        args.putString("mensaje", "Escriba la ip del destino");
                        ot.setArguments(args);
                        // Es necesario definir qué debe hacer el diálogo cuando el usuario toque la
                        // opción positiva o negativa del mismo.
                        ot.setAgenteDeInteraccion(new DialogoDeConsultaSimple.AgenteDeInteraccionConResultado() {
                            @Override
                            public void clickSobreAccionPositiva(DialogFragment dialogo) {
                                // Indica que el usuario está de acuerdo con el texto que ingresó.
                                // Lo que esperamos es que ponga la dirección IP del dispositivo móvil que está
                                // esperando el archivo.
                                final String texto = ((ObtenerTexto) dialogo).obtenerTexto();
                                // Lo siguiente es una definición anónima de un hilo, debido a que todas las
                                // tareas de red deben correr en segundo plano (política de operación de Android).
                                // El hilo llevará a cabo la tarea de envío del archivo.
                                new Thread() {

                                    @Override
                                    public void run() {
                                        try {
                                            // La siguiente variable sólo nos dirá los bytes disponibles del archivo a enviar.
                                            int available;
                                            // El objeto fis es un flujo de lectura del archivo referenciado "calaca.txt".
                                            // Puede ser una foto, pero renombrada al nombre y extensión "hardcodeados".
                                            FileInputStream fis = new FileInputStream(new File(SOURCE + "/calaca.txt"));
                                            available = fis.available();
                                            // Lo siguiente es un mensaje que aparece en "Android Monitor".
                                            Log.e("Sender", "there are " + available + " bytes to read before blocking");
                                            // Declaración del objeto principal de lectura del archivo.
                                            // DataInputStream define muchos métodos prácticos para la lectura de elementos
                                            // en bytes.
                                            DataInputStream entrada = new DataInputStream(fis);
                                            // Sigue un arreglo de bytes que sostendrá tramas de un tamaño específico.
                                            // El tamaño actual es debido a que con ese no se presentan problemas de
                                            // envío de valor, pues si se pone algo más grande, eventualmente (aún no sé
                                            // por qué), el valor recibido de entero será muy grande (al parecer
                                            // es uno muy distinto del enviado).
                                            // Dicho tamaño es importante porque la clase IOHandler manda primero
                                            // la cantidad de bytes que está por enviar para que el receptor se prepare
                                            // y posteriormente envía la trama.
                                            byte[] chunk = new byte[1024];
                                            // Length es una variable que sirve para controlar la cantidad de bytes leídos
                                            // del archivo y no mandar "bytes basura".
                                            int length;
                                            // Sigue un objeto de conexión por socket. Se indica la IP ingresada por el usuario
                                            // y el puerto en el que se debe conectar.
                                            Socket socket = new Socket(texto, 23500);
                                            // Instanciación de IOHandler, con los flujos de entrada/salida obtenidos de la
                                            // conexión exitosa.
                                            IOHandler ioHandler = new IOHandler(new DataInputStream(socket.getInputStream()), new DataOutputStream(socket.getOutputStream()));
                                            // Se coloca la taza de transferencia.
                                            ioHandler.setRate(1024);
                                            // La siguiente variable sólo es un índice para contar las tramas enviadas en el log.
                                            int i = 1;
                                            // Guardamos la estampa de tiempo inicial.
                                            long init = System.currentTimeMillis();
                                            // A partir de este punto se leen bytes del archivo y se envían.
                                            while ((length = entrada.read(chunk)) != -1) {
                                                Log.e("Sender", length + " bytes extracted");
                                                ioHandler.sendMessage(Arrays.copyOf(chunk, length));
                                                Log.e("Sender", length + " bytes sent\t" + i++);
                                            }
                                            // Al terminar, se calculan los minutos que tardó la transferencia.
                                            Log.e("Sender", "Done. Lasted " + ((System.currentTimeMillis() - init) / 60000l) + " minutes.");
                                            // IMPORTANTE: Se procede con el cierre de todos los flujos abiertos.
                                            entrada.close();
                                            ioHandler.close();
                                            socket.close();
                                            // La aplicación maneja un segundo puerto de escucha para poder finalizar la transferencia.
                                            socket = new Socket(texto, 23501);
                                            ioHandler = new IOHandler(new DataInputStream(socket.getInputStream()), new DataOutputStream(socket.getOutputStream()));
                                            // Se envía el código de finalización para que el otro extremo termine.
                                            ioHandler.sendMessage("Disconnect".getBytes());
                                            ioHandler.close();
                                            socket.close();
                                            // Debido a que estamos en un hilo clásico de java y no en uno del framework de Android,
                                            // es necesario pedir al hilo de interfaz de usuario que lleve a cabo una interacción
                                            // gráfica; mostrar un mensaje de Toast de éxito.
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
                        // Una vez que han sido definidas las acciones "Aceptar"/"Cancelar" del diálogo,
                        // procedemos a mostrarlo.
                        ot.show(MainActivity.this.getSupportFragmentManager(), "ObtenerIP");
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
