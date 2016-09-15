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
import android.widget.ProgressBar;
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
    private ProgressBar pb;
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
        pb = (ProgressBar) findViewById(R.id.activity_main_progress_bar);
        pb.setIndeterminate(false);
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
                                            pb.setMax(available);
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
                                            pb.setVisibility(View.VISIBLE);
                                            // La siguiente variable sólo es un índice para contar las tramas enviadas en el log.
                                            int i = 1;
                                            // Guardamos la estampa de tiempo inicial.
                                            long init = System.currentTimeMillis();
                                            // A partir de este punto se leen bytes del archivo y se envían.
                                            while ((length = entrada.read(chunk)) != -1) {
                                                Log.e("Sender", length + " bytes extracted");
                                                ioHandler.sendMessage(Arrays.copyOf(chunk, length));
                                                pb.incrementProgressBy(length);
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
                                            pb.setVisibility(View.INVISIBLE);
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
                    }else{
                        // Si no opera en wifi, opera el blutú.
                        // Si no está activado el blutú, despliega un diálogo para pedir permiso de activarlo.
                        if(!manager.isBluetoothEnabled())
                            manager.enableBluetooth();
                        else
                            // Si ya estaba activado procede a lanzar la pantalla de selección del dispositivo al
                            // que hay que conectarse.
                            launchAction(START_CLIENT_ACTION);
                        clicked = false;
                    }
                }
            }
        });
        // Fin de la preparación de los elementos.
    }

    // Se sobreescribe el siguiente método para establecer el menú de la aplicación.
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        // Se hace referencia a un xml dentro de la carpeta "res/menu" que contiene las entradas
        // del menú que queremos usar.
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;
    }

    // Se sobreescribe el siguiente método para establecer qué hacer en caso de la selección de
    // ciertas entradas del menú.
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        boolean consumed = false;
        // En este caso sólo hay una entrada en el menú, y esta al ser seleccionada efectua algo
        // según el modo de operación actual.
        if( item.getItemId() == R.id.main_menu_start_listening){
            if(wifiToggle.isChecked())
                launchWaiterDialog(); // Cuando se toque la opción del menú lanza un dialogo de espera.
            else if(btToggle.isChecked()){
                wasServerSelected = true;
                if(!manager.isBluetoothEnabled())
                    manager.enableBluetooth();
                else
                    launchWaiterDialogBluetooth(); // Lo mismo que la anterior pero en modo blutú.
            }
            // Se indica que el item fue procesado por la actividad.
            consumed = true;
        }
        return consumed;
    }

    // El siguiente método debe sobreescribirse cuando se espera que una actividad lanzada por la
    // presente devuelva un resultado.
    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data){
        // This method is called when the activity regains control after a launched activity has
        // finished. (Sí, también escribo en inglés pero no hay bronca ¿o sí?)
        if(resultCode == RESULT_OK){ switch(requestCode) {
            // Originalmente escribí el código para que la actividad de espera recibiera tooodos los
            // bytes y luego los devolviera pero ¡oh sorpresa!, en ambientes Android la memoria es
            // muy limitada, y en caso de archivos grandes no alcanzaría la memoria para devolver
            // la imagen completa a la actividad principal (ésta, sin albur).
            case WAITER_ACTIVITY:
                // Del parámetro "data" de tipo "Intent" se obtienen los datos que preparó la
                // actividad lanzada con la bandera WAITER_ACTIVITY para nosotros.
                byte[] content = data.getByteArrayExtra("content"); // Retrieves the file's bytes
                // The way we do things here is not recommended, it only serves as an example of sharing
                // between activities. (Lo que está escrito arriba pero en español)
                // La siguiente línea de código permite crear la carpeta "TerminalSupport" para que
                // ahí dentro resida el fichero recibido.
                File f = new File(SOURCE, "TerminalSupport");
                if (!f.exists()) {
                    f.mkdirs();
                }
                // Sigue la tarea de escritura de los bytes de la imagen en el archivo.
                // Actualmente para no crear buffers grandes en RAM, los bytes son escritos
                // directamente al archivo con forme son recibidos.
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
            // El siguiente caso es por si la actividad de la que estamos volviendo es la de
            // selección del blutú al que vamos a enviar el archivo.
            case START_CLIENT_ACTION:
                // Esta acción es crucial para que nuestra aplicación no drene la batería del teléfono.
                manager.cancelDiscovery();
                // No se ocupa, pero dejo una muestra del uso de un archivo de opciones, dedicado
                // a nuestra aplicación. Es manejado directamente por el sistema operativo.
                SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
                // En él colocamos la dirección mac del dispositivo blutú que seleccionamos.
                editor.putString("device_addr", data.getStringExtra("device_addr"));
                // No olvidamos confirmar los cambios.
                editor.apply();
                // Inicia la conexión para la transferencia por blutú
                new Thread(){
                    @Override public void run() {
                        BluetoothSocket temp;
                        BluetoothSocket socket = null;
                        // Así se crea un objeto de "BluetoothDevice" a partir del string de la mac.
                        BluetoothDevice btDev = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(data.getStringExtra("device_addr"));
                        // El monstruo que sigue a continuación es para conectarnos por medio de blutú
                        // al dispositovo de la mac que seleccionamos anteriormente.
                        try {
                            socket = btDev.createRfcommSocketToServiceRecord(UUID.fromString(BluetoothManager.MY_UUID));
                            // Los logs son banderas de depuración.
                            Log.e("Melchor", "Connecting...");
                            socket.connect();
                            Log.e("Melchor", "Connected, preparing streams...");
                        } catch (IOException e) {
                            // Si no fue posible conectarse a la buena, lo hacemos a la mala.
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
                                // Si llegamos aquí es que de plano se seleccionó un dispositivo
                                // que no está al alcance (dispositivos recordados).
                                Log.e("TulmanSan", "Couldn't fallback while establishing Bluetooth connection. Stopping app..", e2);
                            }
                            e.printStackTrace();
                        }
                        // Una vez que terminamos el ritual de conexión, procedemos a intentar
                        // enviar el archivo.
                        try {
                            // Lo primero que hay que intentar es armar el objeto IOHandler,
                            // que permitirá el envío del archivo en bloques.
                            // Si anteriormente no pudismos crear el socket blutú, aquí hay un
                            // error y saltamos inmediatamente al "catch", terminando la operación.
                            IOHandler ioHandler = new IOHandler(new DataInputStream(socket.getInputStream()), new DataOutputStream(socket.getOutputStream()));
                            // Por pruebas preliminares, se determinó que con esta taza de bytes
                            // no hay problema de escritura de entero.
                            ioHandler.setRate(512);
                            // Lo que sigue es lo mismo que está en la parte de wifi.
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
                            // Aquí el formato de tiempo está más nice.
                            Log.e("Sender", "Done. Lasted " + minutes + " minutes and " + secs + " seconds.");
                            entrada.close();
                            socket.close();
                            // Notificamos que terminamos correctamente.
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "Archivo enviado con éxito.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                            runOnUiThread(new Runnable(){ @Override public void run(){ Toast.makeText(MainActivity.this, "¿Por qué no seleccionó un dispositivo correcto?", Toast.LENGTH_SHORT).show(); }});
                        }
                    }
                }.start();
                break;
            // Si procedemos del diálogo de activación de blutú aquí entraremos.
            case BluetoothManager.REQUEST_ENABLE_BT:
                // La bandera "wasServerSelected" es para saber si el diálogo fue lanzado desde
                // el modo emisor o receprot.
                if(wasServerSelected) {
                    // Si fue desde el receptor, lanzamos la actividad de espera del archivo.
                    wasServerSelected = false;
                    launchWaiterDialogBluetooth();
                }else{
                    // Si no, lanzamos la actividad de selección del dispositivo al cuál enviaremos.
                    launchAction(START_CLIENT_ACTION);
                }
                break;
            }
        }else{
            // Just something else.
            Toast.makeText(this, "Problemas de conexión", Toast.LENGTH_SHORT).show();
        }
    }

    // Es recomendable seprar las acciones de inicio de actividades en métodos como los siguientes tres.
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

    // Banderas útiles en la actividad.
    private static final int START_CLIENT_ACTION = 11;
    private static final String SOURCE = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static final int WAITER_ACTIVITY = 01;
}
