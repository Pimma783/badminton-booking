package com.badminton.service;

import java.time.*;
import java.util.Set;

/**
 * เวลาเปิด-ปิดของวันหนึ่ง เก็บเป็นชั่วโมง เช่น open = 13, close = 21
 * <p>
 * ตามโจทย์: วันธรรมดา 13.00 – 21.00 น. / วันเสาร์ อาทิตย์ และวันหยุดนักขัตฤกษ์ 10.00 – 20.00 น.
 */
public record OperatingHours(int open, int close) {
    /** หาเวลาเปิดของวันที่กำหนด โดยวันหยุดนักขัตฤกษ์มาจากรายการที่ผู้ดูแลระบบตั้งไว้ */
    public static OperatingHours forDate(LocalDate date, Set<LocalDate> holidays) {
        boolean special = date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY || holidays.contains(date);
        return special ? new OperatingHours(10, 20) : new OperatingHours(13, 21);
    }
    /** ช่วงที่จองต้องอยู่ในเวลาเปิดทั้งหมด เช่น ปิด 21:00 จองเริ่ม 20:00 ได้แค่ 1 ชั่วโมง */
    public boolean contains(int start, int duration) { return duration > 0 && start >= open && start + duration <= close; }
    // รูปแบบสำหรับแสดงผล เช่น "13:00 - 21:00"
    @Override public String toString() { return "%02d:00 - %02d:00".formatted(open, close); }
}
