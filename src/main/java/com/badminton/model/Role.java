package com.badminton.model;

/** บทบาทของผู้ใช้ระบบ แต่ละค่าเก็บชื่อภาษาไทยไว้แสดงบนหน้าจอ */
public enum Role {
    CASHIER("พนักงานแคชเชียร์"), ADMIN("ผู้ดูแลระบบ");
    private final String label;
    Role(String label) { this.label = label; }
    // ใช้ชื่อภาษาไทยเมื่อแสดงใน Label หรือ ComboBox
    @Override public String toString() { return label; }
}
