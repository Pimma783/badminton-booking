package com.badminton.model;

/**
 * พนักงานแคชเชียร์ — กรอกข้อมูลการจองแทนสมาชิก ขาย/ให้เช่าอุปกรณ์ และค้นหาการจองได้
 * แต่ดูรายงานรายได้และประวัติสมาชิกไม่ได้
 */
public final class Cashier extends StaffUser {
    public Cashier(String username, String displayName) { super(username, displayName); }
    @Override public Role getRole() { return Role.CASHIER; }
    // แคชเชียร์ไม่มีสิทธิ์ดูรายงาน
    @Override public boolean canViewReports() { return false; }
}
