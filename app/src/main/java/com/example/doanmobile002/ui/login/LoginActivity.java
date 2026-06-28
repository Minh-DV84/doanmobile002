package com.example.doanmobile002.ui.login;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.doanmobile002.MainActivity;
import com.example.doanmobile002.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    // ── Firebase ──────────────────────────────────────────────────────────────
    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleClient;

    // ── Views ─────────────────────────────────────────────────────────────────
    private TextInputLayout   tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton    btnLogin, btnGoogle;
    private TextView          tvRegister, tvForgotPassword;
    private ProgressDialog    progressDialog;

    // ── Google Sign-In launcher ───────────────────────────────────────────────
    private final ActivityResultLauncher<Intent> googleLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        Task<GoogleSignInAccount> task =
                                GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            firebaseAuthWithGoogle(account.getIdToken());
                        } catch (ApiException e) {
                            hideLoading();
                            showToast("Đăng nhập Google thất bại: " + e.getMessage());
                        }
                    }
            );

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();

        // Nếu đã đăng nhập → vào thẳng MainActivity
        if (firebaseAuth.getCurrentUser() != null) {
            goToMain();
            return;
        }

        setupGoogle();
        bindViews();
        setupListeners();
    }

    // ── Setup Google ──────────────────────────────────────────────────────────
    private void setupGoogle() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleClient = GoogleSignIn.getClient(this, gso);
    }

    // ── Bind views ────────────────────────────────────────────────────────────
    private void bindViews() {
        tilEmail         = findViewById(R.id.tilEmail);
        tilPassword      = findViewById(R.id.tilPassword);
        etEmail          = findViewById(R.id.etEmail);
        etPassword       = findViewById(R.id.etPassword);
        btnLogin         = findViewById(R.id.btnLogin);
        btnGoogle        = findViewById(R.id.btnGoogle);
        tvRegister       = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
    }

    // ── Listeners ─────────────────────────────────────────────────────────────
    private void setupListeners() {
        // Email + Password login
        btnLogin.setOnClickListener(v -> loginWithEmail());

        // Google
        btnGoogle.setOnClickListener(v -> {
            showLoading("Đang đăng nhập Google...");
            googleClient.signOut().addOnCompleteListener(task ->
                    googleLauncher.launch(googleClient.getSignInIntent()));
        });

        // Chuyển sang màn đăng ký
        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        // Quên mật khẩu
        tvForgotPassword.setOnClickListener(v -> showForgotPassword());
    }

    // ── Email / Password Login ────────────────────────────────────────────────
    private void loginWithEmail() {
        String email = etEmail.getText() != null ?
                etEmail.getText().toString().trim() : "";
        String pass  = etPassword.getText() != null ?
                etPassword.getText().toString().trim() : "";

        if (!validateInput(email, pass)) return;

        showLoading("Đang đăng nhập...");
        firebaseAuth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(result -> {
                    hideLoading();
                    goToMain();
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    String msg = parseFirebaseError(e.getMessage());
                    tilPassword.setError(msg);
                });
    }

    private boolean validateInput(String email, String pass) {
        boolean ok = true;
        tilEmail.setError(null);
        tilPassword.setError(null);

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Vui lòng nhập email");
            ok = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email không hợp lệ");
            ok = false;
        }
        if (TextUtils.isEmpty(pass)) {
            tilPassword.setError("Vui lòng nhập mật khẩu");
            ok = false;
        } else if (pass.length() < 6) {
            tilPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            ok = false;
        }
        return ok;
    }

    // ── Firebase Auth with Google ─────────────────────────────────────────────
    private void firebaseAuthWithGoogle(String idToken) {
        showLoading("Đang xác thực...");
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    hideLoading();
                    goToMain();
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    showToast("Xác thực Google thất bại: " + e.getMessage());
                });
    }

    // ── Forgot Password ───────────────────────────────────────────────────────
    private void showForgotPassword() {
        String email = etEmail.getText() != null ?
                etEmail.getText().toString().trim() : "";
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Nhập email để lấy lại mật khẩu");
            return;
        }
        showLoading("Đang gửi email...");
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(v -> {
                    hideLoading();
                    showToast("Đã gửi link đặt lại mật khẩu đến " + email);
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    showToast("Không tìm thấy tài khoản với email này");
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void goToMain() {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void showLoading(String msg) {
        progressDialog.setMessage(msg);
        if (!progressDialog.isShowing()) progressDialog.show();
    }

    private void hideLoading() {
        if (progressDialog.isShowing()) progressDialog.dismiss();
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private String parseFirebaseError(String msg) {
        if (msg == null) return "Đã xảy ra lỗi";
        if (msg.contains("password is invalid") ||
                msg.contains("no user record"))
            return "Email hoặc mật khẩu không đúng";
        if (msg.contains("email address is already"))
            return "Email này đã được sử dụng";
        if (msg.contains("network"))
            return "Không có kết nối mạng";
        return "Đăng nhập thất bại. Thử lại sau.";
    }
}