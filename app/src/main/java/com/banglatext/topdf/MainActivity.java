package com.banglatext.topdf;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    EditText editText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.text_length_calculate);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        editText = findViewById(R.id.editText);


        findViewById(R.id.btnOpenBanglaPdf).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BanglaPdfActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.buttonID).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String myText = editText.getText().toString();
                int textLength = myText.length();
                Log.d("MainActivity", "TextLength: " + textLength);
                Toast.makeText(MainActivity.this, String.valueOf(textLength), Toast.LENGTH_SHORT).show();
                if (textLength == 4) {
                    Toast.makeText(MainActivity.this, "Correct", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Incorrect", Toast.LENGTH_SHORT).show();
                }



            }
        });






    }
}