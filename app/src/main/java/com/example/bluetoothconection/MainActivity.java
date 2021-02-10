package com.example.bluetoothconection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {

    public static MyService myService;

    int k;

    Handler mHandler ;
    ImageView b_upload;
    ImageView b_delete;
    ImageView b_play;
    ImageView b_bluetooth;
    ImageView b_disconect;
    ImageView b_stop;

    private static final int PERMISSION_REQUEST_STORAGE = 1000;
    private static final int READ_REQUEST_CODE = 42;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        b_upload = (ImageView) findViewById(R.id.addmusic);

        b_delete = (ImageView) findViewById(R.id.delete);
        b_play = (ImageView) findViewById(R.id.play);
        b_stop = (ImageView) findViewById(R.id.stop);
        b_bluetooth = (ImageView) findViewById(R.id.bluetooth);
        b_disconect = (ImageView) findViewById(R.id.exit);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_STORAGE);
        }


        b_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                performFileSearch();


            }
        });

        b_bluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService(v);

            }
        });


        b_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myService.mConnectedThread != null) {
                    myService.mConnectedThread.write("s");
                } else {
                    Toast.makeText(getApplicationContext(), "Arduino si Android nu sunt imprerecheate, conectati-va la bluetooth", Toast.LENGTH_SHORT).show();
                }


            }
        });

        b_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myService.mConnectedThread != null) {
                    myService.mConnectedThread.write("v");
                } else {
                    Toast.makeText(getApplicationContext(), "Arduino si Android nu sunt imprerecheate, conectati-va la bluetooth", Toast.LENGTH_SHORT).show();
                }
            }
        });

        b_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myService.mConnectedThread != null) {
                    myService.mConnectedThread.write("x");
                } else {
                    Toast.makeText(getApplicationContext(), "Arduino si Android nu sunt imprerecheate, conectati-va la bluetooth", Toast.LENGTH_SHORT).show();
                }

            }
        });

        b_disconect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(v);
                finish();
                System.exit(0);
            }
        });






    }


    public void startService(View view) {
        startService(new Intent(getBaseContext(), MyService.class));



    }

    // Method to stop the service
    public void stopService(View view) {
        stopService(new Intent(getBaseContext(), MyService.class));
    }


    private void readText(String input) {

        File file = new File(Environment.getExternalStorageDirectory(), input);
        StringBuilder text = new StringBuilder();
        String o = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            int line;

            while ((line = br.read()) != -1) {

                if (line == 44 ) {
                    o = "";
                    o = text + o;
                    o = o + ',';
                    myService.mConnectedThread.write(o);
                    text = new StringBuilder();

                }
                else if(line == 62)
                {
                    myService.mConnectedThread.write(">");
                }
                else {
                    text.append((char) line);
                }


            }

            br.close();
            Toast.makeText(getApplicationContext(), "Datele sunt in curs de trimitere", Toast.LENGTH_SHORT).show();



        } catch (IOException e) {
            e.printStackTrace();
        }



            final Handler handler = new Handler();

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        if (myService.Data.equals("k")) {
                            showAlertDialog();
                            myService.Data = "";
                        }
                    }


                }, 3000);



    }


    private void performFileSearch() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((myService.mConnectedThread != null) && (myService.btAdapter != null)) {


        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                String path = uri.getPath();
                path = path.substring(path.indexOf(":") + 1);
                if (path.contains("emulated")) {
                    path = path.substring(path.indexOf("0") + 1);
                }

                readText(path);
                Toast.makeText(getApplicationContext(), "Trimitere finalizata", Toast.LENGTH_SHORT).show();

            }
        }
        } else {
            Toast.makeText(getApplicationContext(), "Arduino si Android nu sunt imprerecheate, conectati-va la bluetooth", Toast.LENGTH_SHORT).show();

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(this, "Permission not granted ", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    public void showAlertDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setCancelable(true);

        builder.setTitle("Alertă!");
        builder.setMessage("Avertisment, fișierul este prea lung, scurtați-l. \n"+"Vă rugăm să așteptați până când această alertă dispare și datele existente sunt șterse.");

        // Setting Negative "Cancel" Button

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });
        final AlertDialog alert = builder.create();
        alert.show();

        final Handler handler  = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (alert.isShowing()) {
                    alert.dismiss();
                }
                if(myService.sendAgain != null)
                {
                    if (myService.sendAgain.equals("n")) {
                        sendAlertDialog();
                        myService.sendAgain = "";
                    }


                }

            }
        };

        alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                handler.removeCallbacks(runnable);
            }
        });

        handler.postDelayed(runnable, 10000);

    }
    public void sendAlertDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setCancelable(true);

        builder.setTitle("Asteptați!");
        builder.setMessage("Veți putea trimite un nou fișier imediat, așteptați puțin.");

        // Setting Negative "Cancel" Button

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });
        final AlertDialog alert = builder.create();
        alert.show();

        final Handler handler  = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (alert.isShowing()) {
                    alert.dismiss();
                }
            }
        };

        alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                handler.removeCallbacks(runnable);
            }
        });

        handler.postDelayed(runnable, 6000);

    }



}






