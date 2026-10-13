package com.badminton.model;

/**
 * ผลการคำนวณราคาก่อนบันทึกการจอง ได้จาก {@code PricingPolicy.quote(...)}
 *
 * @param paidBaht         เงินที่ต้องจ่าย
 * @param packageHoursUsed ชั่วโมงที่จะหักจากแพ็กเกจ
 * @param discountedHours  ชั่วโมงที่ได้ราคา 30%
 * @param duration         จำนวนชั่วโมงทั้งหมดที่จอง
 */
public record BookingQuote(long paidBaht, int packageHoursUsed, int discountedHours, int duration) {
    /** ข้อความสรุปราคาที่แสดงในแบบฟอร์มจอง (แบ่ง 2 บรรทัดเพื่อไม่ให้ข้อความล้นกล่อง) */
    public String description() {
        return "ค่าเช่า " + paidBaht + " บาท  •  ใช้แพ็กเกจ " + packageHoursUsed
                + " ชม.\nชั่วโมงลดราคา " + discountedHours + " ชม.";
    }
}
