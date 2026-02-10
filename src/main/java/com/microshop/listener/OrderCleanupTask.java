package com.microshop.listener;

import com.microshop.context.DBContext;
import com.microshop.dao.DonHangDAO; 
import com.microshop.dao.DonHangSlotSteamDAO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tác vụ chạy định kỳ để quét và hủy các đơn hàng CHO_THANH_TOAN đã quá hạn.
 */
public class OrderCleanupTask extends TimerTask {
    
    private final DonHangDAO donHangDAO; 
    private final DonHangSlotSteamDAO donHangSlotSteamDAO; 
    private final int minutesThreshold;
    private static final Logger LOGGER = Logger.getLogger(OrderCleanupTask.class.getName());
    
    public OrderCleanupTask(DonHangDAO dao,  DonHangSlotSteamDAO dao1, int minutes) {
        this.donHangDAO = dao; 
        this.donHangSlotSteamDAO = dao1;
        this.minutesThreshold = minutes;
    }

    // Phương thức run() chỉ gọi hàm xử lý chính
    @Override
    public void run() {
        try {
            int rowsProcessed = cleanupAbandonedOrders(minutesThreshold); 
            int rowsProcessed1 = cleanupAbandonedSteamOrders(minutesThreshold); 
            if (rowsProcessed > 0) {
                LOGGER.log(Level.INFO, "Đã dọn dẹp thành công {0} đơn hàng bị bỏ dở (quá {1} phút).", 
                        new Object[]{rowsProcessed, minutesThreshold});
            }
            if (rowsProcessed1 > 0) {
                LOGGER.log(Level.INFO, "Đã dọn dẹp thành công {0} đơn hàng slot steam bị bỏ dở (quá {1} phút).", 
                        new Object[]{rowsProcessed1, minutesThreshold});
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Lỗi cơ sở dữ liệu khi dọn dẹp đơn hàng bỏ dở.", ex);
        }
    }
//---------------------------------------------------------
    
    /**
     * Thực hiện giao dịch hủy đơn hàng CHO_THANH_TOAN và giải phóng tài khoản game.
     * Logic DB đã được chuyển từ run() sang đây.
     */
    private int cleanupAbandonedOrders(int minutesThreshold) throws SQLException {
        Connection conn = null;
        int totalProcessed = 0;
        try {
            // 1. LẤY KẾT NỐI VÀ BẮT ĐẦU GIAO DỊCH
            conn = DBContext.getConnection();
//            System.out.println("Kết nối DB thành công."); 
            conn.setAutoCommit(false); 
            
            // 2. TÌM VÀ LẤY ID TAIKHOAN LIÊN QUAN
           // Dùng DATEADD với tham số âm để trừ thời gian trong SQL Server
String sqlSelect = "SELECT MaDonHang, MaTaiKhoan " +
                   "FROM DONHANG " +
                   "WHERE TrangThai = 'CHO_THANH_TOAN' AND ThoiGianTao < DATEADD(minute, -?, GETDATE())";
            List<Integer> maTaiKhoanToRelease = new ArrayList<>();
            List<Integer> maDonHangToUpdate = new ArrayList<>();
//            System.out.println(maDonHangToUpdate.size());
            try (PreparedStatement psSelect = conn.prepareStatement(sqlSelect)) {
                psSelect.setInt(1, minutesThreshold);
                try (ResultSet rs = psSelect.executeQuery()) {
                    while (rs.next()) {
                        maTaiKhoanToRelease.add(rs.getInt("MaTaiKhoan"));
                        maDonHangToUpdate.add(rs.getInt("MaDonHang"));
                    }
                }
            }
            
            if (maDonHangToUpdate.isEmpty()) {
                conn.setAutoCommit(true);
                return 0; // Không có gì để dọn dẹp
            }
//            System.out.println("?????");
//            for(int x : maDonHangToUpdate){
//                System.out.println(x);
//            }
            // 3. GIẢI PHÓNG TAIKHOAN (Chuyển về DANG_BAN)
            String sqlRelease = "UPDATE TAIKHOAN SET TrangThai = 'DANG_BAN' WHERE MaTaiKhoan = ?";
            try (PreparedStatement psRelease = conn.prepareStatement(sqlRelease)) {
                for (Integer maTaiKhoan : maTaiKhoanToRelease) {
                    psRelease.setInt(1, maTaiKhoan);
                    psRelease.addBatch();
                }
                psRelease.executeBatch();
            }

            // 4. HỦY ĐƠN HÀNG (Cập nhật TrangThai thành DA_HUY)
            String sqlUpdate = "UPDATE DONHANG SET TrangThai = 'DA_HUY' WHERE MaDonHang = ?";
            try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {
                for (Integer maDonHang : maDonHangToUpdate) {
                    psUpdate.setInt(1, maDonHang);
                    psUpdate.addBatch();
                }
                int[] results = psUpdate.executeBatch();
                for (int result : results) {
                    if (result > 0) totalProcessed++;
                }
            }

            conn.commit(); // HOÀN TẤT GIAO DỊCH
            return totalProcessed;

        } catch (SQLException e) {
            // XỬ LÝ LỖI: ROLLBACK
            if (conn != null) {
                try {
                    conn.rollback(); 
                    LOGGER.log(Level.SEVERE, "Rollback giao dịch thành công. Lỗi: " + e.getMessage());
                } catch (SQLException rollbackEx) {
                    LOGGER.log(Level.SEVERE, "Lỗi trong khi thực hiện rollback.", rollbackEx);
                }
            }
            throw e; 
            
        } finally {
            // 5. ĐÓNG KẾT NỐI
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); 
                    conn.close(); 
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Lỗi đóng Connection.", e);
                }
            }
        }
    }
    
    private int cleanupAbandonedSteamOrders(int minutesThreshold) throws SQLException {
        Connection conn = null;
        int totalProcessed = 0;
        try {
            // 1. LẤY KẾT NỐI VÀ BẮT ĐẦU GIAO DỊCH
            conn = DBContext.getConnection();
//            System.out.println("Kết nối DB thành công."); 
            conn.setAutoCommit(false); 
            
            // 2. TÌM VÀ LẤY ID TAIKHOAN LIÊN QUAN
            String sqlSelect = "SELECT MaDonHangSlot, MaTaiKhoanSteam " +
                   "FROM DONHANG_SLOT_STEAM " +
                   "WHERE TrangThai = 'CHO_THANH_TOAN' AND ThoiGianTao < DATEADD(minute, -?, GETDATE())";
            
            List<Integer> maTaiKhoanToRelease = new ArrayList<>();
            List<Integer> maDonHangToUpdate = new ArrayList<>();
//            System.out.println(maDonHangToUpdate.size());
            try (PreparedStatement psSelect = conn.prepareStatement(sqlSelect)) {
                psSelect.setInt(1, minutesThreshold);
                try (ResultSet rs = psSelect.executeQuery()) {
                    while (rs.next()) {
                        maTaiKhoanToRelease.add(rs.getInt("MaTaiKhoanSteam"));
                        maDonHangToUpdate.add(rs.getInt("MaDonHangSlot"));
                    }
                }
            }
            
            if (maDonHangToUpdate.isEmpty()) {
                conn.setAutoCommit(true);
                return 0; // Không có gì để dọn dẹp
            }
//            System.out.println("?????");
//            for(int x : maDonHangToUpdate){
//                System.out.println(x);
//            }
            // 3. GIẢI PHÓNG TAIKHOAN_STEAM
            String sqlRelease = "UPDATE TAIKHOAN_STEAM SET SoSlotDaBan = SoSlotDaBan - 1 WHERE MaTaiKhoanSteam = ?";
            try (PreparedStatement psRelease = conn.prepareStatement(sqlRelease)) {
                for (Integer maTaiKhoan : maTaiKhoanToRelease) {
                    psRelease.setInt(1, maTaiKhoan);
                    psRelease.addBatch();
                }
                psRelease.executeBatch();
            }

            // 4. HỦY ĐƠN HÀNG (Cập nhật TrangThai thành DA_HUY)
            String sqlUpdate = "UPDATE DONHANG_SLOT_STEAM SET TrangThai = 'DA_HUY' WHERE MaDonHangSlot = ?";
            try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {
                for (Integer maDonHang : maDonHangToUpdate) {
                    psUpdate.setInt(1, maDonHang);
                    psUpdate.addBatch();
                }
                int[] results = psUpdate.executeBatch();
                for (int result : results) {
                    if (result > 0) totalProcessed++;
                }
            }

            conn.commit(); // HOÀN TẤT GIAO DỊCH
            return totalProcessed;

        } catch (SQLException e) {
            // XỬ LÝ LỖI: ROLLBACK
            if (conn != null) {
                try {
                    conn.rollback(); 
                    LOGGER.log(Level.SEVERE, "Rollback giao dịch thành công. Lỗi: " + e.getMessage());
                } catch (SQLException rollbackEx) {
                    LOGGER.log(Level.SEVERE, "Lỗi trong khi thực hiện rollback.", rollbackEx);
                }
            }
            throw e; 
            
        } finally {
            // 5. ĐÓNG KẾT NỐI
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); 
                    conn.close(); 
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Lỗi đóng Connection.", e);
                }
            }
        }
    }
}
