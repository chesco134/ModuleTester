package org.terminalsupport.jcapiz.moduletester;


import android.content.Intent;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import org.terminalsupport.jcapiz.moduletester.terminalsupport.activities.WaiterActivity;
import org.terminalsupport.jcapiz.moduletester.terminalsupport.dialogos.DialogoDeConsultaSimple;
import org.terminalsupport.jcapiz.moduletester.terminalsupport.dialogos.ObtenerTexto;
import org.terminalsupport.jcapiz.moduletester.terminalsupport.networking.IOHandler;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

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
        ib.setOnClickListener(new View.OnClickListener() {

            private boolean clicked = false;

            @Override
            public void onClick(View v) {
                if (!clicked){
                    clicked = true;
                    ObtenerTexto ot = new ObtenerTexto();
                    Bundle args = new Bundle();
                    args.putString("mensaje", "Escriba la ip del destino");
                    ot.setArguments(args);
                    ot.setAgenteDeInteraccion(new DialogoDeConsultaSimple.AgenteDeInteraccionConResultado() {
                        @Override
                        public void clickSobreAccionPositiva(DialogFragment dialogo) {
                            final String texto = ((ObtenerTexto)dialogo).obtenerTexto();
                            new Thread(){

                                @Override
                                public void run(){
                                    try{
                                        DataInputStream entrada = new DataInputStream(new FileInputStream(new File(SOURCE+"/calaca.txt")));
                                        byte[] chunk = new byte[64];
                                        int length;
                                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                        while((length = entrada.read(chunk)) != -1)
                                            baos.write(chunk, 0, length);
                                        entrada.close();
                                        Socket socket = new Socket(texto, 23500);
                                        IOHandler ioHandler = new IOHandler(new DataInputStream(socket.getInputStream()), new DataOutputStream(socket.getOutputStream()));
                                        ioHandler.sendMessage(baos.toByteArray());
                                        baos.close();
                                        ioHandler.close();
                                        runOnUiThread(new Runnable(){
                                            @Override
                                            public void run(){
                                                Toast.makeText(MainActivity.this, "Archivo enviado con éxito.", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }catch(final IOException e){
                                        e.printStackTrace();
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(MainActivity.this, "Hubo un error: " + e.getMessage() + ".", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }finally {
                                        clicked = false;
                                    }
                                }
                            }.start();
                        }

                        @Override
                        public void clickSobreAccionNegativa(DialogFragment dialogo) {

                        }
                    });
                    FragmentManager fm = MainActivity.this.getSupportFragmentManager();
                    Log.e("Shu", fm == null ? "Nulo" : "Yop");
                    ot.show(fm, "ObtenerIP");
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
            launchWaiterDialog(); // Cuando se toque la opción del menú lanza un dialogo de espera.
            consumed = true;
        }
        return consumed;
    }

    private void launchWaiterDialog() {
        startActivityForResult(new Intent(this, WaiterActivity.class), WAITER_ACTIVITY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        // This method is called when the activity regains control after a launched activity has
        // finished.
        if(resultCode == RESULT_OK && requestCode == WAITER_ACTIVITY){
            byte[] content = data.getByteArrayExtra("content"); // Retrieves the file's bytes
            // The way we do things here is not recommended, it only serves as an example of sharing
            // between activities.
            File f = new File(SOURCE, "TerminalSupport");
            if(!f.exists()){
                f.mkdirs();
            }
            try {
                DataOutputStream dos = new DataOutputStream(new FileOutputStream(new File(SOURCE + "/TerminalSupport/new_calaca.txt")));
                dos.write(content);
                dos.close();
                Toast.makeText(this, "Éxito al recibir y escribir el archivo", Toast.LENGTH_SHORT).show();
            }catch(IOException e){
                e.printStackTrace();
                Toast.makeText(this, "Problemas al escribir el archivo", Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(this, "Problemas de conexión", Toast.LENGTH_SHORT).show();
        }
    }

    private static final String SOURCE = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static final int WAITER_ACTIVITY = 01;
}
