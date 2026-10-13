package com.badminton.model;

/** วิธีชำระค่าเช่าสนาม: จ่ายเงินรายชั่วโมง หรือหักจากชั่วโมงแพ็กเกจที่ซื้อไว้ */
public enum PaymentMode {
    CASH("ชำระเงินรายชั่วโมง"), PACKAGE("ใช้แพ็กเกจคงเหลือ");
    private final String label;
    PaymentMode(String label) { this.label = label; }
    // ใช้ชื่อภาษาไทยเมื่อแสดงใน ComboBox
    @Override public String toString() { return label; }
}
