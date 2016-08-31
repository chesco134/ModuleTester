package org.terminalsupport.jcapiz.moduletester.terminalsupport.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import org.terminalsupport.jcapiz.terminalsupport.R;
import org.terminalsupport.jcapiz.terminalsupport.networking.IOHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class WaiterActivity extends Activity {

    private TextView mensajeEspera;
    private WaitingThread wt;
    private ServerSocket server;
    private Socket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.waiter_activity);
        mensajeEspera = (TextView) findViewById(R.id.waiter_activity_mensaje_espera);
        wt = new WaitingThread();
    }

    @Override
    protected void onResume(){
        super.onResume();
        wt.start();
    }

    @Override
    protected void onStop(){
        super.onStop();
        try{
            wt.stopCommunication();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed(){
        // Definir este método como vacío deshablita la "tecla" atrás.
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
            try{
                socket = server.accept();
                server.close();
                IOHandler ioHandler = new IOHandler(new DataInputStream(socket.getInputStream()), new DataOutputStream(socket.getOutputStream()));
                byte[] content = ioHandler.handleIncommingMessage();
                ioHandler.close();
                socket.close();
                Intent data = new Intent();
                data.putExtra("content", content);
                setResult(RESULT_OK);
            }catch(IOException e){
                e.printStackTrace();
                setResult(RESULT_CANCELED);
            }finally{
                finish();
            }
        }

        public void stopCommunication() throws IOException{
            if(server != null && !server.isClosed()) server.close(); else if(socket != null && !socket.isClosed()) socket.close();
        }
    }
}
