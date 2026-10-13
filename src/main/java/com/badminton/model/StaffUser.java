package com.badminton.model;

/**
 * ผู้ใช้ระบบ (พนักงาน) — เป็น abstract class ตามข้อกำหนดโจทย์ "ให้ใช้ interface / abstract class"
 * <p>
 * โจทย์กำหนดผู้ใช้ระบบ 2 ประเภท คือ พนักงานแคชเชียร์ ({@link Cashier}) และผู้ดูแลระบบ ({@link Administrator})
 * ทั้งสองมีชื่อผู้ใช้และชื่อที่แสดงเหมือนกัน จึงเก็บไว้ที่คลาสแม่นี้ (Inheritance)
 * ส่วนบทบาทและสิทธิ์ที่ต่างกันให้คลาสลูกเป็นผู้กำหนดเอง (Polymorphism)
 */
public abstract class StaffUser {
    // private + final: ภายนอกอ่านได้ผ่าน getter เท่านั้น แก้ไขไม่ได้ (Encapsulation)
    private final String username;
    private final String displayName;
    // protected: ให้เฉพาะคลาสลูกเรียกได้ เพราะไม่ควรสร้าง StaffUser ลอย ๆ ที่ไม่มีบทบาท
    protected StaffUser(String username, String displayName) {
        this.username = username;
        this.displayName = displayName;
    }
    /** ชื่อผู้ใช้สำหรับเข้าสู่ระบบ ใช้บันทึกว่าใครเป็นผู้ทำรายการ */
    public String getUsername() { return username; }
    /** ชื่อที่แสดงบนแถบหัวของหน้าจอหลัก */
    public String getDisplayName() { return displayName; }
    /** บทบาทของผู้ใช้ — คลาสลูกต้องระบุเอง */
    public abstract Role getRole();
    /**
     * มีสิทธิ์ดูประวัติสมาชิก รายได้ และตั้งวันหยุดหรือไม่
     * โค้ดส่วนอื่นเรียกผ่านชนิด StaffUser โดยไม่ต้องรู้ว่าเป็นคลาสลูกตัวไหน
     */
    public abstract boolean canViewReports();
}
