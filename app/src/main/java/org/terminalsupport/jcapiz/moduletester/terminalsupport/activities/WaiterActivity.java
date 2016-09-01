package org.terminalsupport.jcapiz.moduletester.terminalsupport.activities;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.widget.TextView;
import android.widget.Toast;

import org.terminalsupport.jcapiz.moduletester.R;
import org.terminalsupport.jcapiz.moduletester.terminalsupport.networking.IOHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class WaiterActivity extends Activity {

    private static final String SOURCE = Environment.getExternalStorageDirectory().getAbsolutePath();
    private TextView mensajeEspera;
    private CommandThread ct;
    private WaitingThread wt;
    private ServerSocket server;
    private Socket socket;
    private DataOutputStream salida;
    private File outFile;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.waiter_activity);
        mensajeEspera = (TextView) findViewById(R.id.waiter_activity_mensaje_espera);
        if(savedInstanceState == null) {
            try {
                File f = new File(SOURCE, "TerminalSupport");
                if(!f.exists()){
                    f.mkdirs();
                }
                outFile = new File(SOURCE.concat("/TerminalSupport/Calaca.txt"));
                salida = new DataOutputStream(new FileOutputStream(outFile));
            } catch (IOException ignore) {}
        }
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

    @Override
    protected void onResume(){
        super.onResume();
        ct = new CommandThread();
        wt = new WaitingThread();
        ct.start();
        wt.start();
    }

    @Override
    protected void onStop(){
        super.onStop();
        ct.stopSelf();
    }

    @Override
    public void onBackPressed(){
        // Definir este método como vacío deshablita la "tecla" atrás.
    }

    private class CommandThread extends Thread{

        private ServerSocket server;

        @Override
        public void run(){
            Socket socket;
            IOHandler ioHandler;
            String message;
            try{
                server = new ServerSocket(23501);
                while(true){
                    socket = server.accept();
                    ioHandler = new IOHandler(new DataInputStream(socket.getInputStream()), new DataOutputStream(socket.getOutputStream()));
                    message = new String(ioHandler.handleIncommingMessage());
                    if("Disconnect".equals(message)){
                        wt.stopCommunication();
                    }
                }
            }catch(IOException e){
                e.printStackTrace();
                try{ wt.stopCommunication(); }catch(IOException ignore){}
            }
            runOnUiThread(new Runnable(){ @Override public void run(){ Toast.makeText(WaiterActivity.this, "Finaliza hilo de control", Toast.LENGTH_SHORT).show(); }});
        }

        public void stopSelf(){
            try{
                server.close();
            }catch(IOException ignore){}
        }
    }

    private class WaitingThread extends Thread{

        WaitingThread(){
            try{
                server = new ServerSocket(23500);
            }catch(IOException e){
                e.printStackTrace();
            }
        }

        @Override
        public void run(){
            IOHandler ioHandler = null;
            try{
                socket = server.accept();
                ioHandler = new IOHandler(new DataInputStream(socket.getInputStream()), new DataOutputStream(socket.getOutputStream()));
                ioHandler.setRate(1024);
                while(true) {
                    salida.write(ioHandler.handleIncommingMessage());
                }
            }catch(IOException e){
                e.printStackTrace();
                setResult(RESULT_CANCELED);
            }finally{
                try{
                    server.close();
                    if(ioHandler != null) ioHandler.close();
                    socket.close();
                }catch(IOException ignore){}
                runOnUiThread(new Runnable(){ @Override public void run(){ Toast.makeText(WaiterActivity.this, "Finaliza hilo de escritura", Toast.LENGTH_SHORT).show(); }});
                finish();
            }
        }

        public void stopCommunication() throws IOException{
            if(server != null && !server.isClosed()) server.close(); else if(socket != null && !socket.isClosed()) socket.close();
        }
    }
}
