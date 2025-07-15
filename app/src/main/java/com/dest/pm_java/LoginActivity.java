package com.dest.pm_java;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.dest.pm_java.models.User;
import com.dest.pm_java.utilities.TokenManager;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        TextView emailView = findViewById(R.id.editTextLoginEmail);
        TextView passwordView = findViewById(R.id.editTextLoginPassword);
        Button btnLogin = findViewById(R.id.buttonLogin);

        btnLogin.setOnClickListener(v -> {
            new LoginTask().execute(emailView.getText().toString(), passwordView.getText().toString());
        });



    }

    private class LoginTask extends AsyncTask<String, Void, LoginTask.LoginResult> {
        public class LoginResult {
            public boolean success;
            public String token;
            public User user;
            public String message;

            public LoginResult(boolean success, String token, User user, String message) {
                this.success = success;
                this.token = token;
                this.user = user;
                this.message = message;
            }
        }

        @Override
        protected LoginResult doInBackground(String... strings) {
            String username = strings[0];
            String password = strings[1];

            try {
                URL url = new URL("http://10.0.2.2:3000/api/v1/login");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String jsonBody = "{" +
                        "\"email\": \"" + username + "\"," +
                        "\"password\": \"" + password + "\"" +
                        "}";

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }

                    JSONObject json = new JSONObject(response.toString());

                    if (json.getBoolean("success")) {
                        String token = json.getString("token");
                        JSONObject data = json.getJSONObject("data");

                        User user = new User(
                                data.getString("id"),
                                data.getString("email"),
                                data.getString("username"),
                                data.getString("first_name"),
                                data.getString("last_name"),
                                data.getString("avatar_url")
                        );

                        return new LoginResult(true, token, user, json.getString("message"));
                    } else {
                        return new LoginResult(false, null, null, json.getString("message"));
                    }
                } else {
                    return new LoginResult(false, null, null, "Login failed with code: " + responseCode);
                }

            } catch (Exception e) {
                Log.e("LOGIN_ERROR", e.toString());
                return new LoginResult(false, null, null, "Network error: " + e.getMessage());
            }
        }

        @Override
        protected void onPostExecute(LoginResult result) {
            if (result.success && result.token != null) {
                TokenManager tokenManager = new TokenManager(LoginActivity.this);
                tokenManager.saveToken(result.token);
                tokenManager.saveUserData(
                        result.user.getId(),
                        result.user.getUsername(),
                        result.user.getEmail()
                );

                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(LoginActivity.this,
                        result.message != null ? result.message : "Login Gagal",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

}