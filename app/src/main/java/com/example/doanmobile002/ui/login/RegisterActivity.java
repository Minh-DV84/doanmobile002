package com.example.doanmobile002.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.doanmobile002.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;

import android.app.ProgressDialog;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;

    private TextInputLayout    tilName, tilEmail, tilPassword, tilConfirm;
    private TextInputEditText  etName, etEmail, etPassword, etConfirm;
    private MaterialButton     btnRegister;
    private TextView           tvLogin;
    private ProgressDialog     progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();
        bindViews();
        setupListeners();
    }

    private void bindViews() {
        tilName     = findViewById(R.id.tilName);
        tilEmail    = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirm  = findViewById(R.id.tilConfirm);
        etName      = findViewById(R.id.etName);
        etEmail     = findViewById(R.id.etEmail);
        etPassword  = findViewById(R.id.etPassword);
        etConfirm   = findViewById(R.id.etConfirm);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin     = findViewById(R.id.tvLogin);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Đang tạo tài khoản...");
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> register());
        tvLogin.setOnClickListener(v -> finish()); // Quay lại LoginActivity
    }

    private void register() {
        String name    = etName.getText()     != null ? etName.getText().toString().trim()     : "";
        String email   = etEmail.getText()    != null ? etEmail.getText().toString().trim()    : "";
        String pass    = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirm = etConfirm.getText()  != null ? etConfirm.getText().toString().trim()  : "";

        if (!validate(name, email, pass, confirm)) return;

        progressDialog.show();

        firebaseAuth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(result -> {
                    // Cập nhật displayName
                    UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build();
                    result.getUser().updateProfile(req)
                            .addOnCompleteListener(t -> {
                                progressDialog.dismiss();
                                Toast.makeText(this,
                                        "Tạo tài khoản thành công! Vui lòng đăng nhập.",
                                        Toast.LENGTH_LONG).show();
                                finish();
                            });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    String msg = e.getMessage();
                    if (msg != null && msg.contains("email address is already"))
                        tilEmail.setError("Email này đã được sử dụng");
                    else
                        Toast.makeText(this, "Lỗi: " + msg, Toast.LENGTH_SHORT).show();
                });
    }

    private boolean validate(String name, String email, String pass, String confirm) {
        boolean ok = true;
        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirm.setError(null);

        if (TextUtils.isEmpty(name)) {
            tilName.setError("Vui lòng nhập tên"); ok = false;
        }
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Vui lòng nhập email"); ok = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email không hợp lệ"); ok = false;
        }
        if (TextUtils.isEmpty(pass)) {
            tilPassword.setError("Vui lòng nhập mật khẩu"); ok = false;
        } else if (pass.length() < 6) {
            tilPassword.setError("Mật khẩu phải có ít nhất 6 ký tự"); ok = false;
        }
        if (TextUtils.isEmpty(confirm)) {
            tilConfirm.setError("Vui lòng xác nhận mật khẩu"); ok = false;
        } else if (!pass.equals(confirm)) {
            tilConfirm.setError("Mật khẩu không khớp"); ok = false;
        }
        return ok;
    }
}