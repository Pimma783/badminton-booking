package com.badminton.service;

import com.badminton.model.*;

/**
 * วิธีคิดราคาค่าเช่าสนาม — เป็น interface ตามข้อกำหนดโจทย์
 * <p>
 * ClubService เรียกใช้ผ่าน interface นี้ ถ้าวันหนึ่งสโมสรเปลี่ยนโปรโมชัน
 * ก็สร้างคลาสใหม่ที่ implements PricingPolicy แล้วส่งเข้า ClubService แทนได้ทันที
 */
public interface PricingPolicy {
    /**
     * คำนวณราคาสำหรับการจองหนึ่งครั้ง
     *
     * @param member   สมาชิกผู้จอง (ใช้ชั่วโมงสะสมและแพ็กเกจคงเหลือ)
     * @param duration จำนวนชั่วโมงที่จอง
     * @param mode     วิธีชำระ: จ่ายเงิน หรือใช้แพ็กเกจ
     * @return เงินที่ต้องจ่าย ชั่วโมงแพ็กเกจที่ใช้ และชั่วโมงที่ได้ส่วนลด
     */
    BookingQuote quote(Member member, int duration, PaymentMode mode);
}
