package com.badminton.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * รายการจองสนาม 1 รายการ — ตามโจทย์ ทุกครั้งที่จองต้องลงชื่อ ระบุหมายเลขสนาม วัน เวลา
 * และแนบสลิปการชำระเงิน โดยพนักงานแคชเชียร์เป็นผู้กรอก
 *
 * @param id               เลขที่การจอง เช่น B00001
 * @param memberId         รหัสสมาชิกผู้จอง
 * @param memberName       ชื่อสมาชิก ณ ตอนจอง (การลงชื่อ)
 * @param court            หมายเลขสนาม 1-12
 * @param date             วันที่ใช้สนาม
 * @param startHour        ชั่วโมงที่เริ่ม เช่น 13 คือ 13:00 น.
 * @param duration         จำนวนชั่วโมงที่จอง
 * @param packageHoursUsed จำนวนชั่วโมงที่หักจากแพ็กเกจ
 * @param discountedHours  จำนวนชั่วโมงที่ได้ราคา 30% (ชั่วโมงที่ 20, 40, ...)
 * @param paidBaht         เงินที่รับจริงสำหรับการจองนี้
 * @param slipPath         ตำแหน่งสำเนาสลิป (อ้างอิงจากโฟลเดอร์ข้อมูล)
 * @param staffUsername    ชื่อผู้ใช้ของพนักงานที่บันทึกรายการ
 * @param createdAt        วันเวลาที่บันทึกรายการ
 */
public record Booking(String id, String memberId, String memberName, int court,
                      LocalDate date, int startHour, int duration, int packageHoursUsed,
                      int discountedHours, long paidBaht, String slipPath,
                      String staffUsername, LocalDateTime createdAt) implements Serializable {
    /** ชั่วโมงที่สิ้นสุด (ไม่รวมชั่วโมงนี้) เช่น เริ่ม 13 จอง 2 ชั่วโมง จะได้ 15 */
    public int endHour() { return startHour + duration; }
    /** ช่วงเวลาสำหรับแสดงผล เช่น "13:00 - 15:00" */
    public String timeRange() { return "%02d:00 - %02d:00".formatted(startHour, endHour()); }
    /** คำอธิบายวิธีชำระเงินแบบสั้น */
    public String paymentDescription() {
        return packageHoursUsed == 0 ? "ชำระเงิน" : "แพ็กเกจ " + packageHoursUsed + " ชม. + เงิน";
    }
}
