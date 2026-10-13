package com.badminton.model;

/** ประเภทรายได้ ใช้แยกยอดในหน้ารายงานรายได้ของผู้ดูแลระบบ */
public enum RevenueType {
    COURT("ค่าเช่าสนาม"), PACKAGE("ขายแพ็กเกจ"), RENTAL("เช่าอุปกรณ์"), PRODUCT("ขายสินค้า");
    private final String label;
    RevenueType(String label) { this.label = label; }
    // ใช้ชื่อภาษาไทยเมื่อแสดงในตารางและไฟล์ CSV
    @Override public String toString() { return label; }
}
