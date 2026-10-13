package com.badminton.model;

import java.io.Serializable;

/**
 * สินค้าที่ขายหน้าร้าน (ไม้แบด ลูกขนไก่ เทปพันด้าม) — โจทย์ให้นักศึกษากำหนดราคาเอง
 *
 * @param id        รหัสสินค้า เช่น P001
 * @param name      ชื่อสินค้า
 * @param priceBaht ราคาต่อชิ้น (บาท)
 * @param stock     จำนวนคงเหลือในสต็อก
 */
public record Product(String id, String name, long priceBaht, int stock) implements Serializable {
    /** คืนสินค้าตัวเดิมที่ตัดสต็อกตามจำนวนที่ขายแล้ว */
    public Product afterSale(int quantity) { return new Product(id, name, priceBaht, stock - quantity); }
    // ข้อความที่แสดงใน ComboBox เลือกสินค้า
    @Override public String toString() { return name + " • " + priceBaht + " บาท (เหลือ " + stock + ")"; }
}
