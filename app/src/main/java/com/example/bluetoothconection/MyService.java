package com.example.bluetoothconection;

import android.app.AlertDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;


public class MyService extends Service {
    final int handlerState = 0;                        //used to identify handler message
    Handler bluetoothIn;
    public static BluetoothAdapter btAdapter = null;
    public static BluetoothDevice mDevice;

    public static ConnectingThread mConnectingThread;
    public static ConnectedThread mConnectedThread;

    public static String Data ;
    public static String sendAgain ;
    public static MainActivity mainActivity;

    public boolean stopThread = false;
    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // String for MAC address

    private StringBuilder recDataString = new StringBuilder();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("BT SERVICE", "SERVICIUL S-A CREAT");
        stopThread = false;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("BT SERVICE", "SERVICIUL S-A CREAT");
        bluetoothIn = new Handler() {

            public void handleMessage(android.os.Message msg) {
                Log.d("DEBUG", "handleMessage");
                if (msg.what == handlerState) {                                     //if message is what we want
                    String readMessage = (String) msg.obj;                                                                // msg.arg1 = bytes from connect thread

                    recDataString.append(readMessage);

                    Log.d("RECORDED", recDataString.toString());
                    // Do stuff here with your data, like adding it to the database
                }
                recDataString.delete(0, recDataString.length());                    //clear all string data
            }
        };


        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter

        checkBTState();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        bluetoothIn.removeCallbacksAndMessages(null);
        stopThread = true;
        if (mConnectedThread != null) {
            mConnectedThread.closeStreams();
        }
        if (mConnectingThread != null) {
            mConnectingThread.closeSocket();
        }
        Log.d("SERVICE", "onDestroy");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {

        if (btAdapter == null) {
            Log.d("BT SERVICE", "BLUETOOTH NU ESTE SUPORTAT DE DEVICE, SERVICE OPRIT");
            stopSelf();
        } else {
            if (btAdapter.isEnabled()) {
                Log.d("DEBUG BT", "BT ACTIV! ADRESA BT  : " + btAdapter.getAddress() + " , BT NAME : " + btAdapter.getName());
                try {
                    Set<BluetoothDevice> device = btAdapter.getBondedDevices();
                    if (device.size() > 0) {

                        for (BluetoothDevice Mdevice : device) {
                            mDevice = Mdevice;
                        }
                    }

                    mConnectingThread = new ConnectingThread(mDevice);
                    mConnectingThread.start();
                } catch (IllegalArgumentException e) {
                    Log.d("DEBUG BT", "PROBLEME CU ADRESA MAC : " + e.toString());
                    Log.d("BT SERVICE", "ADRESA MAC GRESITA, OPRIRE SERVICIU");
                    stopSelf();
                }
            } else {
                Log.d("BT SERVICE", "BLUETOOTH-UL NU ESTE ACTIV, SERVICIU OPRIT");
                stopSelf();
            }
        }
    }

    // New Class for Connecting Thread
    private class ConnectingThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectingThread(BluetoothDevice device) {
            Log.d("DEBUG BT", "IN CURS DE CONECTARE");
            mmDevice = device;
            BluetoothSocket temp = null;

            Log.d("DEBUG BT", "BT UUID : " + BTMODULEUUID);
            try {
                temp = mmDevice.createRfcommSocketToServiceRecord(BTMODULEUUID);
                Log.d("DEBUG BT", "SOCKET CREAT : " + temp.toString());
            } catch (IOException e) {
                Log.d("DEBUG BT", "SOCKET-UL NU S-A CREAT :" + e.toString());
                Log.d("BT SERVICE", "CREAREA SOCKET-ULUI A ESUAT, SERVICE OPRIT");
                stopSelf();
            }
            mmSocket = temp;
        }

        @Override
        public void run() {
            super.run();


           // Stabiliți conexiunea prin socket Bluetooth.
           // Anularea descoperirii deoarece poate încetini conexiunea
            btAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
                Log.d("DEBUG BT", "BT SOCKET ESTE CONECTAT");
                mConnectedThread = new ConnectedThread(mmSocket);
                mConnectedThread.start();
                //I send a character when resuming.beginning transmission to check device is connected
                //If it is not an exception will be thrown in the write method and finish() will be called

            } catch (IOException e) {
                try {
                    Log.d("DEBUG BT", "CONECTAREA SOCKET-ULUI A ESUAT: " + e.toString());
                    Log.d("BT SERVICE", "CONECTAREA SOCKET-ULUI A ESUAT, OPRIRE SERVICE");
                    mmSocket.close();
                    stopSelf();
                } catch (IOException e2) {
                    Log.d("DEBUG BT", "ÎNCHIDEREA SOCKET-ULUI A ESUAT  :" + e2.toString());
                    Log.d("BT SERVICE", "NCHIDEREA SOCKET-ULUI A ESUAT, SERVICE OPRIT");
                    stopSelf();

                }
            } catch (IllegalStateException e) {
                Log.d("DEBUG BT", " CONEXIUNEA THREAD-ULUI A ESUAT : " + e.toString());
                Log.d("BT SERVICE", "CONEXIUNEA THREAD-ULUI A ESUAT, OPRIRE SERVICE");
                stopSelf();
            }
        }

        public void closeSocket() {
            try {

                mmSocket.close(); // ÎNCHIDERE SOCKET BLUETOOTH
            } catch (IOException e2) {
                Log.d("DEBUG BT", e2.toString());
                Log.d("BT SERVICE", " ÎNCHIDERE SOCKET-ULUI BLUETOOTH A ESUAT, OPRIRE SERVICE");
                stopSelf();
            }
        }
    }


    public class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;


        public ConnectedThread(BluetoothSocket socket) {

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {

                tmpIn = socket.getInputStream();//Creare flux de I pentru conectare
                tmpOut = socket.getOutputStream();//Creare flux de O pentru conectare
            } catch (IOException e) {
                Log.d("DEBUG BT", e.toString());
                Log.d("BT SERVICE", "FLUX-UL READ/WRITE NE CREAT , OPRIRE SERVICE");

                stopSelf();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // ASCULTAREA MESAJELOR
            while (true && !stopThread) {
                try {
                    bytes = mmInStream.read(buffer);            //CITIREA BITILOR DIN FULX
                    String readMessage = new String(buffer, 0, bytes);
                    Data = readMessage;
                    if(readMessage.equals("k")) {

                        mConnectedThread.write("x");

                    }
                    else if(readMessage.equals("n"))
                    {
                        sendAgain = readMessage;
                    }





                    //Trimitere octeți obținuți la UI Activity prin intermediul handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();

                } catch (IOException e) {
                    Log.d("DEBUG BT", e.toString());
                    Log.d("BT SERVICE", "FLUXUL READ/WRITE NU SE POATE REALIZA , OPRIRE SERVICE");
                    stopSelf();
                    break;
                }


            }

        }


        public void write(String input) {
            byte[] msgBuffer = input.getBytes();    //CONVERSIA STRINGURILOR IN BYTE

                try {

                    mmOutStream.write(msgBuffer); //scrieți octeți prin conexiune BT prin outstream


                } catch (IOException e) {

                    Toast.makeText(getApplicationContext(), "Nu s-a putut realiza aceasta actiune.Activati bluetooth-ul, imperechiati-l si apoi incercati din nou ", Toast.LENGTH_SHORT).show();
                    stopSelf();
                }


        }

        public void closeStreams() {
            try {
                //ÎNCHIDEREA FLUXULUI DE INTRARE IESIRE
                mmInStream.close();
                mmOutStream.close();
            } catch (IOException e2) {

                Log.d("DEBUG BT", e2.toString());
                Log.d("BT SERVICE", "ÎNCHIDEREA FLUXULUI A ESUAT, OPRIRE SERVICE");
                stopSelf();
            }
        }
    }




}

