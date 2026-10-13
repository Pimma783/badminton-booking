package com.badminton.model;

/** ประเภทอุปกรณ์ให้เช่า ตามโจทย์ "เช่ารองเท้า ไม้แบดมินตันได้ราคาคู่ละ 50 บาท/ครั้ง/วัน" */
public enum RentalType {
    SHOES("รองเท้า (คู่)"), RACKET("ไม้แบดมินตัน (คู่)");
    private final String label;
    RentalType(String label) { this.label = label; }
    // ใช้ชื่อภาษาไทยเมื่อแสดงใน ComboBox และตาราง
    @Override public String toString() { return label; }
}
