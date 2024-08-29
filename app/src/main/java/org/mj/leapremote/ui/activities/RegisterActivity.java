package org.mj.leapremote.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.mj.leapremote.Define;
import org.mj.leapremote.R;
import org.mj.leapremote.model.User;
import org.mj.leapremote.service.HttpService;
import org.mj.leapremote.util.DataUtil;
import org.mj.leapremote.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private EditText reg_username;
    private EditText reg_password;
    private EditText reg_password2;
    private EditText reg_mail;
    private EditText reg_code;
    private Button reg_btn_sure;
    private Button reg_btn_login;
    private Button reg_btn_code;
    private TextView errorMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ((Toolbar)findViewById(R.id.id_toolbar)).setNavigationOnClickListener(v -> finish());
        reg_username = findViewById(R.id.reg_username);
        reg_password = findViewById(R.id.reg_password);
        reg_password2 = findViewById(R.id.reg_password2);
        reg_mail = findViewById(R.id.reg_mail);
        reg_code = findViewById(R.id.reg_code);
        reg_btn_sure = findViewById(R.id.reg_btn_sure);
        reg_btn_login = findViewById(R.id.reg_btn_login);
        reg_btn_code = findViewById(R.id.reg_btn_code);
        errorMessage = findViewById(R.id.reg_errnoMessage);
        reg_btn_sure.setOnClickListener(v -> register());
        reg_btn_code.setOnClickListener(v -> {
            if(reg_btn_code.isEnabled()) {
                reg_btn_code.setEnabled(false);
                String email = reg_mail.getText().toString().trim();
                if(Utils.isEmail(email)) {
                    errorMessage.setVisibility(View.INVISIBLE);
                    Map<String, String> map = new HashMap<>();
                    map.put("email", email);
                    map.put("getCode", "1");
                    new Thread(() -> {
                        User user = HttpService.register(map);
                        if (user == null) {
                            showErrorMessage(getResources().getText(R.string.cannot_connect_to_server).toString());
                            runOnUiThread(() -> reg_btn_code.setEnabled(true));
                        } else if (Utils.string(user.getUsername()).isEmpty()) {
                            boolean success = false;
                            switch (user.getUserId()) {
                                case 1:
                                    showErrorMessage(getString(R.string.incorrectDataFormat));
                                    break;
                                case 2:
                                    showErrorMessage(getString(R.string.alreadyRegisteredUsername));
                                    break;
                                case 3:
                                    showErrorMessage(getString(R.string.alreadyRegisteredEmail));
                                    break;
                                case 4:
                                    success = true;
                                    new Thread(() -> {
                                        for (int i = 60; i > 0; i--) {
                                            int finalI = i;
                                            runOnUiThread(() -> reg_btn_code.setText(getString(R.string.waitCode1) + finalI + getString(R.string.waitCode2)));
                                            try {
                                                Thread.sleep(1000);
                                            } catch (InterruptedException interruptedException) {
                                                interruptedException.printStackTrace();
                                            }
                                        }
                                        runOnUiThread(() -> {
                                            reg_btn_code.setText(getString(R.string.getVerificationCode));
                                            reg_btn_code.setEnabled(true);
                                        });
                                    }).start();
                                    break;
                                case 5:
                                    showErrorMessage(getString(R.string.verificationCodeWrong));
                                    break;
                                default:
                                    showErrorMessage(getString(R.string.unableToParseReturnData));
                            }
                            if(!success)
                                runOnUiThread(() -> reg_btn_code.setEnabled(true));
                        } else {
                            Define.user = user;
                            DataUtil.save();
                            runOnUiThread(() -> {
                                LoginActivity.INSTANCE.finish();
                                Intent intent = new Intent(this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            });
                        }
                    }).start();
                } else {
                    showErrorMessage(getString(R.string.emailWrongInFormat));
                    reg_btn_code.setEnabled(true);
                }
            }
        });
        reg_btn_login.setOnClickListener(v -> finish());
    }

    private void register() {
        if(reg_btn_sure.isEnabled()) {
            reg_btn_sure.setEnabled(false);
            String username = reg_username.getText().toString().trim();
            String password = reg_password.getText().toString().trim();
            String passwordAgain = reg_password2.getText().toString().trim();
            String email = reg_mail.getText().toString().trim();
            String code = reg_code.getText().toString().trim();
            if(Utils.check(username,password)) {
                if(password.equals(passwordAgain)) {
                    if(Utils.isEmail(email)) {
                        if(code.length()==4) {
                            errorMessage.setVisibility(View.INVISIBLE);
                            Map<String, String> map = new HashMap<>();
                            map.put("username", username);
                            map.put("password", password);
                            map.put("email", email);
                            map.put("code", code);
                            new Thread(() -> {
                                User user = HttpService.register(map);
                                if (user == null) {
                                    showErrorMessage(getResources().getText(R.string.cannot_connect_to_server).toString());
                                } else if (Utils.string(user.getUsername()).isEmpty()) {
                                    switch (user.getUserId()) {
                                        case 0:
                                            break;
                                        case 1:
                                            showErrorMessage(getString(R.string.incorrectDataFormat));
                                            break;
                                        case 2:
                                            showErrorMessage(getString(R.string.alreadyRegisteredUsername));
                                            break;
                                        case 3:
                                            showErrorMessage(getString(R.string.alreadyRegisteredEmail));
                                            break;
                                        case 4:
                                            showErrorMessage(getString(R.string.needVerificationCode));
                                            break;
                                        case 5:
                                            showErrorMessage(getString(R.string.verificationCodeWrong));
                                            break;
                                        default:
                                            showErrorMessage(getString(R.string.unableToParseReturnData));
                                    }
                                } else {
                                    Define.user = user;
                                    DataUtil.save();
                                    runOnUiThread(() -> {
                                        LoginActivity.INSTANCE.finish();
                                        Intent intent = new Intent(this, MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    });
                                }
                            }).start();
                        }else
                            showErrorMessage(getString(R.string.verificationCodeWrongInFormat));
                    }else
                        showErrorMessage(getString(R.string.emailWrongInFormat));
                }else
                    showErrorMessage(getString(R.string.passwordDifferent));
            }else
                showErrorMessage(getString(R.string.usernameOrPasswordWrongInFormat));
            reg_btn_sure.setEnabled(true);
        }
    }

    private void showErrorMessage(String message) {
        runOnUiThread(() -> {
            errorMessage.setText(message);
            errorMessage.setVisibility(View.VISIBLE);
        });
    }
}
