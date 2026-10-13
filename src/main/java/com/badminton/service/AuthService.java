package com.badminton.service;

import com.badminton.model.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * ตรวจการเข้าสู่ระบบด้วยบัญชีสาธิต 2 บัญชี (admin / cashier)
 * <p>
 * ใช้สำหรับงานเรียนเท่านั้น ถ้านำไปใช้จริงควรเก็บผู้ใช้และรหัสผ่านที่ hash แล้วไว้ในฐานข้อมูล
 */
public final class AuthService {
    /**
     * คืนผู้ใช้ตามบทบาท (Administrator หรือ Cashier) ถ้ารหัสผ่านถูกต้อง
     * ผู้เรียกได้ชนิด StaffUser กลับไปและไม่ต้องรู้ว่าเป็นคลาสลูกตัวไหน (Polymorphism)
     */
    public StaffUser login(String username, String password) {
        String user = username == null ? "" : username.trim();
        if (user.equals("admin") && matches(password, "admin123")) return new Administrator(user, "ผู้ดูแลระบบ");
        if (user.equals("cashier") && matches(password, "cashier123")) return new Cashier(user, "แคชเชียร์");
        throw new IllegalArgumentException("ชื่อผู้ใช้หรือรหัสผ่านไม่ถูกต้อง");
    }
    /**
     * เปรียบเทียบรหัสผ่านผ่านค่า hash ด้วย MessageDigest.isEqual ซึ่งใช้เวลาเท่ากันทุกครั้ง
     * ไม่ว่าตัวอักษรจะผิดที่ตำแหน่งไหน จึงเดารหัสจากเวลาที่ใช้ตรวจไม่ได้
     */
    private boolean matches(String actual, String expected) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return actual != null && MessageDigest.isEqual(digest.digest(actual.getBytes(StandardCharsets.UTF_8)),
                    digest.digest(expected.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
