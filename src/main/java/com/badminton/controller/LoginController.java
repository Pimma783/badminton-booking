package com.badminton.controller;

import com.badminton.BadmintonApp;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/** Controller ของหน้าเข้าสู่ระบบ (login.fxml) */
public final class LoginController {
    // ชื่อตัวแปร @FXML ต้องตรงกับ fx:id ในไฟล์ login.fxml
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    /** กดปุ่มเข้าสู่ระบบ: ถ้าถูกต้องเปิดหน้าหลัก ถ้าผิดแสดงข้อความใต้ฟอร์ม */
    @FXML private void login() {
        try {
            BadmintonApp.service().signIn(usernameField.getText(), passwordField.getText());
            BadmintonApp.showMain();
        } catch (Exception ex) { errorLabel.setText(ex.getMessage()); }
    }
}
