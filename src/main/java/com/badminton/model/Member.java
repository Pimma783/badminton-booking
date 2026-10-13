package com.badminton.model;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * สมาชิกของสโมสร — ตามโจทย์ ผู้ที่จองสนามได้ต้องสมัครเป็นสมาชิกก่อน
 * <p>
 * เป็น record ซึ่งแก้ค่าไม่ได้หลังสร้าง (immutable) การเปลี่ยนยอดต้องสร้างอ็อบเจกต์ใหม่
 * ผ่านเมธอด withXxx และมีเพียง ClubService ที่เรียกใช้ จึงไม่มีส่วนอื่นแอบแก้ยอดได้
 *
 * @param id           รหัสสมาชิก เช่น M0001
 * @param name         ชื่อ-นามสกุล
 * @param phone        เบอร์โทรศัพท์ (ห้ามซ้ำกับสมาชิกคนอื่น)
 * @param joinedOn     วันที่สมัคร
 * @param bookedHours  จำนวนชั่วโมงที่จองสะสมทั้งหมด ใช้คำนวณสิทธิ์ชั่วโมงที่ 20
 * @param packageHours ชั่วโมงแพ็กเกจคงเหลือ
 */
public record Member(String id, String name, String phone, LocalDate joinedOn,
                     int bookedHours, int packageHours) implements Serializable {
    /** คืนสมาชิกคนเดิมที่เพิ่มชั่วโมงจองสะสม และหักชั่วโมงแพ็กเกจตามที่ใช้ไป */
    public Member withBooking(int hours, int credits) {
        return new Member(id, name, phone, joinedOn, bookedHours + hours, packageHours - credits);
    }
    /** คืนสมาชิกคนเดิมที่ได้ชั่วโมงแพ็กเกจเพิ่ม 15 ชั่วโมง (แพ็กเกจ 2,600 บาท) */
    public Member withPackage() {
        return new Member(id, name, phone, joinedOn, bookedHours, packageHours + 15);
    }
    // ข้อความที่แสดงใน ComboBox เลือกสมาชิก และใช้ค้นหาในแท็บสมาชิก
    @Override public String toString() { return id + " • " + name + " (" + phone + ")"; }
}
