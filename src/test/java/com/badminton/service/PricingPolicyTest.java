package com.badminton.service;

import com.badminton.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

/** ทดสอบการคิดราคา: 180 บาท/ชม. ชั่วโมงสะสมที่ 20 คิด 54 บาท และการใช้แพ็กเกจ */
class PricingPolicyTest {
    private final PricingPolicy policy = new StandardPricingPolicy();
    // สร้างสมาชิกทดสอบที่จองสะสมมาแล้ว total ชั่วโมง และมีแพ็กเกจเหลือ credits ชั่วโมง
    private Member member(int total, int credits) { return new Member("M1", "สมาชิกทดสอบ", "0812345678", LocalDate.now(), total, credits); }
    // แต่ละแถว: ชั่วโมงสะสมเดิม, จำนวนชั่วโมงที่จอง, เงินที่ต้องจ่าย, จำนวนชั่วโมงที่ได้ส่วนลด
    // เช่น "18,3,414,1" = สะสม 18 จอง 3 → ชั่วโมงที่ 19, 20, 21 → 180 + 54 + 180 = 414
    @ParameterizedTest
    @CsvSource({"0,1,180,0", "18,1,180,0", "19,1,54,1", "18,3,414,1", "19,3,414,1", "39,2,234,1", "20,1,180,0"})
    void calculatesEveryTwentiethHour(int total, int duration, long cash, int discounted) {
        BookingQuote quote = policy.quote(member(total, 0), duration, PaymentMode.CASH);
        assertEquals(cash, quote.paidBaht()); assertEquals(discounted, quote.discountedHours());
    }
    // ใช้แพ็กเกจ 3 ชั่วโมง ต้องไม่เก็บเงินซ้ำ
    @Test void packageHoursAreNotChargedTwice() {
        BookingQuote quote = policy.quote(member(0, 15), 3, PaymentMode.PACKAGE);
        assertEquals(0, quote.paidBaht()); assertEquals(3, quote.packageHoursUsed());
    }
    // ชั่วโมงที่ 20 จ่ายเงิน 54 บาท ไม่หักแพ็กเกจ ส่วนอีก 2 ชั่วโมงหักแพ็กเกจ
    @Test void discountedPackageHourIsPaidInCashWithoutConsumingCredit() {
        BookingQuote quote = policy.quote(member(19, 2), 3, PaymentMode.PACKAGE);
        assertEquals(54, quote.paidBaht()); assertEquals(2, quote.packageHoursUsed());
    }
    // แพ็กเกจเหลือไม่พอต้องจองไม่ได้
    @Test void insufficientCreditsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> policy.quote(member(0, 1), 2, PaymentMode.PACKAGE));
    }
    // จำนวนชั่วโมงต้องมากกว่า 0
    @Test void rejectsZeroAndNegativeDuration() {
        assertThrows(IllegalArgumentException.class, () -> policy.quote(member(0, 0), 0, PaymentMode.CASH));
        assertThrows(IllegalArgumentException.class, () -> policy.quote(member(0, 0), -1, PaymentMode.CASH));
    }
}
