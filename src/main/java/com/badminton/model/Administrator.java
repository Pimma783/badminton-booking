package com.badminton.model;

/**
 * ผู้ดูแลระบบ — ทำรายการได้เหมือนแคชเชียร์ และยังดูประวัติการจองของสมาชิก
 * ดูสรุปรายได้รายวัน/รายเดือน และกำหนดวันหยุดนักขัตฤกษ์ได้
 */
public final class Administrator extends StaffUser {
    public Administrator(String username, String displayName) { super(username, displayName); }
    @Override public Role getRole() { return Role.ADMIN; }
    // ผู้ดูแลระบบมีสิทธิ์ดูรายงานทั้งหมด
    @Override public boolean canViewReports() { return true; }
}
