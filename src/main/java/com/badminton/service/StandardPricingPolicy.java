package com.badminton.service;

import com.badminton.model.*;

/**
 * ราคามาตรฐานตามโจทย์: ชั่วโมงละ 180 บาท และ "จองครบ 19 ชั่วโมง ชั่วโมงที่ 20 คิดเพียง 30%"
 * <p>
 * 30% ของ 180 บาท = 54 บาท โดยให้สิทธิ์ซ้ำทุกชั่วโมงสะสมที่ 20, 40, 60, ...
 */
public final class StandardPricingPolicy implements PricingPolicy {
    public static final int HOURLY_RATE = 180;
    @Override public BookingQuote quote(Member member, int duration, PaymentMode mode) {
        if (duration < 1 || duration > 10) throw new IllegalArgumentException("เลือกจำนวนชั่วโมง 1-10 ชั่วโมง");
        if (mode == null) throw new IllegalArgumentException("เลือกวิธีชำระเงิน");
        long money = 0;
        int credits = 0, discounts = 0;
        // ไล่คิดทีละชั่วโมง เพราะในการจองครั้งเดียวอาจมีชั่วโมงที่ 20 อยู่ตรงกลาง
        // เช่น สะสมแล้ว 18 ชม. จอง 3 ชม. → ชั่วโมงที่ 19, 20, 21 → 180 + 54 + 180
        for (int hour = 1; hour <= duration; hour++) {
            if ((member.bookedHours() + hour) % 20 == 0) {
                // ชั่วโมงสะสมที่หาร 20 ลงตัว คิด 30% และจ่ายเป็นเงิน ไม่หักแพ็กเกจ
                money += 54;
                discounts++;
            } else if (mode == PaymentMode.PACKAGE) {
                // ใช้แพ็กเกจ: หักชั่วโมงแพ็กเกจแทนเงิน
                credits++;
            } else { money += HOURLY_RATE; }
        }
        // ใช้แพ็กเกจแต่ชั่วโมงคงเหลือไม่พอ → ไม่ให้จอง (ไม่ผสมเงินกับแพ็กเกจให้อัตโนมัติ)
        if (credits > member.packageHours()) throw new IllegalArgumentException("ชั่วโมงแพ็กเกจไม่เพียงพอ ต้องใช้ " + credits + " ชั่วโมง");
        return new BookingQuote(money, credits, discounts, duration);
    }
}
