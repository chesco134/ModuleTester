package org.terminalsupport.jcapiz.moduletester.terminalsupport.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import org.terminalsupport.jcapiz.moduletester.R;
import org.terminalsupport.jcapiz.moduletester.terminalsupport.networking.IOHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by jcapiz on 1/09/16.
 */
public class WaiterActivityBT extends Activity {

    private DataOutputStream salida;
    private File outFile;
    private ServerManager serverManager;
    private static final String SOURCE = Environment.getExternalStorageDirectory().getAbsolutePath();

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.waiter_activity);
        if(savedInstanceState == null)
            try {
                File f = new File(SOURCE, "TerminalSupport");
                if(!f.exists()){
                    f.mkdirs();
                }
                outFile = new File(SOURCE.concat("/TerminalSupport/Calaca.txt"));
                salida = new DataOutputStream(new FileOutputStream(outFile));
            } catch (IOException ignore) {}
    }

    @Override
    protected void onResume(){
        super.onResume();
        serverManager = new ServerManager();
        serverManager.start();
    }

    @Override
    protected void onStop(){
        super.onStop();
        serverManager.interruptActions();
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        outState.putSerializable("out_file", outFile);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState){
        outFile = (File) savedInstanceState.getSerializable("out_file");
        assert outFile != null;
        try{ salida = new DataOutputStream(new FileOutputStream(outFile, true)); }catch(IOException ignore){}
    }

    private class ServerManager extends Thread{

        public static final String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";
        private static final String MY_NAME = "Balalaika";
        private final BluetoothServerSocket serverSocket;
        private final BluetoothAdapter mBluetoothAdapter;
        private BluetoothSocket socket;
        private IOHandler ioHandler;
        private boolean success = false;

        public ServerManager(){
            BluetoothServerSocket zukam = null;
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            try{
                zukam = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(MY_NAME, UUID.fromString(MY_UUID));
            } catch (IOException e){
                e.printStackTrace();
            }
            serverSocket = zukam;
        }

        @Override
        public void run(){
            socket = null;
            ioHandler = null;
            try{
                runOnUiThread(new Runnable(){ @Override public void run(){ Toast.makeText(WaiterActivityBT.this, "Preparados", Toast.LENGTH_SHORT).show(); }});
                socket = serverSocket.accept();
                serverSocket.close();
                ioHandler = new IOHandler(new DataInputStream(socket.getInputStream()), new DataOutputStream(socket.getOutputStream()));
                ioHandler.setRate(512);
                while(true) {
                    salida.write(ioHandler.handleIncommingMessage());
                }
            }catch(IOException e){
                e.printStackTrace();
                if(e.getMessage().contains("connection abort"))
                    success = true;
            }finally{
                try{
                    salida.close();
                    ioHandler.close();
                }catch(NullPointerException | IOException ignore){ ignore.printStackTrace(); }
                runOnUiThread(new Runnable(){ @Override public void run(){ Toast.makeText(WaiterActivityBT.this, "Finaliza hilo de escritura", Toast.LENGTH_SHORT).show(); }});
                finish();
            }
        }

        public void interruptActions(){
            try{
                ioHandler.close();
                salida.close();
                if(!success) outFile.delete();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }
}
