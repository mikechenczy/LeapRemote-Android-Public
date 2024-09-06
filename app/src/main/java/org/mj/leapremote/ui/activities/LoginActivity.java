package org.mj.leapremote.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.mj.leapremote.Define;
import org.mj.leapremote.R;
import org.mj.leapremote.model.User;
import org.mj.leapremote.service.HttpService;
import org.mj.leapremote.util.DataUtil;
import org.mj.leapremote.util.Utils;

public class LoginActivity extends AppCompatActivity {
    public static LoginActivity INSTANCE;
    private EditText usernameText;
    private EditText passwordText;
    private Button loginButton;
    private Button registerButton;
    public Button forgetButton;
    private CheckBox checkbox;
    private TextView errorMessage;

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        INSTANCE = this;
        LogoActivity.INSTANCE = this;
        //Utils.verifyStoragePermissions(this); //获取储存权限
        ((TextView) findViewById(R.id.versionText)).setText(getString(R.string.versionText) + Utils.getVersion(this));
        usernameText = findViewById(R.id.usernameText);
        passwordText = findViewById(R.id.passwordText);
        checkbox = findViewById(R.id.checkBox);
        errorMessage = findViewById(R.id.errnoMessage);
        loginButton = findViewById(R.id.loginButton);
        if(!Define.autoLogin)
            checkbox.setChecked(false);
        loginButton.setOnClickListener(v -> login());
        registerButton = findViewById(R.id.registerButton);
        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
        forgetButton = findViewById(R.id.forgetButton);
        forgetButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgetPasswordActivity.class);
            startActivity(intent);
        });
        if(!Utils.stringIsEmpty(Define.autoLoginUsername))
            usernameText.setText(Define.autoLoginUsername);
        if(!Utils.stringIsEmpty(Define.autoLoginUsername) && Define.autoLogin)
            passwordText.setText(Define.autoLoginPassword);
    }

    public void login() {
        if(loginButton.isEnabled()) {
            loginButton.setEnabled(false);
            loginButton.setBackgroundResource(R.drawable.shape_login_pressing);
            loginButton.setText(getString(R.string.loggingIn));
            String username = usernameText.getText().toString();
            String password = passwordText.getText().toString();
            if(Utils.check(password)) {
                new Thread(() -> {
                    runOnUiThread(() -> errorMessage.setVisibility(View.INVISIBLE));
                    User user = HttpService.login(username, password);
                    if (user == null)
                        LoginActivity.this.runOnUiThread(() -> showErrorMessage(getResources().getText(R.string.cannot_connect_to_server).toString()));
                    else if (Utils.stringIsEmpty(user.getUsername()))
                        LoginActivity.this.runOnUiThread(() -> showErrorMessage(getString(user.getUserId() == 1?R.string.usernameOrPasswordWrongInFormat:R.string.usernameOrPasswordWrong)));
                    else {
                        Define.user = user;
                        Define.autoLogin = checkbox.isChecked();
                        new Thread(DataUtil::save).start();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    LoginActivity.this.runOnUiThread(() -> {
                        loginButton.setBackgroundResource(R.drawable.shape_login);
                        loginButton.setEnabled(true);
                        loginButton.setText(getString(R.string.login));
                    });
                }).start();
            } else {
                showErrorMessage(getString(R.string.usernameOrPasswordWrongInFormat));
                loginButton.setBackgroundResource(R.drawable.shape_login);
                loginButton.setEnabled(true);
                loginButton.setText(R.string.login);
            }
        }
    }

    public void showErrorMessage(String message){
        errorMessage.setText(message);
        errorMessage.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.addCategory(Intent.CATEGORY_HOME);
            startActivity(i);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
