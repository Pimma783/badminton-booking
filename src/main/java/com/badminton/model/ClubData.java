package com.badminton.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;

/**
 * ข้อมูลทั้งหมดของสโมสรที่บันทึกลงไฟล์ในครั้งเดียว (สมาชิก การจอง การรับเงิน การเช่า สินค้า วันหยุด)
 * <p>
 * ClubService จะแก้ไขบน "สำเนา" ของอ็อบเจกต์นี้ และนำสำเนามาใช้จริงหลังบันทึกไฟล์สำเร็จแล้วเท่านั้น
 * ถ้าบันทึกไม่สำเร็จ ข้อมูลเดิมในหน่วยความจำจึงไม่เสียหาย
 */
public final class ClubData implements Serializable {
    // เลขเวอร์ชันของรูปแบบข้อมูล ใช้ตรวจว่าไฟล์ .bin ตรงกับโปรแกรมเวอร์ชันนี้
    @Serial private static final long serialVersionUID = 1L;
    public final List<Member> members = new ArrayList<>();
    public final List<Booking> bookings = new ArrayList<>();
    public final List<Payment> payments = new ArrayList<>();
    public final List<Rental> rentals = new ArrayList<>();
    public final List<Product> products = new ArrayList<>();
    // TreeSet เรียงวันที่ให้อัตโนมัติ
    public final Set<LocalDate> holidays = new TreeSet<>();
    public ClubData() {}
    /**
     * สร้างสำเนาสำหรับแก้ไข — คัดลอกเฉพาะรายการ (shallow copy) ก็พอ
     * เพราะสมาชิก การจอง ฯลฯ เป็น record ที่แก้ค่าไม่ได้อยู่แล้ว
     */
    public ClubData(ClubData source) {
        members.addAll(source.members); bookings.addAll(source.bookings);
        payments.addAll(source.payments); rentals.addAll(source.rentals);
        products.addAll(source.products); holidays.addAll(source.holidays);
    }
    /** ข้อมูลเริ่มต้นเมื่อเปิดโปรแกรมครั้งแรก: ยังไม่มีสมาชิก มีเฉพาะสินค้าพื้นฐาน 3 รายการ */
    public static ClubData emptyClub() {
        ClubData data = new ClubData();
        // ราคาสินค้ากำหนดเองตามโจทย์ (รหัส, ชื่อ, ราคา, สต็อกเริ่มต้น)
        data.products.add(new Product("P001", "ไม้แบดมินตัน", 790, 30));
        data.products.add(new Product("P002", "ลูกขนไก่ (หลอด)", 350, 60));
        data.products.add(new Product("P003", "เทปพันด้ามไม้", 65, 100));
        return data;
    }
}
